# Cue d'État — Ground-Up Rebuild

Working plan for the re-envisioning. Tracks what is done and what remains.

## Principles

1. **The world is metric.** No dimensionless "logical units". Table, balls,
   pockets, spin, speed and distance are SI (metres, radians, m/s) everywhere.
   Conversion to pixels happens once, at draw time, through a real camera model.
2. **The core is pure.** All game model, geometry, physics and aim maths lives in
   Kotlin Multiplatform `commonMain` with zero Android/OpenCV/ARCore types, so it
   is testable on any machine without an emulator.
3. **Illegal states are unrepresentable.** Mutually exclusive modes are sealed
   types, not independent booleans.
4. **Free, donation-supported.** No paywall, no entitlement, no billing.

## Phases

### A — Toolchain (JDK 21, latest stable, KMP/CMP)
- [x] Version catalog: every version verified latest-stable from maven-metadata
- [x] `settings.gradle.kts`: KMP core module graph; drop `mavenLocal()` and the
      unused Sonatype snapshots repo; GitHub Packages is scoped to `com.meta.wearable` only
      (there are no flavors; building `:app` needs those credentials)
- [x] Root `build.gradle.kts`: KMP + Compose Multiplatform plugins
- [x] `gradle.properties`: JDK 21 toolchain, KMP flags
- [x] Core module build files

### B — The metric core (`:core:*`, KMP commonMain)
- [x] `:core:units` — Length/Angle/Speed value classes, SI-backed
- [x] `:core:geometry` — Vec2, Table (cushions, **pocket apertures**), intersections
- [x] `:core:physics` — speed-aware collision with **cut-induced throw**, squirt at
      cue-tip, cushion restitution + throw (no swerve: the word appears only in a comment)
- [x] `:core:projection` — CameraIntrinsics + TablePose + Projector (generalises
      the old `TableFrameHomography`, which was the one correct file in the repo)
- [x] `:core:aim` — AimSolver: ghost ball as seed, throw/squirt correct the line
- [x] `:core:advisor` — shot advisor ported onto the metric core
- [x] `:core:state` — sealed `Interaction`, per-concern state slices
- [x] Tests: known-good billiards constructions to sub-degree tolerance
- [~] **None of `:core:advisor`, `:core:state` or `:core:projection` is used by `:app`, `:wear` or
      `:feature_expert_ar` (only their own tests). `CoreBridge` has no callers.** `:core:aim`,
      `:core:physics` and `:core:geometry` reach the app only through that bridge, so the shipped
      aim is still the ghost-ball geometry in `UpdateStateUseCase`, and the README's throw/squirt
      claims describe the core, not what the screen shows. Wiring is phase D.

### C — Donation model (replaces the paid gateway)
- [x] Delete all billing/entitlement/paywall/integrity code (~30 files)
- [x] Expert mode free for everyone; no gating
- [x] `SupportLinks` + donation sheet (external links, no billing dep)
- [x] Drop `billing-ktx` / `play-integrity` / credentials deps
- [x] Remove entitlement fields from state and reducers

### D — Android rewiring
- [ ] **Not done.** Delete the fake-3D projection (`Perspective.kt` visual-pitch remap, 30% roll,
      hardcoded `-32f` viewing distance)
- [~] Core provides it and `CoreBridge` exposes it; the renderer still uses the
      old path. Migration is the next pass.
- [ ] **Not done.** Fixes itself once the renderer moves to `:core:projection`,
      where a ball centre is `(x, y, radius)` and lift is not a separate term.
- [~] `CoreBridge.distanceBetween` returns a real length; `TopControls` still
      renders the old `1200 / screenRadius` value.
- [~] Banking/massé mutual exclusion: a sealed `ShotMode` exists in `:core:state` (unused by the
      app); the app still has two booleans, kept exclusive by a clear in `ToggleReducer` and
      `SpinReducer`
- [x] Fix: Beginner + glasses dead end
- [x] Hater mode out of the primary mode cycle

### E — Build/release hygiene
- [x] CI runs tests before building
- [x] Release publish gated on `refs/heads/main`
- [ ] ~~FOSS builds from a clean clone~~ — not true: no flavors exist and `:app` needs GitHub
      Packages credentials for Meta Wearables. Only `-Pcuedetat.coreOnly=true` is credential-free.
- [x] `build.log` untracked, `.gitignore` collapsed (2,947 lines to ~50).
      `ml/` deliberately left tracked — see Follow-up.
- [~] `blank.yml` deleted; `@main` actions flagged in place but NOT pinned —
      the SHAs could not be resolved from the build sandbox.
- [x] Collapse the duplicated `Dev_Guide/` + `docs/` trees

## Follow-up (not done in this pass)

- **Delete `CueDetatState.comparableFields()`.** Convert the three `FloatArray?`
  fields (`relocaliserDeltaQ`, `lockedHsvColor`, `lockedHsvStdDev`) to a small
  content-comparing wrapper so the compiler-generated `equals()`/`hashCode()` are
  correct on their own. 38 call sites across seven files. Verified in sync at
  120/120 today, but nothing enforces it.
- **Migrate the renderer onto `:core:projection`.** `Perspective.kt`'s fudged
  pitch curve, the 30% roll and the hardcoded viewing distance should go, and
  every draw site should read one `ProjectedBall` rather than re-deriving lift.
  This is the change that fixes the guide lines not landing on the ball.
- **`ml/`** — about 79 MiB of tracked training artifacts (sum of `git ls-files ml`). `git rm --cached` would
  not reclaim clone cost (the blobs stay in history); needs a history rewrite or
  an LFS migration, and an explicit decision from the owner.
- **Pin the `@main` GitHub Actions** to commit SHAs. Flagged in place; the SHAs
  could not be resolved from the build sandbox.
- **Instrumented tests** still need an emulator matrix in CI.
