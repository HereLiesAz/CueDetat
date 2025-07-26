# 2.1. User Experience Modes

The application operates in three distinct user experience modes, selected on launch.

## I. Expert Mode

Expert Mode provides the complete, unfiltered feature set of the application. It is designed for
users who are comfortable with all the tools and want no restrictions.

* **Core Mandates:**
  * The **Table** is **always visible**.
  * The **Actual Cue Ball** is **always visible**.
* **Available Features:** All application features are enabled, including:
  * Protractor and Banking Sub-Modes for aiming.
  * Full Spin Control.
  * Placement of Obstacle Balls.
  * Access to "Too Advanced Options" for computer vision tuning and calibration.
  * The bottom-right action button is always "Reset View".

### Protractor Sub-Mode (Expert)

* **Functionality:** The standard aiming mode for cut shots. The user can drag the `TargetBall` and
  `ActualCueBall` to match the real balls on the table. A rotational drag gesture aims the shot.

### Banking Sub-Mode (Expert)

* **Functionality:** Allows the user to calculate multi-rail kick and bank shots by dragging the
  `ActualCueBall` and an aiming target.

## II. Beginner Mode

A simplified mode with two distinct sub-modes to guide new users.

### 1. Protractor Sub-Mode (Locked State)

This is the default state upon entering Beginner Mode.

- **`isBeginnerViewLocked`**: `true`.
- **View**: The view is rendered with a flat, top-down perspective. The 3D perspective tilt is
  disabled.
- **Bubble Level**: The 3D "ghost" component of the balls is repurposed as a bubble level. It moves
  on-screen in the opposite direction of the device's **pitch** and **roll** to guide the user to a
  perfectly flat orientation. This effect is constrained, preventing the 3D component's center from
  moving beyond its own radius from the 2D component's center. The tilt effect is capped at +/- 20
  degrees.
- **Gestures**: All drag, pan, and multi-touch gestures are disabled. The protractor unit is
  immobile.
- **Visuals**: The `ShotGuideLine`, distance display, and all warning popups are hidden. The tangent
  line is rendered as two solid lines.
- **Zoom**: A special, expanded zoom range is active to make the protractor large and clear.
- **Button**: The bottom-right action button displays "Unlock View".

### 2. Free Aim Sub-Mode (Unlocked State)

This mode is entered after the user taps "Unlock View".

- **`isBeginnerViewLocked`**: `false`.
- **View**: The standard 3D perspective tilt (based on pitch only) is enabled.
- **Controls**: The `TargetBall` becomes draggable, and rotational aiming is enabled.
- **Visuals**: The `ShotGuideLine` becomes visible. The tangent line reverts to its standard
  half-solid, half-dotted appearance. Warnings and the distance display are active.
- **Zoom**: The zoom range reverts to the standard range used in Expert Mode.
- **Button**: The bottom-right action button displays "Lock View", which returns to the locked
  state.

### Disabled Features (All Beginner Sub-Modes)

- Banking Mode
- Spin Control
- Obstacle Balls
- Advanced Options
- World Rotation
- The following menu items are hidden: Camera Toggle, Table Size, and Distance Unit Toggle.

## III. Hater Mode

Hater Mode transforms the application into a "Magic 8-Ball" that delivers cynical and unhelpful "
advice."

* **Core Concept**: An interactive, darkly humorous oracle that responds to a device shake.
* **User Interface**:
  * The screen renders a large, murky blue triangle (the "die") that floats in a dark void.
  * The triangle drifts and rotates based on the device's gyroscope. It can be "pushed" by the
    user's touch.
  * The main menu is accessible but limited to mode selection and informational links.
* **Interaction**:
  * A physical shake of the device triggers a new response.
  * The die animates out, then a new one animates in with a new response.
* **Content**: The responses are drawn from a predefined set of cynical, witty, and non-committal
  phrases that are consistent with the application's established persona.