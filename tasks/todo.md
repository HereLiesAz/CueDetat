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
- [ ] **2. Core search**
  - [ ] Banks and kicks: unfold off the rails; use `Cushion.rebound` for the rail's effect; handle pocket jaws
        (`firstCushionHit` returns null there).
  - [ ] Legal 2-ball combinations: `AimSolver` twice (cue -> A, then A's departure -> B -> pocket), `GroupRules`
        for legality, obstruction on every leg.
  - [ ] Candidate type with ball ids, pocket index, leg points; a probability model for banks, kicks, combos
        (the existing one is direct-only and uncalibrated).
  - [ ] Hand-worked test layouts for each shot type.
- [ ] **3. Wiring**
  - [ ] State -> core input: detected balls plus hand-placed ones (all "mine"), pockets (scanned ones synthesised),
        table spec; extend the driver key (table size, placed balls, scan pockets), not just detections.
  - [ ] Map the core result onto `RecommendedShot` so the HUD and renderer keep working.
  - [ ] Re-enable the rail toggle and stop forcing `isAdvisorEnabled = false` at load.
  - [ ] Decide the future of `domain/advisor/ShotAdvisor` (delete or keep behind the toggle).
- [ ] **4. Auto-aim**
  - [ ] New routed event (`StateReducerRoutingTest` enforces routing): set target, aim, anchors; pause on manual
        drag of the target or aim; a re-arm control.
  - [ ] Advisor rejects shots whose ghost would sit off the table (so `BallBounds.confine` never has to move the
        chosen aim). A combo targets ball A; the A -> B -> pocket leg is drawn from the recommendation.
  - [ ] Review `isTiltBeyondLimit` (assumes up-screen shots).
- [ ] **5. Device validation.** Cannot be done in the authoring environment; CI proves compile and unit tests only.
      Aim quality, probabilities (uncalibrated), and performance on a real table are open until then.
- [ ] **Later:** cue-leave and safeties on core once it has a post-contact cue path; massé ghost off-table.

## Risks

- Nothing here proves the picks are good. Probabilities are placeholders (aiming sigma 0.6 degrees is not fitted).
- Recompute cost grows with targets x pockets x all balls (combos) x rails (banks); not measured.
- `CueDetatState.comparableFields()` is hand-maintained over ~120 fields (ARCHITECTURE invariant, unenforced):
  every new state field must be added to it.
