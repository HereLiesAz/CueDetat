// Upload checks of the capture relay, with GitHub's API stubbed out. Run: npm test
import { test } from "node:test";
import assert from "node:assert/strict";
import relay from "../src/index.js";

const env = { GITHUB_TOKEN: "t", GITHUB_REPO: "o/r" };
const INSTALL = "123e4567-e89b-12d3-a456-426614174000";
const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 1, 2, 3]);

function upload(meta, { install = INSTALL, image = JPEG } = {}) {
  const form = new FormData();
  form.append("image", new File([image], "a.jpg"));
  form.append("meta", new File([JSON.stringify(meta)], "a.json"));
  return new Request("https://relay/upload", { method: "POST", body: form, headers: { "X-Install-Id": install } });
}

function stubGithub(status = 201) {
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push({ url, body: JSON.parse(init.body) });
    return new Response("{}", { status });
  };
  return calls;
}

test("stores image then sidecar under day/install/id", async () => {
  const calls = stubGithub();
  const res = await relay.fetch(upload({ id: "cap_1790000000000" }), env);
  assert.equal(res.status, 201);
  assert.equal(calls.length, 2);
  assert.match(calls[0].url, /\/repos\/o\/r\/contents\/captures\/\d{4}-\d{2}-\d{2}\/123e4567-e89b-12d3-a456-426614174000\/cap_1790000000000\.jpg$/);
  assert.match(calls[1].url, /cap_1790000000000\.json$/);
  const sidecar = JSON.parse(Buffer.from(calls[1].body.content, "base64").toString());
  assert.equal(sidecar.installId, INSTALL);
});

test("an already-stored frame (GitHub 422) counts as stored", async () => {
  stubGithub(422);
  assert.equal((await relay.fetch(upload({ id: "cap_1790000000000" }), env)).status, 201);
});

test("refuses bad install id, path-like capture id, non-JPEG, wrong method and path", async () => {
  const calls = stubGithub();
  assert.equal((await relay.fetch(upload({ id: "cap_1790000000000" }, { install: "nope" }), env)).status, 400);
  assert.equal((await relay.fetch(upload({ id: "../../etc" }), env)).status, 400);
  assert.equal((await relay.fetch(upload({ id: "cap_1790000000000" }, { image: new Uint8Array([1, 2, 3]) }), env)).status, 400);
  assert.equal((await relay.fetch(new Request("https://relay/upload"), env)).status, 405);
  assert.equal((await relay.fetch(new Request("https://relay/other", { method: "POST" }), env)).status, 404);
  assert.equal(calls.length, 0);
});
