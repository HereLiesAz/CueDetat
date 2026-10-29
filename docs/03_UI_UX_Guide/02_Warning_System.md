# 3.2. Warning System

The `WarningManager` class manages the display of user-facing warnings.

* **Display:** Warnings (e.g., "Impossible Shot") are displayed as large, semi-transparent kinetic
  text at random locations on the screen.
* **Trigger:** The `isGeometricallyImpossible` flag in the `CueDetatState`, calculated by
  `UpdateStateUseCase`, is the primary trigger.

## Impossible Shot Detection & Display

* **Detection Principle**: A shot is impossible if the path to the ghost ball is obstructed by the
  target ball itself. This is determined by comparing the distance from the `shotLineAnchor` to the
  `GhostCueBall` against the distance from the anchor to the `TargetBall`. If
  `distance(anchor, ghost) > distance(anchor, target)`, the shot is impossible.

* **Display Timing**:
* **Immediate Feedback**: The `shotLine` and `ghostCueBall` outlines must change to a warning color
  *immediately* when a warning flag in the `CueDetatState` becomes `true`.
* **Kinetic text**: the insult appears when the shot *first becomes* impossible, obstructed or too
  steeply tilted (`MainViewModel.processAndEmitState`), led by a short plain statement of which of
  the three it is ("No such shot.", "Something is in the way.", "Too steep a tilt."). It is
  dismissed when the next gesture starts and otherwise fades after three seconds
  (`WarningManager`).
* **Notices**: AR and cloud notices, lock failures and CV hints are written into `warningText` by
  reducers and fade after five seconds (`MainViewModel.fadeReducerNotice`). A *persistent* notice
  (`ArNotice(persistent = true)`: tracking lost, the camera or ARCore failing to start) stays
  until its resolution clears it.