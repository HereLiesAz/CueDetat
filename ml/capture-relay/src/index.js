/**
 * Cue D'état training-capture relay (Cloudflare Worker).
 *
 * The app (CaptureRecorder) POSTs one frame at a time to /upload as multipart/form-data:
 *   image  JPEG, the upright camera frame
 *   meta   JSON sidecar (sensors, table pose, detections; see CaptureRecorder)
 * with headers X-Install-Id (random per install) and X-App-Version.
 *
 * The relay checks the upload and commits both files to a private GitHub repo:
 *   captures/<yyyy-mm-dd>/<install-id>/<id>.jpg|.json
 * It holds the GitHub token so the app never does; a leaked app can only send frames through
 * these checks, not touch the repo.
 *
 * Environment:
 *   GITHUB_TOKEN   secret: fine-grained token, Contents read/write on GITHUB_REPO only
 *   GITHUB_REPO    owner/name of the private data repo
 *   GITHUB_BRANCH  branch to commit to (default main)
 *   UPLOAD_LIMIT   optional rate-limit binding, keyed per install id
 */

const MAX_IMAGE_BYTES = 6 * 1024 * 1024;
const MAX_META_BYTES = 64 * 1024;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const CAPTURE_ID = /^cap_\d{10,16}$/;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== "/upload") return text(404, "not found");
    if (request.method !== "POST") return text(405, "POST only");

    const installId = (request.headers.get("X-Install-Id") || "").toLowerCase();
    if (!UUID.test(installId)) return text(400, "bad install id");

    if (env.UPLOAD_LIMIT) {
      const { success } = await env.UPLOAD_LIMIT.limit({ key: installId });
      if (!success) return text(429, "slow down");
    }

    let form;
    try {
      form = await request.formData();
    } catch {
      return text(400, "expected multipart/form-data");
    }
    const image = form.get("image");
    const meta = form.get("meta");
    if (!(image instanceof File) || !(meta instanceof File)) return text(400, "image and meta required");
    if (image.size > MAX_IMAGE_BYTES || meta.size > MAX_META_BYTES) return text(413, "too large");

    const jpeg = new Uint8Array(await image.arrayBuffer());
    if (jpeg.length < 4 || jpeg[0] !== 0xff || jpeg[1] !== 0xd8) return text(400, "not a JPEG");

    let sidecar;
    try {
      sidecar = JSON.parse(await meta.text());
    } catch {
      return text(400, "meta is not JSON");
    }
    if (!CAPTURE_ID.test(String(sidecar.id))) return text(400, "bad capture id");
    sidecar.installId = installId;
    sidecar.appVersion = (request.headers.get("X-App-Version") || "").slice(0, 32);
    sidecar.receivedAt = new Date().toISOString();

    const day = sidecar.receivedAt.slice(0, 10);
    const base = `captures/${day}/${installId}/${sidecar.id}`;
    const repo = env.GITHUB_REPO;
    const branch = env.GITHUB_BRANCH || "main";

    // JPEG first: a sidecar without its image is useless, the reverse is merely unlabelled.
    const a = await putFile(env, repo, branch, `${base}.jpg`, base64(jpeg), `capture ${sidecar.id}`);
    if (!a.ok && a.status !== 422) return text(502, `github ${a.status}`); // 422: already there (retry)
    const json = new TextEncoder().encode(JSON.stringify(sidecar, null, 1));
    const b = await putFile(env, repo, branch, `${base}.json`, base64(json), `capture ${sidecar.id} meta`);
    if (!b.ok && b.status !== 422) return text(502, `github ${b.status}`);

    return text(201, "stored");
  },
};

function putFile(env, repo, branch, path, content, message) {
  return fetch(`https://api.github.com/repos/${repo}/contents/${path}`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${env.GITHUB_TOKEN}`,
      Accept: "application/vnd.github+json",
      "User-Agent": "cuedetat-capture-relay",
      "X-GitHub-Api-Version": "2022-11-28",
    },
    body: JSON.stringify({ message, content, branch }),
  });
}

function base64(bytes) {
  let s = "";
  for (let i = 0; i < bytes.length; i += 0x8000) {
    s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  }
  return btoa(s);
}

function text(status, body) {
  return new Response(body, { status, headers: { "Content-Type": "text/plain" } });
}
