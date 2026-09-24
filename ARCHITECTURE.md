# ARCHITECTURE.md

Companion to [`AGENTS.md`](AGENTS.md). What shouldn't live in a prompt. Re-read at thread start.

---

## Current version

`1.10.8.566` — see [`version.properties`](version.properties). Never revert this file.

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
feature_expert_ar/ — ARCore-backed table-scan flow (analyzer, session, GL background renderer).
                     Depends on app's domain/data contracts, not the other way around.
feature_mlmodel/   — asset-only dynamic-feature module; ships the merged TFLite pocket/ball
                     detector binary, no code.
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
  device; any Android/CV dependency here silently kills that. Enforced by
  module `build.gradle.kts` dependency declarations under `core/` — not
  compiler-enforced, so a careless `import` can violate it without a build
  failure. Verify by grepping the module's imports before merging.

- **`CueDetatState.comparableFields()` stays in sync with the primary
  constructor field list** (`app/src/main/java/com/hereliesaz/cuedetat/domain/UiModel.kt`).
  Reason: `equals`/`hashCode` are hand-maintained over ~120 fields; drift here
  silently breaks state-diffing used for recomposition/undo. Not compiler-enforced
  — verify by counting both lists before touching `UiModel.kt`.

- **The `foss` flavor builds with zero credentials.** Reason: an outside
  contributor with a clean clone must be able to build `foss` without a
  `local.properties` file or any secret. Enforced by the credential-fallback
  logic in `settings.gradle.kts` (Meta Wearables DAT maven repo is scoped to
  `com.meta.wearable`, consumed only by `play`) and by `app/build.gradle.kts`
  flavor-specific dependency blocks. Breaking this means `play`-only code or
  deps leaked into the shared source set.

- **`version.properties` is never reverted.** Reason: stated directly in
  AGENTS.md; it's the single source of truth for build/version numbering
  across CI release workflows.

- **No feature ships partially wired.** Reason: this codebase has a real,
  recurring failure pattern — state added to `UiModel.kt`/reducers with no
  composable ever reading it (dead dialogs, dead screens, dead config fields).
  Not enforced by anything mechanical. Before marking a feature done, grep
  that its state has a reader, not just a writer.

---

## Decisions

- **Multi-module split into `core/*` (2026, see [`REBUILD_PLAN.md`](REBUILD_PLAN.md)).**
  Reason: the pre-rebuild app had all aim/physics/geometry logic entangled
  with Android and OpenCV types, making it untestable without a device or
  emulator. Splitting the pure math into KMP modules under `core/` lets that
  logic run in a plain JVM test task.

- **`feature_mlmodel` is an asset-only dynamic-feature module, not code.**
  Reason: keeps the merged TFLite model binary out of the base APK/AAB,
  letting it be delivered on demand via Play Feature Delivery on the `play`
  flavor. `foss` includes it unconditionally since there's no Play Store
  delivery mechanism available to that flavor.

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

- **The table is locked by hand, not detected.** Once tracking, the rail's `ar`
  button reads `lock`: the user lines the virtual table up over the real one and
  taps it. The virtual corners' current screen positions are cast onto the
  detected horizontal plane (`TableFrameHomography.screenToPlane`) and anchored;
  `ArTableSession` then drives the overlay from those anchors. Reason: the
  user's eye is the one table detector that has worked. The cast uses the
  infinite plane, not per-corner hit-tests, because plain felt is rarely mapped
  edge to edge by ARCore.

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
- Merged pocket/ball TFLite model: `feature_mlmodel/src/main/assets/ml/MASTER_POOL_MODEL.tflite`.
