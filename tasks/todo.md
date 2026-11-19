# Auto-select the shot, and combination shots

Goal (owner, 11/18/2026): the app chooses which ball to hit and where, aims the protractor at it,
and covers combination shots. Decisions taken with the owner:

- **Auto-aim, with manual override.** The target ball, pocket and aim follow the best shot; dragging
  the target or the aim by hand pauses it until it is re-armed.
- **Legal 2-ball combinations only.** Cue hits your ball A, which drives your ball B into a pocket. The
  first ball must be your group; the 8 is never part of one.
- **Engine: port into `:core:advisor` and wire it in** (REBUILD_PLAN phase D), not the app's heuristic
  advisor.
- **Hand-placed balls count, and are all treated as the shooter's own.** No per-ball typing UI.
- **v1 ships without cue-leave (position spin) and safeties.** Core has no cue-ball path after contact.
  The app advisor is unreachable today (`SHOW_ADVISOR = false`, forced off at load), so nothing
  regresses for users. Both follow once core has a post-contact path.

## What the code looked like when this was planned (verified, 11/18/2026)

- No bridge: `CoreBridge` flipped y only and has no callers. The app table is portrait (long axis
  logical y, y down); the core's is long-axis-x. Pocket order differs; scanned pockets carry no mouth
  width or facing.
- Core has no multi-cushion tracer. `Cushion.rebound` and `Table.firstCushionHit` have no callers
  outside tests. `Contact.resolve` returns directions only (no speed after contact), so a chain has
  no speed model. `firstCushionHit` returns null near pocket mouths.
- Core's advisor takes bare `Vec2`s: no identity, no group.
- Nothing sets the aim from code. Collisions to handle: a stale `targetCvAnchor` makes `SnapReducer`
  pull the target back; `BallBounds.confine` rewrites the aim if the chosen ghost is near a rail;
  `isTiltBeyondLimit` assumes shots go up-screen.
- The app advisor's combos took ball A from every detected ball, including the opponent's.
- The advisor's input ignores `obstacleBalls`, and with no detections it returns null.

## Phases (one PR each)

- [x] **0.** Ball bounds on the table, ghost cue ball included (#322, merged).
- [ ] **1. Frame and ball model** (this PR)
  - [x] `core/geometry` `ScreenPlane`: app portrait y-down plane <-> core plane, tested in core.
  - [x] `Table.nearestPocketIndex`: a pocket known by position onto the core's pockets.
  - [x] `core/advisor` `TableBall`, `BallGroup`, `GroupRules` (pottable, first contacts, legal combinations).
  - [x] App `TableFrame` (PointF adapter); `CoreBridge` delegates; one source for the scale.
  - [ ] CI green on the app side (`TableFrameTest`; `:app` cannot be built in the authoring environment).
- [x] **2. Core search** (`core/advisor`: `ShotPlanner`, `PrecisionModel`)
  - [x] Banks and kicks, one cushion: mirror across the cushion's *centre line* (the ball's centre bounces),
        pure mirror law. Not used yet: `Cushion.rebound` (pace and spin on the rail), two or more cushions.
        Kicks only when the straight shot is not there; the cushion point is a fixed point with the throw-
        corrected ghost (4 passes, 1 mm tolerance).
  - [x] Legal 2-ball combinations: `AimSolver` twice (work back from the pocket: where the first ball meets
        the second, then where the cue meets the first), `GroupRules.isLegalCombination`, obstruction on every
        leg with moved balls excluded.
  - [x] `ShotPlan` (kind, first and pocketed ball ids, pocket index, ghost, cue path, ball paths, cuts,
        probability). The probability is one shared model: legs multiply the precision demanded, each cushion
        halves it (placeholder). The direct case is unchanged (the old hand-computed test still passes).
  - [x] No shot is offered whose ghost ball (or a striking ball's contact position) would be off the table.
  - [x] Hand-worked layouts for each kind (`ShotPlannerTest`).
  - [ ] Not done, deliberately: 3+ ball chains, safeties, position play, speed/spin choice.
- [x] **3. Wiring** (`CoreShotAdvice`; `:app` cannot be built in the authoring environment, so CI is the first compile)
  - [x] State -> core input: cue = detected, else hand-placed; balls = detected (group from CV against the target
        type; unclassified are only avoided) plus hand-placed obstacle balls (all "mine"), deduplicated. The
        protractor's target is the advisor's output, not a ball. Pockets and table come from the core table for the
        chosen size (the same table the logical plane is built from), so scanned pocket positions add nothing.
  - [x] Driver key covers table size, group, calibration, detections and hand-placed balls.
  - [x] Core plan mapped onto `RecommendedShot` (HUD and renderer unchanged): path cue -> ghost -> ball -> pocket,
        banks via the cushion, combos A -> meeting point -> B -> pocket, kicks with `approachOrigin`.
  - [x] Rail toggle re-enabled (`SHOW_ADVISOR`), and the load no longer forces `isAdvisorEnabled = false`.
  - [x] The old app `ShotAdvisor`, `AdvisorInputAdapter`, `AdvisorInput` and `ShotAdvisorTest` are deleted
        (owner's call, to avoid confusion). `SpinPhysicsCalculator` stays: the spin path still uses it.
- [x] **4. Auto-aim** (`AutoAim`; first compiled by CI, like phase 3)
  - [x] A new recommendation becomes the aim: target ball, ghost and aim angle (the equivalent angle nearest the
        current one), `targetCvAnchor` cleared so `SnapReducer` cannot pull the target back. Applied only while
        the advisor is on, not paused, not banking or massé, not choosing a ball, and not mid-gesture.
  - [x] Manual override: dragging the target or rotating the aim (`GestureReducer`) sets `isAutoAimPaused`
        (transient, so launches start armed). `ResumeAutoAim` (rail item "Auto-aim", shown only while paused)
        re-applies the current recommendation at once; switching the advisor on starts armed. No new routed
        event is unrouted (`StateReducerRoutingTest`).
  - [x] Kicks do not aim the protractor (the cue goes via a cushion, so its straight line to the ghost is not the
        shot); the gold overlay shows them. Banks and combos aim at the struck ball.
  - [x] `isTiltBeyondLimit` is not applied to a shot the advisor chose (`AutoAim.drives`); a shot the user aims
        is still held to it.
  - [x] The planner already offers no shot whose ghost is off the table, so `BallBounds.confine` has nothing to
        move in the chosen aim.
  - [ ] Known gap: the planner's cue ball may be a *detected* one while the protractor's shot-line anchor is the
        hand-placed one (or the bottom of the screen when there is none). Placing the cue ball from the plan is
        not done; the overlay's gold path is always drawn from the planner's cue.
- [ ] **5. Device validation.** Cannot be done in the authoring environment; CI proves compile and unit tests only.
      Aim quality, probabilities (uncalibrated), and performance on a real table are open until then.
- [ ] **Later:** cue-leave and safeties on core once it has a post-contact cue path; massé ghost off-table.

## Risks

- Nothing here proves the picks are good. Probabilities are placeholders (aiming sigma 0.6 degrees is not fitted).
- Recompute cost grows with targets x pockets x all balls (combos) x rails (banks); not measured.
- `CueDetatState.comparableFields()` is hand-maintained over ~120 fields (ARCHITECTURE invariant, unenforced):
  every new state field must be added to it.

# Stabilise the camera and the table placement (owner, 11/19/2026)

Goal: less jitter on screen, and less wasted work behind it. Four PRs, each tested with synthetic noise on the
JVM (no device here; every change to how the camera *feels* still needs a look on a phone). Findings were
checked against the source on 11/19/2026.

- [x] **1. Quiet the churn** (low risk)
  - [x] `OrientationGate`: a sensor orientation within 0.03 degrees (about a pixel) of the one last applied is
        dropped before the reducers; drift still gets through as steps that small.
  - [x] `ArMatrixHold`: a table matrix that goes missing for up to 250 ms while the camera is *tracking* is held
        instead of blinking the table off; a camera tracking loss still hides it at once and forgets the hold.
  - [x] `BallFollow` in `SnapReducer`: an anchored ball ignores detection noise under 1 logical unit and follows
        a real move with no lag.
  - [x] Advisor driver compares `CoreShotAdvice.Signature`s with a 2-unit tolerance, one to one by ball kind,
        against the last one acted on; the old key truncated to whole units and flipped on a cell edge.
  - [x] Corrected the stale `UpdateType.MATRICES_ONLY` comment. It also runs aiming, and that is **needed**: the
        shot line's anchor is a screen point mapped through the matrices. Not "redundant work" as first reported.
- [x] **2. Sensor filter**: `OrientationFilter`, a One Euro filter per axis on time, not on samples: one filter at
      16 and 50 Hz (lag at 5, 20, 60 deg/s identical to 0.1 deg), yaw has its own speed (a yaw-only turn at
      20 deg/s lagged ~50 degrees after ten seconds; now under 1), rest noise 0.05 -> ~0.006 degrees, step
      settles in ~60 ms, the rate switch follows its own speed estimate (noise reads under 1.1 deg/s against a
      3 deg/s threshold). Constants: min cutoff 0.1 Hz, beta 0.25, max 8 Hz; beta chosen on a sweep (lag at
      5 deg/s halves from 1.3 to 0.6 degrees, rest noise unchanged).
- [x] **3. AR corner filter** (`ScreenCornerSmoother`): one speed (so one cutoff) for the quad, a 20 px/s noise
      floor so a still camera really gets the minimum cutoff, a 0.15 px hold on the published quad, and an
      unchanged matrix is dropped before the reducers. **Correction to the survey:** it called the old onset
      lag "over 100 px"; measured on a synthetic 30 fps quad it was 12.5 px at 1000 px/s. The real fault was
      the speed estimate leaking its own noise (rest residual 0.37 px for 1 px in; now 0.21), so the change is
      a noise-floor gate, not a retune; lag also halves (6 px onset, 3 px steady). Not done: the 150 px
      restart threshold (a snap to the raw position loses only the few pixels of lag).
- [x] **4. Non-AR fit loop**: `TableSnapPolicy.TablePull` (replaces `pullStep`): the fit pose is averaged
      before the table moves toward it, the pull starts at 1.5 px / 0.3 deg and stops at 0.5 px / 0.1 deg, and
      both shares are taken from the time since the last fit (unchanged at the 300 ms reference). On a
      synthetic 3 px noisy fit: same ~5.9 s to settle, 0.61 px of wander instead of 0.80, steps on 38% of fits
      instead of 75% (each step is a full recompute). **Correction to the survey:** `applyPoseUpdate` (the
      "second controller") lives in the saved-scan edge fallback of the AR frame path, not in the non-AR
      loop the pull runs in, so the two do not fight in one mode; left alone.
- [ ] Also found: in AR, `effectivePitchDegrees` is never refreshed (ball lift uses stale IMU pitch).
- [ ] From the CV survey, not started: temporal tracking of detections (a ball missed for one frame vanishes,
      types flip per frame), cue selection (first CUE-typed ball wins, others dropped), stale `visionData` after
      the camera goes off, `VisionAnalyzer` error handling.
