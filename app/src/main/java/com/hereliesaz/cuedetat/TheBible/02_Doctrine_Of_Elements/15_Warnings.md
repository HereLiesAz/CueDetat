# Warnings

* The `WarningManager` class is responsible for managing the display logic of warning messages.
* Warnings (e.g., "Impossible Shot") are displayed as large, semi-transparent kinetic text at random locations on the screen.
* An `isImpossibleShot` flag in the state, calculated by `UpdateStateUseCase`, triggers the warning.

***
## Addendum: Impossible Shot Detection & Display

* **The One True Principle**: A shot is impossible if the cue ball would have to pass through the object ball to reach the ghost ball position. This is determined by checking if the angle of the `ShotGuideLine` falls outside the 180-degree arc defined by the `TangentLine`. If the shot line is not in the two valid 90-degree quadrants, the shot is physically impossible.

* **Display Timing Mandate**: The visual feedback for an impossible shot must be bifurcated for clarity and responsiveness.
  * **Immediate Feedback**: The `shotLine` and the `ghostCueBall` outline must change to a warning red *immediately* when the `isImpossibleShot` flag in the `OverlayState` becomes `true`. This must be a direct result of the state update, providing instant feedback during a gesture.
  * **Delayed Feedback**: The kinetic text warning (the sarcastic message) must only be displayed *after* the user's gesture has concluded, triggered by the `GestureEnded` event. This prevents the screen from being cluttered with text while the user is actively making an adjustment.