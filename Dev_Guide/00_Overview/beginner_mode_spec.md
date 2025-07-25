# Proposal: Beginner Mode Feature Set

To make the application "absolutely approachable" for new users, this mode should radically simplify the interface and remove all advanced, non-essential features.

### I. Core Configuration & Sub-Modes

Beginner Mode operates in two distinct sub-modes.

#### 1. Protractor Sub-Mode (Initial State)

This is the default state upon entering Beginner Mode.

- **`isBeginnerViewLocked`**: `true`.
- **View**: The view is locked to a perfect top-down perspective (`pitchAngle` = 0). Pan is
  disabled.
- **Controls**: The `TargetBall` and `GhostCueBall` are locked at their default positions. The user
  can only perform a rotational drag to aim.
- **Visuals**: The `ShotGuideLine` is hidden to simplify the view.
- **Zoom**: A special zoom range is active. The default zoom level is the maximum zoom, which places
  the Target and Ghost balls at 85% of the screen width.
- **"Unlock View" Button**: The "Reset View" button is replaced with an "Unlock View" button, which
  transitions the user to the Free Aim sub-mode.

#### 2. Free Aim Sub-Mode (Unlocked State)

This mode is entered after the user taps "Unlock View".

- **`isBeginnerViewLocked`**: `false`.
- **View**: The top-down lock is released, and standard perspective tilt is enabled.
- **Controls**: The `TargetBall` becomes draggable.
- **Visuals**: The `ShotGuideLine` becomes visible.
- **Zoom**: The special Beginner Mode zoom range remains active.
- **"Reset View" Button**: The button reverts to its standard "Reset View" functionality.

### II. General Feature & UI Reductions

The following features **must be removed or hidden** from the UI in both sub-modes of Beginner Mode:

- **Banking Mode**: The "Calculate Bank" menu option must be removed.
- **Spin Control**: The "Spin" button and its associated UI must be removed.
- **Obstacle Balls**: The "Add Ball" button must be removed.
- **Advanced Options**: The entire "Too Advanced Options" menu item and its associated dialogs must
  be removed.
- **World Rotation**: The horizontal table rotation slider must be removed.
- **Table Controls**: All controls for showing/hiding the table or cue ball must be removed.