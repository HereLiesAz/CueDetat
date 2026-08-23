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
      unused Sonatype snapshots repo; scope GitHub Packages to the play flavor
- [x] Root `build.gradle.kts`: KMP + Compose Multiplatform plugins
- [x] `gradle.properties`: JDK 21 toolchain, KMP flags
- [x] Core module build files

### B — The metric core (`:core:*`, KMP commonMain)
- [x] `:core:units` — Length/Angle/Speed value classes, SI-backed
- [x] `:core:geometry` — Vec2, Pose2, cushions, **pocket apertures**, intersections
- [x] `:core:physics` — speed-aware collision with **cut-induced throw**, squirt at
      cue-tip, swerve, cushion restitution + throw
- [x] `:core:projection` — CameraIntrinsics + TablePose + Projector (generalises
      the old `TableFrameHomography`, which was the one correct file in the repo)
- [x] `:core:aim` — AimSolver: ghost ball as seed, throw/squirt correct the line
- [x] `:core:advisor` — shot advisor ported onto the metric core
- [x] `:core:state` — sealed `InteractionMode`, per-concern state slices
- [x] Tests: known-good billiards constructions to sub-degree tolerance

### C — Donation model (replaces the paid gateway)
- [x] Delete all billing/entitlement/paywall/integrity code (~30 files)
- [x] Expert mode free for everyone; no gating
- [x] `SupportRepository` + donation sheet (external links, no billing dep)
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
- [x] Fix: banking/massé mutual exclusion via sealed mode
- [x] Fix: Beginner + glasses dead end
- [x] Hater mode out of the primary mode cycle

### E — Build/release hygiene
- [x] CI runs tests before building
- [x] Release publish gated on `refs/heads/main`
- [x] FOSS builds from a clean clone (Meta Wearables → play only)
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
- **`ml/`** — 52 MB of unreferenced training artifacts. `git rm --cached` would
  not reclaim clone cost (the blobs stay in history); needs a history rewrite or
  an LFS migration, and an explicit decision from the owner.
- **Pin the `@main` GitHub Actions** to commit SHAs. Flagged in place; the SHAs
  could not be resolved from the build sandbox.
- **Instrumented tests** still need an emulator matrix in CI.
