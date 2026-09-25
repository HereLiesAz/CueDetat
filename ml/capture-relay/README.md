# Capture relay

Receives training frames from the app (`CaptureRecorder`) and commits them to a private GitHub
repo. The GitHub token lives here, not in the APK: anything shipped in an APK can be extracted,
and whoever extracts a token can write to (or wipe) its repo. The app only knows this relay's
URL.

What it accepts: `POST /upload`, multipart with `image` (JPEG, ≤ 6 MB) and `meta` (JSON,
≤ 64 KB, `id` like `cap_<ms>`), header `X-Install-Id` (UUID). Anything else is refused.
Frames land at `captures/<yyyy-mm-dd>/<install-id>/<id>.jpg|.json`, one commit per file.
Uploads are rate-limited per install (30 a minute).

## Set up (once)

1. Create a **private** repo for the data, e.g. `HereLiesAz/cuedetat-captures`, with a first
   commit on `main` (a README is enough).
2. Create a fine-grained GitHub token: *only* that repo, permission **Contents: read and
   write**, nothing else.
3. Add it to the Worker as the secret `GITHUB_TOKEN` (Cloudflare dashboard → Workers →
   `cuedetat-capture-relay` → Settings → Variables and secrets). Deploys keep it.

Deploys are central: `.github/workflows/capture-relay-deploy.yml` is bound to
`HereLiesAz/workflows`' **Cloudflare Worker Deploy**, which runs `npm test` here and
`wrangler deploy` on every push to `main` touching `ml/capture-relay/`, with the central
Cloudflare credentials. The first deploy creates the Worker; set the secret after it, then the
relay works. (To deploy by hand instead: `wrangler secret put GITHUB_TOKEN && wrangler deploy`.)

4. In the CueDetat repo: Settings → Secrets and variables → Actions → new secret
   `CAPTURE_RELAY_URL` = the workers.dev URL. CI builds pick it up; local builds take
   `-PcaptureRelayUrl=…`. Without it, the app keeps frames on the phone and sends nothing.

Each push's deploy result shows on the commit as the `.github/workflows/capture-relay-deploy.yml`
status, linking to the central run.

## Limits worth knowing

- GitHub repos get slow past a few GB. At ~1 MB a frame that is a few thousand frames; move
  old days out (or to Git LFS / object storage) before then.
- Free Workers allow 100k requests a day, far more than this needs.
- The app sends only on unmetered networks (Wi-Fi) and only after the user agreed in the
  consent dialog; the menu's "Training data" item turns it off.
