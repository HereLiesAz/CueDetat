# Releasing Cue D’état — Signed AAB & Google Play Delivery

This document covers how the app is packaged for the Play Store as a signed
Android App Bundle (AAB), how the version code stays monotonic, how modular
delivery is configured, and how to publish through CI.

---

## TL;DR

~~~bash
# Local signed AAB (Play), versionCode = git commit count:
./gradlew bundleRelease -PversionBuild=$(git rev-list --count HEAD)

# Local signed APK (GitHub Releases):
./gradlew assembleRelease -PversionBuild=$(git rev-list --count HEAD)
~~~

CI: every push to `main` publishes to Play through the central **Android Play Release**
(see §5).

---

## 1. One build, two channels

There are no product flavors. One variant, `com.hereliesaz.cuedetat`, ships as a signed AAB
to Google Play and as a signed APK on GitHub Releases.

- The ~24 MB TFLite model and the Expert-AR code (ARCore) are compiled into the app. Their
  sources still live in `feature_mlmodel/` and `feature_expert_ar/`, which are plain source
  folders added to `:app`'s main source set, not modules.
- Updates: `GithubAppUpdater` checks the installer of record. Play installs update through
  Play and never see the GitHub check (Play forbids self-updating); every other install
  checks GitHub Releases and opens the new APK in the browser. The app holds no
  `REQUEST_INSTALL_PACKAGES` permission.
- The Meta Wearables SDK is always included, so builds need GitHub Packages credentials
  (`GH_ACTOR`/`GH_TOKEN`, or `gh_user`/`gh_token` in `local.properties`).

Earlier installs of the old `com.hereliesaz.cuedetat.foss` build can't upgrade in place (a
different package); those users install once more.

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
  ./gradlew bundleRelease -PversionBuild=$(git rev-list --count HEAD) \
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
There are no separate artifacts to build; this is inherent to `bundleRelease`.
(The app already restricts native code to `arm64-v8a` for 16 KB-page
compliance, so the ABI dimension is effectively a single split.)

### 4.2 R8 / minification + resource shrinking (already enabled)

`buildTypes.release` already sets `isMinifyEnabled = true` and
`isShrinkResources = true`, with keep rules in `app/proguard-rules.pro` covering
the reflection/JNI-sensitive surfaces (TensorFlow Lite, OpenCV, ML Kit, Hilt,
Gson/Retrofit, Parcelable, `@Keep`). No change was needed here.

### 4.3 Model and AR code: bundled

Both used to be on-demand dynamic feature modules. Split installs failed on real devices
(ARCore's native library invisible to the process), so both are now compiled into the base.
`ModelDelivery` and `ArFeatureDelivery` remain as interfaces with no-op implementations
(`BundledModelDelivery`, `BundledArFeatureDelivery`); `ArControllerFacade` still resolves
`ArControllerImpl` reflectively, and `app/proguard-rules.pro` keeps it.

---

## 5. CI: publishing to Play

`.github/workflows/play_publish.yml` is the registered source for HereLiesAz/workflows'
**Android Play Release**; the controller replaces it with a tracker, and the build runs
centrally with the central signing and Play credentials. On every push to `main` it builds
`./gradlew bundleRelease` (versionCode = git commit count), uploads one AAB, and assigns it in
a single edit to:

| Track | Status |
|-------|--------|
| internal | completed (live to internal testers) |
| alpha (closed testing) | completed |
| beta (open testing) | draft |
| production | draft |

Promoting the production draft is a human step in the Play Console.

`android_release.yml` builds the signed APK for GitHub Releases.

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

The app processes camera frames on-device. The one exception is optional **training data**
(`CaptureRecorder`, see `PrivacyPolicy.md`): only after the user agrees in the consent
dialog, steady camera frames with sensor readings and detections are sent over Wi-Fi to the
developer (via the Cloudflare relay into a private GitHub repo).

When filling out the Play Console **Data safety** form:

- **Photos and videos: collected**, optional (user can decline or turn it off), not shared
  with third parties, encrypted in transit, purpose "App functionality" / product
  improvement. Deletion requests go to the developer.
- **App info and performance / device identifiers**: none beyond a random per-install id sent
  with training frames; no advertising id, no `AD_ID` permission, no ads SDK.
- **Location**: coarse location stays on the device (table profiles); not collected.
- A public **privacy policy URL** is required; host `PrivacyPolicy.md` and link it in the
  listing.
