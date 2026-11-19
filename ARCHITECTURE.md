# ARCHITECTURE.md

Companion to [`AGENTS.md`](AGENTS.md). What shouldn't live in a prompt. Re-read at thread start.

---

## Current version

`1.14.0.1` (MAJOR.MINOR.PATCH.BUILD) — see [`version.properties`](version.properties), which CI bumps on
every merge, so this line trails it. Never revert that file.

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
core/state       — shot state: sealed Interaction and ShotMode, per-concern slices. No other
                   module depends on it, and the app does not use it yet (see REBUILD_PLAN.md).

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
  list at :272, `equals`/`hashCode` at :301/:307).
  Reason: `equals`/`hashCode` are hand-maintained over 120 fields; drift here
  silently breaks state-diffing used for recomposition/undo.
  **Unenforced**: no compiler check or test. Verify by counting both lists
  (120 and 120 when last recounted).

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

- **With a visible table, no ball is ever outside it, by its own radius: the target, cue and obstacle
  balls, and the protractor's ghost cue ball.**
  Reason: ball positions are written from many reducers (drags, tap-to-place, detection follow,
  undo, reset) and none checked the table, so a ball could be dragged or carried off it; the ghost
  is derived (target centre + 2r at the aim angle), so an aim into a rail put it off the table with
  nothing reporting it (`isGeometricallyImpossible` never looks at the table).
  Enforced by: `BallBounds.confine` (`domain/BallBounds.kt:47`), which clamps the stored balls
  (`Table.clampBallCenter`, `view/model/Table.kt:89`) and moves the aim to the nearest angle that
  keeps the ghost on the table (`BallBounds.aimWithGhostOnTable`, `domain/BallBounds.kt:112`).
  Applied where state leaves `stateReducer` (`domain/StateReducer.kt:165`) and where
  `SnapReducer.reduce` returns (`domain/reducers/SnapReducer.kt:182`); a new ball writer outside
  those two paths is unenforced. Not covered: the massé ghost (`masseGhostBallCenter`), which the
  massé simulation derives after the reducers run, and banking mode, which has no ghost.

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

- **Hue is averaged and windowed as a circle.** `HueMath` (pure) holds the circular mean/spread
  of a hue histogram and the wrapping windows; `HueMask` (OpenCV) applies them. Reason: OpenCV hue
  wraps at 180, red felt straddles the seam, and a linear mean of 170 and 10 is 90.
  `FeltColorMatch` (Android HSV, degrees) already measured hue distance on the circle.

- **Tracks and anchors are matched with `Assignment` (Hungarian), not nearest-in-turn.** Reason:
  the greedy match is order-dependent and can leave a later detection without a track that an
  assignment giving everyone one would have found. Pairs are gated (`SNAP_PROXIMITY_THRESHOLD`,
  three times that for anchors), so the matching maximises matched pairs within the gate first,
  then minimises total distance.

- **The AR preview is put back by the felt only when it is suspect, and only by a sure fit.**
  The felt fit still never pulls a world-anchored table frame to frame. But for
  `SUSPECT_NANOS` after tracking returns from a loss, a fresh placement or a dropped lock, a fit
  of quality >= 0.8 (overlap discounted by how steady the last five fits were:
  `TableFitQuality`, `TableFitStabilizer`) that overlaps the projected table < 0.9 on three
  fits in a row re-seeds the preview from the fit's outline (`ArTableSession.seedPreviewFromQuad`;
  at most twice a window, `TableReseedPolicy`), never while the user is touching the table or
  Lock View is on. Heading is taken from the fit only if its orientation confidence is >= 0.5,
  and the end closest to the current one is kept (the felt can't tell head from foot). A locked
  anchor ARCore has stopped (or not tracked for 5 s) drops the lock rather than leave a "locked"
  table that is never drawn. Reason: after tracking loss ARCore may re-find the world somewhere
  else, the anchors then agree with themselves and nothing inside ARCore can say they are wrong;
  the image can.

- **Felt colour picks the table's plane.** At lock, points across the virtual
  table are sampled from the camera image; only felt-coloured ones
  (`FeltColorMatch`) are hit-tested, and the plane they hit most wins. Reason:
  ARCore finds the floor and furniture too, and a table pinned to the floor
  drifts by parallax as the user moves. Colour, not texture, is what felt has.

---

- **The app's logical plane crosses into the core through `ScreenPlane`: `core.x = screen.y`,
  `core.y = screen.x`, over one scale** (`core/geometry/.../ScreenPlane.kt`; the `PointF` adapter is
  `app/.../domain/core/TableFrame.kt`; `CoreBridge` delegates to it). Reason: the app's table is
  portrait with its long axis on logical y and y pointing down the screen; the core's is
  long-axis-x and right-handed. The old bridge crossed with `(x, -y)` and no swap, which left the
  long axis on core y: harmless for the aim solver (rotation-invariant), wrong for anything that
  uses the core `Table` (pockets, cushions), which is what the advisor needs. The swap is a proper
  rotation once the screen's y-down is counted, so a counter-clockwise turn as seen is
  counter-clockwise in the core, which keeps the sign of cut, throw and english meaning the same
  (`ScreenPlaneTest`, which runs under `:core:geometry:jvmTest`). The scale has one source:
  `TableFrameHomography` (Float, used by the AR code), widened to Double by `TableFrame`. It
  replaced two separately declared copies.

- **The advisor's balls carry identity and group** (`core/advisor/.../Balls.kt`: `TableBall`,
  `BallGroup`, `GroupRules`). Reason: it took bare positions, which cannot say "the first ball struck
  must be yours", so a combination could be recommended that hit the opponent's ball first.
  Combinations are legal only between two different balls of the shooter's own group; the 8 is never
  part of one.

- **`ShotPlanner` plans banks, kicks and legal combinations in the core; its probability is one model
  (`PrecisionModel`) shared with the direct-pot advisor** (`core/advisor/.../ShotPlanner.kt`,
  `PrecisionModel.kt`). Reason: the app's advisor scored each kind with its own hand-tuned confidence
  constant (`BANK_CONF = 0.55`, `KICK_CONF`, `COMBO_CONF`), which the rebuild had already called out as
  numbers nothing was calibrated against. Here a chain of contacts multiplies the precision demanded of
  the cue (distance over a ball diameter, and one over the cosine of the cut, at each), so a combination
  falls out of the same arithmetic as a direct pot, and a cushion costs a stated factor
  (`PrecisionModel.CUSHION_PRECISION`, an uncalibrated placeholder, as is the 0.6 degree aiming spread).
  Banks and kicks use the pure mirror law on the cushion centre line, so they assume no english on the
  rebound and a medium pace. Every shot's ghost ball must be wholly on the table, matching the
  `BallBounds` invariant on the app side.

- **The advisor aims the protractor, and the user can take the aim back** (`app/.../domain/advisor/AutoAim.kt`,
  `CoreShotAdvice.kt`). Reason: choosing the ball and the pocket is the point of the advisor, and a
  recommendation drawn beside an aim the user must then copy by hand is a second job. The advisor owns
  the target ball and the aim while it is on; dragging the target or rotating the aim sets
  `isAutoAimPaused` and it lets go, and a recommendation never moves a ball under a finger. A kick does
  not aim the protractor: the cue goes off a cushion first, so the straight line the protractor draws to
  the ghost would not be the shot. The tilt limit (`isTiltBeyondLimit`, which assumes shots run up the
  screen) is not applied to a shot the advisor chose. The advisor's balls are the detected ones and the
  hand-placed ones (all treated as the shooter's own); the protractor's target is its output, not an input.

- **Jitter is cut at the gate, not only in the filters** (`OrientationGate`, `ArMatrixHold`, `BallFollow`,
  `CoreShotAdvice.Signature`). Reason: the sensor filter, the AR corner filter and the table fit each smooth
  their own signal, but nothing decided whether a *tiny* change was worth the reduce, the matrix rebuild, the
  aim recompute and the redraw, so a phone on a table ran the whole pipeline on every sample. Each gate is a
  pure class with its constant derived in its KDoc and tested against synthetic noise. The advisor compares
  ball positions by distance against the last signature it acted on, not by rounding onto a grid, which flips
  its answer when a ball sits on a cell edge.

## Where things actually live (correcting known-stale doc claims)

- ARCore session/depth capability: `feature_expert_ar/.../ArTableSession.kt`
  (not `app/.../data/ArDepthSession.kt` — that file has never existed).
- AR frame bridge / GL background renderer: `feature_expert_ar/.../ArFrameProcessor.kt`,
  `feature_expert_ar/.../ArBackgroundRenderer.kt` (not under `app/.../data/`).
- Merged 3-class (table / hole / side) TFLite model: `feature_mlmodel/src/main/assets/ml/MASTER_POOL_MODEL.tflite`.
