# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed — AR table fitting

- **Table rotation followed the fingers backwards in AR.** The world-frame yaw is counter-clockwise
  seen from above, the gesture and swiper are clockwise-positive, and nothing converted between
  them (`TableFrameHomography.clockwiseToWorldYaw`). The sensor path was already right.

### Added — Cue / Target ball buttons

- **Two rail buttons, coloured as the virtual balls are drawn (the cue ball's ring, the target's).**
  Tap one, then tap that ball on the screen: set both balls up, then drag to aim. A tap snaps to a
  detected ball within three ball radii, otherwise the ball goes exactly where the finger landed;
  pressing the armed button again cancels. Works with the camera off. Placing the cue ball carries
  on to the target only while the target hasn't been placed.
  (`MainScreenEvent.BeginBallSelection`, `BallPlacement`.)
- **A pick is completed by the tap-up alone.** The touch-down used to place the ball from a
  confirmed detection and the release then answered the next prompt too, so one tap could set the
  cue and the target to the same ball. Previously a tap also moved nothing unless a detection was
  near (it only set an anchor), and snapped to the nearest detection however far away.

### Changed — AR table jitter

- **The table's projected corners are smoothed (`ScreenCornerSmoother`, a One Euro filter).**
  Tracking noise at rest is filtered hard; real motion (walking, panning, resizing) barely at all,
  so the table steadies without swimming. Restarts on lock/unlock, tracking loss and any jump over
  150 px. Not a fix for tracking that is lost or wrong.

### Fixed — AR sliders pinned at maximum

- **The zoom slider is a self-centring jog under ARCore world tracking.** There, zoom moves the
  table along the camera line, so an absolute position meant nothing and ran into its end stop with
  the table still the wrong size. Dragging now nudges the range (`NudgeTableRange` →
  `ArController.nudgeTableRange`, clamped 0.65–8 m by the session) and the thumb springs back to
  centre on release; the +/- buttons nudge one unit. Unchanged without ARCore.
- **A resting thumb no longer drives the table-height slider.** The three-finger height drag now
  needs three fingers all moving the same way.

### Added — AR table fitting

- **Resize without zooming.** In AR the pinch now resizes the table in place (`tableFitScale`,
  `TableScaleChanged`) instead of dollying it toward or away from the camera; the zoom slider keeps
  the range. Applies before and after Lock.

### Changed — Navigation & Tutorial

- **AzNavRail upgraded 10.1 → 10.18.**
- **Tutorial rebuilt on AzNavRail's status-driven guidance framework.** The bespoke scripted tutorial
  (`TutorialOverlay` canvas highlights, `TutorialReducer` step machine, tutorial state/events, and the
  `HAS_SEEN_*` DataStore flags) was removed and reimplemented with AzNavRail 10.18's guidance DSL
  (`azStatus`/`azEdge`/`azGoal`/`azGuidanceTarget`) in `AzNavRailMenu`, with a new
  `TutorialGuidanceTargets.kt` mapping app state to the on-screen highlight shapes (target/ghost/cue
  ball, aiming line, zoom slider). All five flows reuse their existing step text verbatim; the
  framework draws the spotlight and persists completion (`az_navrail_completed_goals`). The tutorial is
  now non-blocking.

### Security — Dependencies

- **Netty forced to 4.1.135.Final** across all modules to clear transitive CVEs (HTTP/2
  CONTINUATION-flood / MadeYouReset DoS, SNI 16 MiB allocation, decompression bombs, request
  smuggling, IPv6 subnet-filter bypass, TLS hostname-verification bypass, and more).

### Removed — Billing

- **The entire billing/entitlement/paywall stack has been deleted (~30 files).** Expert Mode
  is free for everyone, unconditionally — no `expert_mode_unlock` product, no paywall, no
  entitlement check, no trial, and nothing to restore. `billing-ktx` and `play-integrity`
  dependencies were dropped, entitlement fields were removed from state and reducers, and a
  `SupportRepository` + donation sheet (external links only, no billing dependency) replaced
  the paid gateway. See `README.md` ("Free, and how it stays that way") and `REBUILD_PLAN.md`
  (Phase C) for the reasoning: entitlement was read from an unsigned, client-persisted
  DataStore blob with no server ever contradicting it, and a debug billing/tester-license
  console shipped to every user in every build with no `BuildConfig.DEBUG` gate.

### Changed — Battery & processing efficiency

- **Adaptive CV pipeline throttle.** The main vision/ML pass (`VisionRepository.processImage`)
  used to run on every camera frame (~30fps). It now runs at ~30fps only while the scene is
  moving and widens to ~12fps once detections are stable, gated by an allocation-free time
  check in `VisionAnalyzer` so throttled frames skip the per-pixel YUV→RGB conversion entirely.
- **Adaptive sensor rates.** The rotation-vector sensor registers at `SENSOR_DELAY_UI` (~15Hz)
  and only bumps to `SENSOR_DELAY_GAME` while the phone is being moved; the shake accelerometer
  dropped to `SENSOR_DELAY_UI`. Previously both ran at ~50Hz continuously.
- **Hater-mode physics** stepped at 60fps unconditionally; now 30fps with an idle-pause that
  stops stepping once the die settles and resumes on the next input.
- **Per-frame allocations removed.** Pooled the downscaled ML bitmap in `VisionRepository`;
  hoisted per-frame `Paint`/`Path` allocations out of the `BallRenderer`/`LineRenderer` draw
  loops into reused fields; LRU-bounded the glow-paint cache.
- **TFLite** CPU inference now scales threads to the device (was hardcoded to 2). A GPU/NNAPI
  delegate path was added but is gated off pending on-device validation (thread-affinity and
  in-graph-NMS constraints).
- **Entitlement re-verification** on app resume is TTL-coalesced and the redundant second
  foreground trigger was removed; table-scan location prefers the cached fix and time-bounds
  any active GPS request.

## [0.9.1] - 2025-08-21

### Fixed

- Corrected the transformation pipeline to ensure all 2D transformations (zoom, rotation, placement) are applied *before* 3D perspective transformations (pitch/tilt). This resolves a rendering issue that caused improper "roll" when the view was tilted.

## [0.9.0] - 2025-07-30

### Changed

- Overhauled the physics simulation in Hater Mode, replacing the previous engine with Google's
  LiquidFun for improved stability and performance.
- Adjusted the size of the Hater Mode triangle to better contain its text, and then reduced it to a
  more reasonable size.

### Fixed

- Resolved a critical bug that prevented the main menu and navigation rail from opening correctly by
  fixing the underlying state management logic.
- Eliminated violent vibrations and erratic behavior of the triangle in Hater Mode by implementing a
  stable, synchronous physics update loop.

### Removed

- Removed the `kphysics` library dependency, which was the source of the physics instability.