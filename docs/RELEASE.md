# Releasing Cue D’état — Signed AAB & Google Play Delivery

This document covers how the app is packaged for the Play Store as a signed
Android App Bundle (AAB), how the version code stays monotonic, how modular
delivery is configured, and how to publish through CI.

---

## TL;DR

```bash
# Local signed AAB (Play flavor), versionCode = git commit count:
./gradlew bundlePlayRelease -PversionBuild=$(git rev-list --count HEAD)

# Local signed FOSS APK (GitHub-Releases distribution):
./gradlew assembleFossRelease -PversionBuild=$(git rev-list --count HEAD)
```

CI: run the **“Play Publish (AAB)”** workflow (`workflow_dispatch`). It defaults
to *build-only* (uploads the `.aab` as an artifact). Set `publish: true` to push
to Play; it defaults to the **internal** track, **draft** status.

---

## 1. Flavors & distribution channels

The app has two product flavors on the `distribution` dimension:

| Flavor | applicationId | Channel | Notes |
|--------|---------------|---------|-------|
| `play` | `com.hereliesaz.cuedetat` | Google Play (AAB) | Includes Play Billing, Credential Manager, Play Feature Delivery. |
| `foss` | `com.hereliesaz.cuedetat.foss` | GitHub Releases (standalone APK) | No Google Play dependencies; self-updates by sideloading. |

Because the FOSS build is a **standalone APK with no Play split-install
channel**, anything delivered on-demand for Play must still be bundled directly
into the FOSS APK. This shapes the dynamic-feature setup below.

---

## 2. Signing

`app/build.gradle.kts` defines `signingConfigs.release`, which reads, in order:

1. Gradle properties: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
2. Environment variables of the same names

If none are present, the release build falls back to the debug keystore (so local
release builds don’t fail for contributors without the keystore). **A real
release must supply the keystore.**

Two equivalent ways to sign a release build:

- **In-file config (local):** put the values in `local.properties` /
  `~/.gradle/gradle.properties` or pass `-PKEYSTORE_PATH=… -PKEYSTORE_PASSWORD=… …`.
- **Injected signing (CI):** pass the standard AGP injected-signing properties
  (this is what the CI workflows use, materializing the keystore from secrets):

  ```bash
  ./gradlew bundlePlayRelease -PversionBuild=$(git rev-list --count HEAD) \
    -Pandroid.injected.signing.store.file=$PWD/app/keystore.jks \
    -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
    -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
    -Pandroid.injected.signing.key.password=$KEY_PASSWORD
  ```

> Never commit a keystore or secrets. `keystore.jks`, `*.p12`, and
> `local.properties` are git-ignored / generated only in CI.

---

## 3. Version code (must strictly increase)

Play rejects an upload whose `versionCode` is equal to or lower than a previous
one. The build resolves `versionCode` like this:

- **Local builds:** auto-incremented from `version.properties` (`BUILD`),
  exactly as before — no behavior change.
- **CI / explicit override:** pass `-PversionBuild=<n>`. This becomes the
  authoritative `versionCode` (and the build segment of `versionName`). CI passes
  `git rev-list --count HEAD`, which is monotonic across commits.

When `-PversionBuild` is supplied, the build **does not** write the number back
into `version.properties` (so CI’s commit-count never clobbers the local
sequence). An optional `-PversionName=<string>` overrides the version name.

---

## 4. Modular delivery & size

### 4.1 Automatic AAB splits (no extra work)

Publishing an **AAB** means Google Play generates and serves optimized APKs per
device automatically — split by **screen density**, **ABI**, and **language**.
There are no separate artifacts to build; this is inherent to `bundlePlayRelease`.
(The app already restricts native code to `arm64-v8a` for 16 KB-page
compliance, so the ABI dimension is effectively a single split.)

### 4.2 R8 / minification + resource shrinking (already enabled)

`buildTypes.release` already sets `isMinifyEnabled = true` and
`isShrinkResources = true`, with keep rules in `app/proguard-rules.pro` covering
the reflection/JNI-sensitive surfaces (TensorFlow Lite, OpenCV, ML Kit, Hilt,
Gson/Retrofit, Parcelable, `@Keep`). No change was needed here.

### 4.3 Dynamic feature module: `:feature_mlmodel` (on-demand)

The single largest payload is the ~24 MB TFLite master model
(`ml/MASTER_POOL_MODEL.tflite`). It now lives in an **on-demand dynamic feature
module**, `:feature_mlmodel`:

- The model file physically lives in
  `feature_mlmodel/src/main/assets/ml/`.
- **Play (`play` AAB):** the base bundle ships **without** the model (~24 MB
  smaller install). It is downloaded on demand via **Play Feature Delivery** the
  first time the user needs detection — either opening the table-scan flow
  (`TableScanViewModel`) or the first processed camera frame in the aiming path
  (`VisionRepository.processImage` / `processArCpuImage`) — both routed through
  `PocketDetector.ensureModelReady()`.
- **FOSS (`foss` APK):** the `foss` flavor adds the feature module’s `assets`
  directory to its own source set, so the standalone APK bundles the model
  directly (no Play channel needed, no duplicated file in git).

Delivery is implemented behind the flavor-abstracted `ModelDelivery` interface
(mirroring the existing `AppUpdater` pattern):

- `app/src/main/.../delivery/ModelDelivery.kt` — interface (defaults: present).
- `app/src/foss/.../delivery/FossModelDelivery.kt` — no-op (model bundled).
- `app/src/play/.../delivery/PlayModelDelivery.kt` — `SplitInstallManager` +
  `SplitCompat` on-demand install.

The detector degrades gracefully: if the model isn’t present yet, detection
simply returns no result (no crash), and the base app remains fully installable
and usable on its own. `ensureModelReady()` triggers the install and reloads.

> ⚠️ **Needs device verification.** The build wiring is validated by CI
> (`bundlePlayRelease` / `assembleFossRelease` must assemble), but the runtime
> on-demand split install + cross-split asset access (`SplitCompat` +
> `createPackageContext`) cannot be verified in this environment. Validate on a
> real device via the **internal** track before relying on it in production. If
> issues arise, the safe fallback is to move the model back into
> `app/src/main/assets/ml/` (revert the module) — both flavors work unchanged.

### 4.4 Play In-App Updates (optional follow-up)

The `play` flavor’s `PlayAppUpdater` is currently a no-op (Play manages store
updates). If you later want in-app update prompts (flexible/immediate), add the
Play Core **App Update** API (`com.google.android.play:app-update`) — a clean,
optional follow-up, out of scope here.

---

## 5. CI: the **Play Publish (AAB)** workflow

File: `.github/workflows/play_publish.yml` — `workflow_dispatch` only.

Inputs:

| Input | Values | Default | Meaning |
|-------|--------|---------|---------|
| `track` | internal / alpha / beta / production | `internal` | Play track. |
| `status` | draft / completed | `draft` | Release status on the track. |
| `publish` | true / false | `false` | `false` = build + upload the `.aab` as a workflow artifact only; `true` = also push to Play. |

What it does: checkout (full history) → materialize keystore from secrets → JDK
17 + Gradle → `bundlePlayRelease -PversionBuild=$(git rev-list --count HEAD)`
with injected signing → upload the `.aab` artifact → (if `publish=true`) publish
via `r0adkll/upload-google-play@v1` with the `mapping.txt` for crash
deobfuscation. The package name is read from `gradle/libs.versions.toml`.

The existing `release.yml` / `android_release.yml` (FOSS APK → GitHub Releases)
are unchanged.

---

## 6. Required repository secrets

Signing (already used by `android_release.yml`):

| Secret | Purpose |
|--------|---------|
| `KEYSTORE_PRIVATE` | PEM private key (assembled into `keystore.jks` in CI). |
| `KEYSTORE_CHAIN` | PEM certificate chain. |
| `KEYSTORE_PASSWORD` | Keystore + key store password. |
| `KEY_ALIAS` | Key alias. |
| `KEY_PASSWORD` | Key password. |

Play publishing (new):

| Secret | Purpose |
|--------|---------|
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Cloud service-account JSON with Play release access. |

Build support (already used elsewhere; pass-through, all optional/graceful):

| Secret | Purpose |
|--------|---------|
| `GH_TOKEN`, `GH_ACTOR` | Read the Meta Wearables artifacts from GitHub Packages. |
| `GOOGLE_SERVICES_API_KEY`, `PROJECT_ID`, `CLIENT_ID` | `google-services.json` injection (if a template exists). |
| `GG_LINK`, `GG_SESSION` | Tester-license allowlist scrape (writes an empty allowlist when absent). |

---

## 7. One-time Play Console / Google Cloud setup (manual)

1. **Create a service account** in Google Cloud Console for the project linked to
   your Play developer account; create a JSON key for it.
2. In the **Play Console → Users and permissions**, invite that service account
   and grant it **release** permission for this app (at minimum: *Release to
   testing tracks* / *Release to production*, plus *View app information*).
3. Put the JSON key contents into the `PLAY_SERVICE_ACCOUNT_JSON` repo secret.
4. **First upload must be manual.** The Play Developer API cannot create the very
   first release of a brand-new app/package. Upload one signed AAB by hand in the
   Play Console (any track) to establish the app, then the workflow can publish
   all subsequent builds.
5. Make sure the **upload key** in your keystore matches what Play expects. If you
   use **Play App Signing** (recommended), the keystore here is your *upload*
   key; Google holds the app signing key.

---

## 8. Data safety & privacy (Play requirement)

The app is camera/sensor based and processes frames on-device; per
`PrivacyPolicy.md` it does **not** collect or transmit personal data, and makes
only an anonymous GitHub version-check call (FOSS).

When filling out the Play Console **Data safety** form, note:

- **No `AD_ID` permission** is declared and there is **no AdMob/ads SDK** — declare
  “no advertising or ads”.
- The `play` flavor links **Play Billing**, **Credential Manager / Google Sign-In**
  (used only to read the signed-in account email for tester-license matching),
  **Play Integrity**, and **Play Services Location**. Review each for the Data
  safety questionnaire and declare data collection/sharing accordingly.
- A public **privacy policy URL** is required; host `PrivacyPolicy.md` and link it
  in the Play listing.
- If any third-party SDK above transmits data off-device, that must be reflected
  in Data safety and the privacy policy.
