# 1.4. State Management (`UiModel.kt`)

The `CueDetatState` data class is the single, immutable source of truth for the application's UI.
This document provides an overview of its key properties.

### View & Dimensions

- **`viewWidth`, `viewHeight`**: The pixel dimensions of the rendering canvas.
- **`zoomSliderPosition`**: The raw `Float` value from the UI slider, used to calculate the actual
  zoom factor.
### Core Logical Model

- **`protractorUnit`**: A `ProtractorUnit` data class holding the `center`, `radius`, and
  `rotationDegrees` of the aiming tool.
- **`onPlaneBall`**: An optional `OnPlaneBall` representing the user-placed cue ball.
- **`obstacleBalls`**: A `List<OnPlaneBall>` for any additional balls placed on the table.
- **`table`**: A `Table` data class defining the table's `size`, `rotationDegrees`, and `isVisible`
  state.
### UI & Mode Controls

- **`experienceMode`**: A transient, nullable `ExperienceMode` enum. It is **not persisted** to
  disk, which forces the user to select a mode on every fresh application launch.
- **`isBankingMode`**: `Boolean` flag to switch between Protractor and Banking modes.
- **`isSpinControlVisible`**: `Boolean` for the visibility of the spin control UI.
- **`isCameraVisible`**: `Boolean` for the visibility of the camera feed.
- **`orientationLock`**: An `OrientationLock` enum. Its default value is **`PORTRAIT`**.
- **`isBeginnerViewLocked`**: A `Boolean` specific to Beginner Mode, which locks the view to a
  top-down perspective and disables certain interactions.
- Various other `Boolean` flags for toggling dialogs (`show...Dialog`) and UI helpers (
  `areHelpersVisible`).
### Sensor & Perspective Data

- **`currentOrientation`**: A `FullOrientation` data class holding the device's `pitch`, `roll`, and
  `yaw`.
- **`pitchMatrix`, `railPitchMatrix`, etc.**: `Matrix` objects calculated by `UpdateStateUseCase`
  used to project the 2D logical world into the 3D view.
### Derived State (Calculated by `UpdateStateUseCase`)

- **`isGeometricallyImpossible`, `isObstructed`**: Booleans representing warnings for the current
  aiming solution.
- **`aimingLineBankPath`, `tangentLineBankPath`**: `List<PointF>` describing the path of a banked
  shot.
- **`aimedPocketIndex`, `tangentAimedPocketIndex`**: Optional `Int` indicating which pocket is being
  targeted by a shot. The state is comprehensive, and each property has a singular, defined purpose.
  Any additions must be clearly justified to avoid state pollution.