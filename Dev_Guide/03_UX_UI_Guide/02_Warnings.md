# 3.2. Warning System

The `WarningManager` class manages the display of user-facing warnings.

* **Display:** Warnings (e.g., "Impossible Shot") are displayed as large, semi-transparent kinetic
  text at random locations on the screen.
* **Trigger:** The `isGeometricallyImpossible` flag in the `OverlayState`, calculated by
  `UpdateStateUseCase`, is the primary trigger.

## Impossible Shot Detection & Display

* **Detection Principle**: A shot is impossible if the path to the ghost ball is obstructed by the
  target ball itself. This is determined by comparing the distance from the `shotLineAnchor` to the
  `GhostCueBall` against the distance from the anchor to the `TargetBall`. If
  `distance(anchor, ghost) > distance(anchor, target)`, the shot is impossible.

* **Display Timing**:
* **Immediate Feedback**: The `shotLine` and `ghostCueBall` outlines must change to a warning color
  *immediately* when a warning flag in the `OverlayState` becomes `true`.
* **Delayed Feedback**: The kinetic text warning message must only be displayed *after* the user's
  gesture has concluded, triggered by the `GestureEnded` event.