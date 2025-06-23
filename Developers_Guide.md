Project Development Guide: Cue D'état
This document outlines the core architecture, concepts, and future direction of the Cue D'état
application. It serves as a single source of truth to prevent regressions and ensure consistent
development. Consider it a note-to-self for the AI working on this project, and keep it updated
accordingly with ANYTHING that will be useful to the next AI in the next chat.

NEVER change what is written here, only add to it. Always include anything that you note to yourself as a matter of clarification.

## 1. Core Concepts & Official Terminology
A precise vocabulary is critical. The following terms are to be used exclusively.

* **Logical Plane:** An abstract, infinite 2D coordinate system (like graph paper) where all aiming
  geometry is defined and calculated. This is the "world" of the simulation. The origin (0,0) of
  this plane is conceptually at the top-left, but pivot points for transformations are usually
  the center of the view (`viewWidth/2, viewHeight/2` in logical units).
* **Screen Plane:** The physical 2D plane of the device's screen. This is the "window" through
  which the user views the Logical Plane.
* **Perspective Transformation:** The process, primarily handled by a `pitchMatrix` (and
  `railPitchMatrix` for lifted elements), of projecting the Logical Plane onto the Screen Plane
  to create the 3D illusion. Crucially, this transformation must always pivot around the absolute
  center of the view (logical coordinates `viewWidth/2, viewHeight/2` map to screen coordinates
  `viewWidth/2, viewHeight/2` as the pivot).
* **Global Zoom:** A single zoom factor, controlled by `zoomSliderPosition` and `ZoomMapping.kt`,
  that determines the base logical radius for primary interactive elements like the
  `ProtractorUnit.radius` and `ActualCueBall.radius`.
* **Full Orientation:** Represents the phone's orientation in 3D space, comprising yaw, pitch, and
  roll angles, typically obtained from `Sensor.TYPE_ROTATION_VECTOR`. In `OverlayState`,
  `currentOrientation.pitch` is stored as `-sensorPitch` for consistency with earlier implementations.
* **Spatial Lock (`isSpatiallyLocked`):** A mode where:
    * User touch input for moving or rotating logical elements (`ProtractorUnit`, `ActualCueBall`,
      `tableRotationDegrees`, `bankingAimTarget`) is disabled.
    * The visual rendering of the Logical Plane attempts to keep elements appearing fixed in
      real-world space relative to their orientation at the moment "Lock" was engaged.
    * **Unlocked View:** Primarily responds to `currentOrientation.pitch` (forward/backward phone
      tilt). Phone roll and yaw have minimal/no direct impact on the rendered plane's orientation.
    * **Lock Engagement:** When "Lock" is pressed, the phone's `currentOrientation` is stored as
      `anchorOrientation`. The visual perspective *does not change at this instant*.
    * **Locked View (Phone Moving):** The `pitchMatrix` is calculated such that the camera
      counter-rotates against the phone's movement (`currentOrientation - anchorOrientation`) in
      all three axes (pitch, roll, yaw). This is achieved by:
        1. Setting the camera's base rotation to match the `anchorOrientation.pitch` (to align
           with the unlocked view's primary axis at the moment of lock).
        2. Calculating deltas: `deltaRoll = currentOrientation.roll - anchorOrientation.roll`,
           `deltaYaw = currentOrientation.yaw - anchorOrientation.yaw`, and
           `deltaPitch_additional = currentOrientation.pitch - anchorOrientation.pitch`.
        3. Applying counter-rotations to the camera for these deltas (e.g., `camera.rotateZ(-deltaRoll)`).
* **On-Screen Elements:**

    * **Protractor Unit (Protractor Mode Only):** The primary aiming apparatus for cut shots.
        * **Target Ball (Protractor):** The logical and visual center of the `ProtractorUnit`. Its
          logical position can be user-moved (if not spatially locked). Its logical radius is set by
          Global Zoom (`state.protractorUnit.radius`).
        * **Ghost Cue Ball (Protractor):** The second ball in the `ProtractorUnit`. Its *absolute
          logical position* (`state.protractorUnit.protractorCueBallCenter`) is calculated by the
          `ProtractorUnit` class based on the Target Ball's center, the unit's radius, and
          `state.protractorUnit.rotationDegrees`. Its logical radius is the same as the Target
          Ball's.
          Both Target and Ghost Cue Ball have a 2D logical representation (drawn by
          `BallRenderer.drawLogicalBalls`) and a 3D screen-space "ghost" effect (drawn by
          `BallRenderer.drawScreenSpaceBalls` with lift).
    * **ActualCueBall:** A user-draggable logical ball (if not spatially locked).
        * **In Protractor Mode (Optional):** Can be toggled by the user. Used for visualizing shots
          originating from a specific point. Rendered with a "lifted" 3D ghost effect.
          Its logical radius (`state.actualCueBall.radius`) is set by Global Zoom.
        * **In Banking Mode (Mandatory, becomes the "Banking Ball"):** Always visible and represents
          the
          cue ball on the table. Its logical radius is set by Global Zoom. Rendered *on* the table
          plane.
    * **Table Visuals (Banking Mode Only):** Wireframe representation of the pool table surface,
      rails,
      pockets, and diamonds.
        * Logically anchored at the view's center (`viewWidth/2, viewHeight/2`).
        * Its logical scale is determined by `tableToBallRatio * ActualCueBall.radius`.
        * The table surface is rendered on the `pitchMatrix`. Rails are rendered on the
          `railPitchMatrix`.
        * Can be rotated by the user via `tableRotationDegrees` (if not spatially locked).
    * **Lines:** (Behavior when spatially locked needs to ensure they respect the fixed logical
      elements)
        * **Protractor Shot Line (Protractor Mode):** From `ActualCueBall.center` (if visible, else
          a default
          screen anchor's logical projection) through the `ProtractorUnit.GhostCueBall`'s absolute
          logical center. Drawn in absolute logical coordinates.
        * **Aiming Line (Protractor Mode):** From `ProtractorUnit.GhostCueBall`'s local position
          through `ProtractorUnit.TargetBall`'s local position. Drawn relative to the transformed
          ProtractorUnit canvas.
        * **Tangent Lines & Angle Lines (Protractor Mode):** Relative to the transformed
          ProtractorUnit canvas.
        * **Banking Shot Line (Banking Mode):** From `ActualCueBall.center` towards
          `bankingAimTarget`, with reflections off logical table boundaries. Drawn in absolute
          logical coordinates.

## 2. Architectural Model & File Structure

The architecture strictly separates data, logic, and presentation.
com/hereliesaz/cuedetat/
├── data/
│ ├── SensorRepository.kt // Provides FullOrientation (yaw, pitch, roll).
│ └── ... (GithubRepository, UpdateChecker)
├── view/
│ ├── model/
│ │ ├── LogicalPlane.kt // Defines ProtractorUnit, ActualCueBall.
│ │ └── Perspective.kt // Manages 3D transformation. Crucial for Spatial Lock.
│ ├── renderer/
│ │ ├── util/
│ │ │ └── DrawingUtils.kt
│ │ ├── BallRenderer.kt
│ │ ├── LineRenderer.kt
│ │ ├── TableRenderer.kt
│ │ ├── RailRenderer.kt
│ │ └── OverlayRenderer.kt
│ ├── state/
│ │ ├── OverlayState.kt // Holds `currentOrientation`, `anchorOrientation`, `isSpatiallyLocked`.
│ │ └── ScreenState.kt
│ └── ProtractorOverlayView.kt // Handles touch input; respects `isSpatiallyLocked`.
├── domain/
│ ├── StateReducer.kt // Manages `isSpatiallyLocked`, sets `anchorOrientation`. Ignores placement
// events when locked.
│ └── UpdateStateUseCase.kt // Passes orientations and lock state to `Perspective.kt`.
└── ui/
├── composables/
├── MainViewModel.kt // Receives `FullOrientationChanged`, orchestrates state.
├── MainScreen.kt // Contains "Lock" button, passes state to `ProtractorOverlayView`.
└── MainScreenEvent.kt // Includes `ToggleSpatialLock`, `FullOrientationChanged`.

**The Golden Rule**: ViewModel orchestrates. StateReducer computes state. UpdateStateUseCase
computes derived state. Renderers display.

## 3. Rendering Pipeline (Conceptual) & Spatial Lock Integration

1.  **Sensor Input (`SensorRepository`)**: Provides continuous `FullOrientation` (yaw, pitch, roll)
    data.
2.  **ViewModel (`MainViewModel`)**: Receives `FullOrientationChanged` events and passes them to the
    `StateReducer` via `updateContinuousState`.
3.  **StateReducer (`StateReducer`)**:
    *   Updates `OverlayState.currentOrientation` with the latest sensor data.
    *   Handles `ToggleSpatialLock`:
        *   If locking: sets `isSpatiallyLocked = true` and
            `anchorOrientation = currentState.currentOrientation`.
        *   If unlocking: sets `isSpatiallyLocked = false` and `anchorOrientation = null`.
    *   If `isSpatiallyLocked == true`, it ignores most `MainScreenEvent` types that attempt to
        change the logical position or rotation of elements (e.g., `RotationChanged`,
        `UpdateLogicalUnitPosition`, etc.). Events like zoom, mode changes, and sensor updates are
        still processed.
4.  **UpdateStateUseCase (`UpdateStateUseCase`)**:
    *   Receives the `OverlayState` (containing `currentOrientation`, `anchorOrientation`,
        `isSpatiallyLocked`).
    *   Calls `Perspective.createPitchMatrix`, passing all three: `currentOrientation`,
        `anchorOrientation`, and `isSpatiallyLocked`.
5.  **Perspective Transformation (`Perspective.createPitchMatrix`)**:
    *   **If Unlocked (`isSpatiallyLocked == false` or `anchorOrientation == null`):**
        *   The camera is rotated *only* based on `currentOrientation.pitch` (forward/backward
            tilt). Roll and yaw from the phone do not directly influence the main plane's rendering
            matrix.
    *   **If Locked (`isSpatiallyLocked == true` and `anchorOrientation != null`):**
        1.  The camera's base orientation is set using `camera.rotateX(anchorOrientation.pitch)`.
            This ensures the initial locked view matches the primary tilt of the unlocked view,
            preventing a visual jump in pitch when the lock is engaged.
        2.  Deltas are calculated: `deltaRoll = currentOrientation.roll - anchorOrientation.roll`,
            `deltaYaw = currentOrientation.yaw - anchorOrientation.yaw`, and
            `deltaPitch_additional = currentOrientation.pitch - anchorOrientation.pitch`.
        3.  These deltas are then applied as *counter-rotations* to the camera (which is already
            pitched by `anchorOrientation.pitch`). For example, `camera.rotateZ(-deltaRoll)`
            counteracts phone roll. The goal is to make the logical elements appear fixed in world
            space relative to their configuration at the moment of lock. The exact signs and order
            of applying delta rotations (e.g., `Z, X, Y` or `Z, Y, X` for the deltas) are critical
            and may require empirical tuning for intuitive feel, especially yaw.
6.  **ViewModel (Continued)**: Emits the fully processed `OverlayState` (with matrices) to the UI.
7.  **Renderer (`ProtractorOverlayView` via `OverlayRenderer`):**
    *   Uses the `pitchMatrix` (and `railPitchMatrix`) from `OverlayState` to draw all logical
        elements. Since their logical coordinates are fixed (when locked) and the matrix now reflects
        the counter-rotated camera view, the elements should appear spatially anchored.
    *   Touch input for moving/rotating elements is disabled by `ProtractorOverlayView` if
        `canonicalState.isSpatiallyLocked` is true (except for scaling).

## 4. Core Operational Modes & Entity Behavior

(Protractor Mode, Banking Mode, Global Zoom and Radii sections remain largely the same but should be understood in the context that user manipulation of element positions/rotations is disabled when `isSpatiallyLocked` is true.)

## 5. Key Implementation Learnings & Mandates (Chronological where possible)

(Existing points A-N remain relevant.)

* **O. Spatial Lock Implementation Details:**
    *   `SensorRepository` now provides `FullOrientation` (yaw, pitch, roll).
    *   `OverlayState` stores `currentOrientation` and `anchorOrientation` (when locked).
    *   `StateReducer`:
        *   On `ToggleSpatialLock` to true: `isSpatiallyLocked = true`,
            `anchorOrientation = currentOrientation`.
        *   On `ToggleSpatialLock` to false: `isSpatiallyLocked = false`, `anchorOrientation = null`.
        *   When locked, ignores events that would change logical positions/rotations of elements.
    *   `ProtractorOverlayView`: Disables touch manipulations (except zoom) when locked.
    *   `Perspective.createPitchMatrix`:
        *   Unlocked: Uses `currentOrientation.pitch` only.
        *   Locked: Establishes view with `anchorOrientation.pitch`. Then applies counter-rotations
            based on `currentOrientation - anchorOrientation` for pitch, roll, and yaw to achieve
            world-space stability. Fine-tuning of rotation order and signs (especially for yaw) is
            critical for intuitive feel.
* **P. Avoiding Visual "Jump" on Lock:** The strategy for "Lock" mode in `Perspective.kt` aims to
  prevent a jarring visual change when the lock is engaged by ensuring the initial locked camera
  perspective (based on `anchorOrientation.pitch`) aligns with the unlocked view's primary
  rotational axis. Full 3D stabilization (roll, yaw) then activates relative to this initial
  locked view.

## 6. Future Development Plan

* **Bank/Kick Shot Calculator (Refinement):**
    * Improve reflection logic in `LineRenderer` for more than 2 banks.
    * Consider pocket geometry for line termination/success.
* **Object/Table Detection (Computer Vision):**
    * Use OpenCV or ML Kit to detect table boundaries and ball positions.
    * Project screen coordinates to Logical Plane to auto-place `ActualCueBall`.
* **"English" / Spin Visualization:** Add UI controls to simulate sidespin, altering tangent lines
  or shot paths.
* **Tutorial Enhancements:** Make the tutorial more interactive, highlighting UI elements
  corresponding to the current step.
* **Spatial Lock Refinement:**
    *   Empirically tune the signs and order of applying delta rotations (pitch, roll, yaw) in
        `Perspective.kt` for the most intuitive "world lock" feel.
    *   Consider using Quaternions instead of Euler angles in `Perspective.kt` for more robust
        3D rotations, avoiding gimbal lock and simplifying combined rotations. This is a significant
        refactor.
    *   Investigate if ARCore or similar AR SDKs could provide more stable world tracking if sensor-only
        data proves too drifty for reliable spatial anchoring over time.



