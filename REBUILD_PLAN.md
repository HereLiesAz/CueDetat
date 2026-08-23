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
- [ ] `gradle.properties`: JDK 21 toolchain, KMP flags
- [ ] Core module build files

### B — The metric core (`:core:*`, KMP commonMain)
- [ ] `:core:units` — Length/Angle/Speed value classes, SI-backed
- [ ] `:core:geometry` — Vec2, Pose2, cushions, **pocket apertures**, intersections
- [ ] `:core:physics` — speed-aware collision with **cut-induced throw**, squirt at
      cue-tip, swerve, cushion restitution + throw
- [ ] `:core:projection` — CameraIntrinsics + TablePose + Projector (generalises
      the old `TableFrameHomography`, which was the one correct file in the repo)
- [ ] `:core:aim` — AimSolver: ghost ball as seed, throw/squirt correct the line
- [ ] `:core:advisor` — shot advisor ported onto the metric core
- [ ] `:core:state` — sealed `InteractionMode`, per-concern state slices
- [ ] Tests: known-good billiards constructions to sub-degree tolerance

### C — Donation model (replaces the paid gateway)
- [ ] Delete all billing/entitlement/paywall/integrity code (~30 files)
- [ ] Expert mode free for everyone; no gating
- [ ] `SupportRepository` + donation sheet (external links, no billing dep)
- [ ] Drop `billing-ktx` / `play-integrity` / credentials deps
- [ ] Remove entitlement fields from state and reducers

### D — Android rewiring
- [ ] Delete the fake-3D projection (`Perspective.kt` visual-pitch remap, 30% roll,
      hardcoded `-32f` viewing distance)
- [ ] One projection path from real camera intrinsics
- [ ] Fix: guide lines now share the ball's projected position (the lift bug)
- [ ] Fix: real distance readout, or none
- [ ] Fix: banking/massé mutual exclusion via sealed mode
- [ ] Fix: Beginner + glasses dead end
- [ ] Hater mode out of the primary mode cycle

### E — Build/release hygiene
- [ ] CI runs tests before building
- [ ] Release publish gated on `refs/heads/main`
- [ ] FOSS builds from a clean clone (Meta Wearables → play only)
- [ ] Untrack `ml/` (52 MB) and `build.log`; collapse `.gitignore`
- [ ] Pin third-party actions; delete `blank.yml`
- [ ] Collapse the duplicated `Dev_Guide/` + `docs/` trees

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
