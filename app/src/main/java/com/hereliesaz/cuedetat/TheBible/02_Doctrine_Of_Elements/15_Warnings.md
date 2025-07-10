# Warnings

* The `WarningManager` class is responsible for managing the display logic of warning messages.
* Warnings (e.g., "Impossible Shot") are displayed as large, semi-transparent kinetic text at random locations on the screen.
* An `isImpossibleShot` flag in the state, calculated by `UpdateStateUseCase`, triggers the warning.

***
## Addendum: Impossible Shot Detection & Display

* **The One True Principle**: A shot is impossible if the path to the ghost ball is obstructed by the target ball itself. This is determined by comparing the distance from the shot's origin (`shotLineAnchor`) to the `GhostCueBall` against the distance from the origin to the `TargetBall`. If `distance(origin, ghost) > distance(origin, target)`, the shot is physically impossible.

* **Display Timing Mandate**: The visual feedback for an impossible shot must be bifurcated for clarity and responsiveness.
  * **Immediate Feedback**: The `shotLine` and the `ghostCueBall` outline must change to a warning red *immediately* when the `isImpossibleShot` flag in the `OverlayState` becomes `true`. This must be a direct result of the state update, providing instant feedback during a gesture.
  * **Delayed Feedback**: The kinetic text warning (the sarcastic message) must only be displayed *after* the user's gesture has concluded, triggered by the `GestureEnded` event. This prevents the screen from being cluttered with text while the user is actively making an adjustment.