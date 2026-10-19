# ARCHITECTURE.md

Companion to [`AGENTS.md`](AGENTS.md). What shouldn't live in a prompt. Re-read at thread start.

---

## Current version

`1.11.0.1` (MAJOR.MINOR.PATCH.BUILD) — see [`version.properties`](version.properties). Never revert this file.

`version.properties` holds two key sets. **Authoritative: the uppercase set**
(`MAJOR`, `MINOR`, `PATCH`, `BUILD`, `LAST_MAJOR`, `LAST_MINOR`), read by
`app/build.gradle.kts:11-16` and by both release workflows (`android-ci.yml`,
`android_debug_apk_release.yml`) for the tag. The APK file name uses the `versionName` Gradle
stamped (`output-metadata.json`). The camelCase set (`versionMajor`/`versionMinor`/
`versionPatch`/`versionBuild`) and the written-back `versionCode`/`versionName` lines are not
read by any build step. Their values are left as they are.

---

## Module boundaries

```
core/units       — SI value classes (length, angle, velocity). No Android, no OpenCV.
core/geometry    — table/rail/pocket geometry, segment & circle intersection. Pure Kotlin.
core/physics     — contact, spin, throw/squirt physics. Pure Kotlin.
core/aim         — the aim solver. Depends on units/geometry/physics only.
core/advisor     — shot advisor / candidate ranking. Depends on aim/geometry/physics.
core/projection  — homography, DLT pose-from-correspondences, Mat3/Vec3.
core/state       — shared state primitives used across core/* modules.

app/             — the Android application: domain/ (use cases, reducers, MVI state),
                   data/ (repositories, OpenCV-backed vision pipeline), ui/ (Compose),
                   view/ (custom canvas renderers), di/, delivery/, network/, update/, utils/.
feature_expert_ar/ — NOT a Gradle module: a source directory compiled into :app
                     (app/build.gradle.kts:170,174). ARCore-backed table-scan flow (analyzer,
                     session, GL background renderer). Reaches app code only through
                     arfeature/ArController and ArFeatureEntryPoint.
feature_mlmodel/   — NOT a Gradle module: an assets directory. app/build.gradle.kts:169 adds
                     it to :app's assets; it holds the merged 3-class (table / hole / side)
                     TFLite detector binary, no code.
wear/               — Wear OS companion (stroke sensor capture, message relay to phone app).
```

`core/*` modules are the one part of this codebase with a real regression net
(`./gradlew -Pcuedetat.coreOnly=true allTests` runs with no Android SDK). `app/`,
`feature_expert_ar/`, and `wear/` currently have thin or zero unit coverage —
treat changes there as unverified until you've read the call sites yourself.

---

## Invariants

Each invariant below states the reason it exists, per AGENTS.md's drift-detection
rule — if the reason no longer holds, the invariant is stale, not sacred.

- **`core/*` modules stay pure Kotlin (no Android/OpenCV/ARCore types).**
  Reason: it's the only part of the app that can be tested without an SDK or a
  device; any Android/CV dependency here silently kills that.
  Enforced by: the core-only build (`settings.gradle.kts:81`, the `coreOnly` flag, which
  skips every Android module) run in CI at `.github/workflows/android-ci.yml:72` and
  `.github/workflows/android_debug_apk_release.yml:97`. A stray `android.*` import fails to
  compile there only if the runner has no Android SDK on the classpath of those modules;
  the core `build.gradle.kts` files declare no Android dependency, which is what makes
  it fail. Not otherwise checked (no lint rule).

- **`CueDetatState.comparableFields()` stays in sync with the primary
  constructor field list** (`app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`,
  list at :260, `equals`/`hashCode` at :289/:295).
  Reason: `equals`/`hashCode` are hand-maintained over 119 fields; drift here
  silently breaks state-diffing used for recomposition/undo.
  **Unenforced**: no compiler check or test. Verify by counting both lists
  (119 and 119 when last recounted).

- **Building `:app` needs GitHub Packages credentials; the core does not.**
  There are no product flavors (no `foss`/`play`). `app/build.gradle.kts:378-380` depends
  unconditionally on `mwdat-core`, `mwdat-camera` and (debug) `mwdat-mockdevice`, which
  resolve only from the credentialed GitHub Packages repo declared at
  `settings.gradle.kts:29-70` (`gh_user`/`gh_token` in `local.properties`, Gradle
  properties, or `GH_ACTOR`/`GH_TOKEN` env). Without them an `:app` build fails to
  resolve (the settings file only logs a warning). The credential-free path is
  `./gradlew -Pcuedetat.coreOnly=true allTests`. A previous version of this file claimed
  a credential-free `foss` flavor; that never existed in the current tree.

- **`version.properties` is never reverted.** Reason: stated directly in
  AGENTS.md; Gradle (`app/build.gradle.kts:11-16`) and the release workflows read the
  uppercase keys from it. **Unenforced** mechanically (no hook or CI guard).

- **No feature ships partially wired.** Reason: this codebase has a real,
  recurring failure pattern — state added to `UiModel.kt`/reducers with no
  composable ever reading it (dead dialogs, dead screens, dead config fields).
  **Unenforced.** Before marking a feature done, grep
  that its state has a reader, not just a writer.

---

## Decisions

- **Multi-module split into `core/*` (2026, see [`REBUILD_PLAN.md`](REBUILD_PLAN.md)).**
  Reason: the pre-rebuild app had all aim/physics/geometry logic entangled
  with Android and OpenCV types, making it untestable without a device or
  emulator. Splitting the pure math into KMP modules under `core/` lets that
  logic run in a plain JVM test task.

- **`feature_mlmodel/` and `feature_expert_ar/` are source/asset directories compiled into
  `:app`, not modules.** Reason: split installs of the former dynamic-feature modules failed on
  real devices (see `docs/RELEASE.md`), so there is one build, one variant, no flavors.
  `ModelDelivery`/`ArFeatureDelivery` remain as no-op seams (`BundledModelDelivery`,
  `BundledArFeatureDelivery`); the on-demand path in `MergedTFLiteDetector.ensureModelReady`
  is effectively a retry of the asset load.

- **`MergedTFLiteDetector` is serialised by one lock.** Reason: `detect` (analyzer thread),
  `detectPool` (VisionRepository) and the model (re)load (IO) share one input buffer,
  output tensors and interpreter map; unlocked, they corrupt each other's tensors.
  Enforced at `data/MergedTFLiteDetector.kt` (`lock`, used by `loadMasterPackage`, `detect`,
  `detectPool`).

- **Billing/entitlement code was fully removed** (see `REBUILD_PLAN.md` Phase C,
  and `support/SupportLinks.kt`'s own comment on why no billing code remains).
  Reason: there is no Expert-tier paywall in the current app; Expert Mode is
  unlocked with no purchase gate. `Changelog.md` and `docs/RELEASE.md` are the
  places this decision needs to stay reflected — they drifted once already
  (see `docs/04_Feature_Specs/07_Feature_Camera_Calibration.md` for a parallel
  case: a spec/file describing something that was never actually wired up).

- **The AR table-scan flow is felt capture only.** One tap captures the felt
  colour, loads a default table model and hands off to AR tracking. Reason:
  pocket tapping (the former `CORNER_QUAD` corner taps and `POCKET_GUIDE`
  per-pocket guide) did not work and was deleted.

- **Pre-lock AR is metric and stationary in world space.** One ARCore depth/plane hit
  positions the selected physical table dimensions; when no hit exists, an initial
  camera-relative position is explicitly estimated. A changing camera pose changes
  only the perspective projection. User pan/rotation/zoom edit the world pose; zoom
  changes camera-to-table range, not table dimensions. Reason: artificial screen-space
  pitch and scaling moved/rotated/deformed the preview during camera motion.

- **The table is locked by hand, not detected.** Once tracking, a `lock` rail
  item appears above `ar`/`off`: the user lines the virtual table up over the real one and
  taps it. The virtual corners' current screen positions are cast onto the
  detected horizontal plane (`TableFrameHomography.screenToPlane`) and anchored;
  `ArTableSession` then drives the overlay from those anchors, rebuilding a rigid
  rectangle of the selected physical dimensions on the selected felt plane. Reason: the
  user's eye is the one table detector that has worked. The cast uses the
  infinite plane, not per-corner hit-tests, because plain felt is rarely mapped
  edge to edge by ARCore.

- **The table snaps to the felt to three degrees, and remembers.** Ghost at
  IoU 0.6, Lock snaps at 0.7, untouched pull at 0.85 (`TableSnapPolicy`).
  Poses are remembered per session, per location and for the last table,
  and an on-device learner predicts orientation from the compass. Reason:
  the user asked for snapping with persistence and a prediction model; the
  learner needs no training data, and the pose log feeds a future trained
  model.

- **Felt colour picks the table's plane.** At lock, points across the virtual
  table are sampled from the camera image; only felt-coloured ones
  (`FeltColorMatch`) are hit-tested, and the plane they hit most wins. Reason:
  ARCore finds the floor and furniture too, and a table pinned to the floor
  drifts by parallax as the user moves. Colour, not texture, is what felt has.

---

## Where things actually live (correcting known-stale doc claims)

- ARCore session/depth capability: `feature_expert_ar/.../ArTableSession.kt`
  (not `app/.../data/ArDepthSession.kt` — that file has never existed).
- AR frame bridge / GL background renderer: `feature_expert_ar/.../ArFrameProcessor.kt`,
  `feature_expert_ar/.../ArBackgroundRenderer.kt` (not under `app/.../data/`).
- Merged 3-class (table / hole / side) TFLite model: `feature_mlmodel/src/main/assets/ml/MASTER_POOL_MODEL.tflite`.
