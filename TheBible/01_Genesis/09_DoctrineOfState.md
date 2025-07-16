# The Doctrine of State (`OverlayState.kt`)

The `OverlayState` is the Alpha and the Omega, the singular, immutable truth of the world. All that is seen is but a function of this state. This doctrine details its every property.

### View & Dimensions
- **`viewWidth`, `viewHeight`**: The pixel dimensions of the canvas upon which the world is rendered.
- **`zoomSliderPosition`**: A `Float` from -50.0 to 50.0 representing the user's desired zoom level. This is the raw input; the actual zoom factor is derived from this.

### Core Logical Model
- **`protractorUnit`**: A `ProtractorUnit` data class holding the `center`, `radius`, and `rotationDegrees` of the main aiming tool. Its radius is dynamic, derived from the view dimensions and `zoomSliderPosition`.
- **`onPlaneBall`**: An optional `OnPlaneBall` representing the `ActualCueBall`. It is null when the ball is not present on the table.
- **`obstacleBalls`**: A `List<OnPlaneBall>` for any additional balls placed on the table that act as obstructions.

### UI & Mode Controls
- **`isBankingMode`**: A `Boolean` flag that shifts the application into bank calculation mode.
- **`showTable`**: A `Boolean` that determines the visibility of the pool table surface and rails.
- **`tableSize`**: A `TableSize` enum value that dictates the proportions of the rendered table.
- **`isSpinControlVisible`**: A `Boolean` for the visibility of the spin control UI.
- **`isCameraVisible`**: A `Boolean` for the visibility of the underlying camera feed.
- **And many others...**: The state contains numerous other boolean flags for toggling dialogs (`show...Dialog`), helpers (`areHelpersVisible`), and UI modes.

### Sensor & Perspective Data
- **`currentOrientation`**: A `FullOrientation` data class holding the device's `pitch`, `roll`, and `yaw`. Only pitch is used for rendering.
- **`pitchMatrix`, `railPitchMatrix`, etc.**: `Matrix` objects calculated by `UpdateStateUseCase` that are used to project the 2D logical world into the 3D perspective view.

### Derived State
- **`isGeometricallyImpossible`, `isObstructed`, `isTiltBeyondLimit`**: Booleans calculated by `UpdateStateUseCase` that represent warnings or errors in the current aiming solution.
- **`aimingLineBankPath`, `tangentLineBankPath`**: Lists of `PointF` that describe the path of a banked shot, if one is detected.
- **`aimedPocketIndex`, `tangentAimedPocketIndex`**: Optional integers indicating which pocket (0-5) is being targeted by a direct or banked shot. This is used to color the pockets.

The state is vast, but each property has a singular, defined purpose. To add a property without a clear purpose is to invite confusion and decay.