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
- **Gestures**: All pan, drag, and world-rotation gestures are disabled. The protractor unit is
  immobile. Zoom is controlled exclusively via the on-screen zoom slider.
- **Visuals**: The `ShotGuideLine`, distance display, and all warning popups are hidden. The tangent
  line is rendered as two solid lines.
- **Zoom**: A special, expanded zoom range is active to make the protractor large and clear.
- **Button**: The bottom-right action button displays "Unlock View".

### 2. Free Aim Sub-Mode (Unlocked State)

This mode is entered after the user taps "Unlock View".

- **`isBeginnerViewLocked`**: `false`.
- **View**: The standard 3D perspective tilt (based on pitch only) is enabled.
- **Controls**: The `TargetBall` becomes draggable, and rotational aiming is enabled.
- **Gestures**: Single-finger pan and two-finger pan are suppressed. Two-finger rotation
  (`TableRotationApplied`) and pinch-to-zoom remain active.
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

## IV. AR Camera Flow

The AR camera system follows a strict state machine. The mandatory rule is: **one user interaction = felt color capture only**. The pocket scan is a separate, user-initiated action.

### Camera Mode State Machine

- **`OFF`** → `AR_SETUP`: `CycleCameraMode` always transitions directly from `OFF` to `AR_SETUP`. There is no first-launch `CAMERA_ONLY` intermediate path.
- **`AR_SETUP`** → `AR_ACTIVE`: Auto-advances when `tableOverlayConfidence >= 0.8` and all prerequisites are met.
- **`AR_ACTIVE`** / **`AR_SETUP`** → `OFF`: `TurnCameraOff`.
- **`AR_ACTIVE`** → `AR_SETUP`: `ArTrackingLost` (clears scan model and returns to setup).
- `ToggleTableScanScreen` does **not** change `cameraMode`; it only shows/hides the felt-scan overlay.

### AR_SETUP Behavior

- The **Felt** and **Cancel** nav rail items appear immediately when `cameraMode == AR_SETUP` or `cameraMode == AR_ACTIVE` — not only after the scan completes.
- The **Felt** button dispatches only `ToggleTableScanScreen`. It no longer dispatches `ClearTableScan` first.
- The **AR toggle** in the nav rail uses `toggleOnText = "AR"` and `toggleOffText = "off"`.

### TableScanScreen

- `TableScanScreen` is rendered as an **inline overlay** composable inside `ProtractorScreen`, controlled by `uiState.showTableScanScreen`. It is not a navigated route; `ROUTE_SCAN` has been removed from the `NavHost` entirely.
- The camera analyzer routes to `TableScanAnalyzer` based on `uiState.showTableScanScreen` (not by checking the current navigation route).
- `TableScanScreen` does not request GPS permission and does not show a Cancel button. It includes `LaunchedEffect(Unit) { viewModel.resetScan() }` to reset scan state on entry.
- Pocket-detection auto-complete has been removed from `TableScanViewModel.onFrame()`. Only an explicit call to `captureFeltAndComplete()` can complete the scan.

## III. Hater Mode

Hater Mode transforms the application into a "Magic 8-Ball" that delivers cynical and unhelpful "
advice."

* **Core Concept**: An interactive, darkly humorous oracle that responds to a device shake.
* **Physics Engine**: Google's **LiquidFun** library.
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