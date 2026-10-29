# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed — audit of the whole app (UI/UX and engine)

Two independent read-only audits, then fixes. Nothing here was run on a device; Kotlin/Android code
is compiled by CI only. Core (`:core:*`) tests, the Python tests and the capture-relay tests were run.

- **Expert inherited Beginner's locked view.** `isBeginnerViewLocked` is saved and gates the CV
  analyzer, compass follow and the table fit, but only Beginner's branch set it; Expert now clears it.
- **"Tap the cue ball" re-armed itself.** The first-run prompt fired on every vision frame with one
  confirmed candidate, undoing a disarm and blocking drags while armed. Now once per camera session,
  never after the cue ball is placed.
- **Reset only toggled the first two layouts.** After undo/redo it restored the snapshot taken before
  the first reset, discarding whatever had been set up since. A change since the last toggle now makes
  Reset start fresh (the rail label follows).
- **Reducer-written notices never faded.** AR/cloud notices, lock failures and CV hints sat on screen
  at display size until overwritten. They fade after five seconds; tracking-lost and camera-start
  failures (`ArNotice(persistent = true)`) stay until resolved. Shot warnings now name the cause.
- **Brightness and Glow stick dialogs had no way to open.** Drawer items added.
- **Felt capture could save an invented green.** The shutter waits for a real sample; the dead "Move"
  button and duplicate long-press are gone; the shutter has an accessibility label.
- **"AR Tracking" badge, lock and cloud items** now follow ARCore (badge only with a live projection;
  lock/cloud hidden without ARCore).
- Lock View leaves static Beginner; Mode and Orientation items show what a tap will apply; the spin
  dial stays on screen; a recreated Activity releases a stale AR lock; Tutorial replays the current
  mode's; selection rings wear the balls' colours; two-finger rotate/pan wait for intent; the
  three-finger height drag judges accumulated travel; dynamic Beginner keeps two-finger rotation as
  documented; edge drags are excluded from the system back gesture; the canvas and HUD got
  descriptions, a text halo and an "~" on the estimated distance.
- Permissions: Bluetooth is explained; a camera refusal no longer closes the app (Settings link when
  the system will not ask again); camera and ARCore start failures are shown.
- Removed controls that changed nothing: the Canny sliders and "auto-tune" (no code read them).
- **Planar-YUV frames had scrambled chroma** (`ImageProxy.toMat`); **AR frames assumed portrait**
  (rotation hard-coded to 90, now read from ARCore's own mapping); the adaptive frame-rate throttle
  watched a pocket detector that never sees balls, costing a TFLite pass per frame (now a frame
  difference, and the model is no longer run there); locked/Hater frames were converted before being
  dropped; Beginner-locked and similar state is no longer restored from disk; the per-frame AR events
  coalesce instead of filling the event channel; `TableScanAnalyzer` averaged hue linearly.
- **Core:** `AimSolver.ballFraction` was the cosine of the cut (a half-ball hit is 30°, not 60°) and
  its test restated the formula; `Contact` followed/drew harder than the miscue limit.
- **Capture relay:** rate limit keyed on a client-chosen header now also keys on client IP.
- **CI/repo:** `dependabot.yml` was an empty template; `android-ci.yml` no longer publishes a second,
  differently-signed debug APK unless `ANDROID_CI_PUBLISH` is set; the Jules issue workflows only
  accept the owner's side.
- **Docs:** PrivacyPolicy (citations removed; Cloud Anchors, orientation log, install id), RELEASE,
  ALGORITHMS, ARCHITECTURE, REBUILD_PLAN, README and the UI guides now match the code; the README and
  plan say plainly that the metric core is not yet driving the on-screen aim.


### Fixed — AR table fitting

- **Table rotation followed the fingers backwards in AR.** The world-frame yaw is counter-clockwise
  seen from above, the gesture and swiper are clockwise-positive, and nothing converted between
  them (`TableFrameHomography.clockwiseToWorldYaw`). The sensor path was already right.

### Added — Ball detector training from the labelling pipeline

- **`ml/cuedetat_ball_detector_kaggle.py` (+ `.ipynb`) trains a YOLOv8n ball detector** from the
  Label Studio pipeline's export, with a per-class gate, TFLite/ONNX export and a report.
  `ml/dataset/make_ball_dataset.py` turns `ls_to_yolo.py`'s seventeen classes into the app's
  cue / eight / solid / stripe (or keeps all seventeen); `ball_classes.py` holds the mapping
  (`prelabel.py` now imports `BALL_LABELS` from it), with tests (`python3 -m unittest
  test_ball_classes`). Not wired into the app, and nothing has been trained yet.

### Fixed — Hue wraps; ball tracks match optimally

- **Hue is a circle everywhere it is averaged or windowed.** OpenCV hue runs 0..180 and wraps, red
  and maroon felt sits across the seam, and the linear mean of hues 170 and 10 is 90 (green). The
  felt auto-detector averaged hue linearly, and the felt masks (`TableFitter`, `CvBallDetector`, the
  CV debug mask) used a linear window that silently matched nothing across the seam. Now a circular
  mean and spread (`HueMath`, from a hue histogram) and wrapping `inRange` windows (`HueMask`).
  (`FeltColorMatch` and the plane-estimate mask already wrapped.)
- **Detections are matched to last frame's tracks, and the cue/target anchors to detections,
  optimally** (`Assignment`, the Hungarian algorithm) instead of nearest-in-turn, which let an early
  detection take the track a later one needed: two close balls could swap or drop a track, and both
  anchors could follow the same candidate. From the Pool-Aid paper's matching idea.

### Changed — AR tracking loss and the felt fit

- **A table that tracking lost no longer stays lost.** An unlocked table whose anchor ARCore
  stopped is placed again (it stayed hidden for the rest of the session); a locked one whose anchor
  stopped, or went untracked for 5 s, drops its lock with a message instead of staying "locked" and
  never drawn, and an Unlock that can't transfer its pose gives up after 3 s instead of retrying
  forever. Tracking lost for 1.5 s says why (too dark, too fast, not enough to see) and clears when
  it returns. (`ArTrackingMessages`, `ArNotice`, `ArTableLockLost`.)
- **A table that comes back wrong is put back by the felt.** For 12 s after tracking returns, a
  table is placed or its lock is dropped, a sure fit (quality >= 0.8) that disagrees with the
  projected table on three fits in a row re-seeds it from the fit's outline: position, heading (if
  the fit is sure of it) and size (if within 0.8-1.25x). At most twice, never while the table is
  being touched or Lock View is on. (`TableReseedPolicy`, `QuadOverlap`,
  `ArTableSession.seedPreviewFromQuad`.)
  Not covered: a *locked* table that ARCore re-finds in the wrong place with its anchor still
  tracking (fits are off while locked): Unlock, and the table is re-seeded from the felt.
- **Saved-placement restore keeps looking.** ARCore answers "no match" until the camera has seen
  enough; Restore now retries every 2 s for up to a minute while the user scans, and each failure
  says what happened (expired, not authorised, service down, no match) instead of an enum name.
- **Felt fit.** The outline is scored on edge distance as well as overlap (distance transform of
  the felt boundary), the felt colour is re-measured inside the answer and the fit refined against
  it, the reported outline is the median of the last five fits, and each fit carries a quality
  (overlap x steadiness) and an orientation confidence (how sharply the score worsens when turned
  3 degrees, and how far the quarter-turned reading trails). (`TableFitter`, `TableFitStabilizer`.)

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