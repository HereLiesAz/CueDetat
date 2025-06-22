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
* **On-Screen Elements:**

    * **Protractor Unit (Protractor Mode Only):** The primary aiming apparatus for cut shots.
        * **Target Ball (Protractor):** The logical and visual center of the `ProtractorUnit`. Its
          logical position is fixed at the view's center. Its logical radius is set by Global Zoom.
        * **Ghost Cue Ball (Protractor):** The second ball in the `ProtractorUnit`. Its logical
          position is derived from the Target Ball's position plus the user-controlled rotation
          angle. Its logical radius is the same as the Target Ball's.
          Both are rendered with a "lifted" 3D ghost effect.
    * **ActualCueBall:** A user-draggable logical ball.
        * **In Protractor Mode (Optional):** Can be toggled by the user. Used for visualizing shots
          originating from a specific point (e.g., jump shots). Rendered with a "lifted" 3D ghost
          effect.
          Its logical radius is set by Global Zoom.
        * **In Banking Mode (Mandatory, becomes the "Banking Ball"):** Always visible and represents
          the
          cue ball on the table. Its logical radius is set by Global Zoom. Rendered *on* the table
          plane (no lift for its Y-position, visual size reflects perspective on the plane).
    * **Table Visuals (Banking Mode Only):** Wireframe representation of the pool table surface,
      rails,
      pockets, and diamonds.
        * Logically anchored at the view's center (`viewWidth/2, viewHeight/2`).
        * Its logical scale is determined by `tableToBallRatio * ActualCueBall.radius` (where
          `ActualCueBall.radius` is the current Global Zoom-scaled radius).
        * The table surface is rendered on the `pitchMatrix`. Rails are rendered on the
          `railPitchMatrix`
          (which includes calculated lift).
        * Can be rotated by the user via `tableRotationDegrees`.
    * **Lines:**
        * **Protractor Shot Line (Protractor Mode):** From `ActualCueBall` (if visible, else a
          default
          screen anchor) through the `ProtractorUnit.GhostCueBall`.
        * **Aiming Line (Protractor Mode):** From `ProtractorUnit.GhostCueBall` through
          `ProtractorUnit.TargetBall`.
        * **Tangent Lines & Angle Lines (Protractor Mode):** Relative to the `ProtractorUnit`.
        * **Banking Shot Line (Banking Mode):** From `ActualCueBall.center` (the "Banking Ball")
          towards
          `bankingAimTarget`, with reflections off logical table boundaries.

## 2. Architectural Model & File Structure

The architecture strictly separates data, logic, and presentation.

com/hereliesaz/cuedetat/
├── view/
│ ├── model/
│ │ ├── LogicalPlane.kt // Defines the abstract geometry (ProtractorUnit, ActualCueBall).
│ │ └── Perspective.kt // Manages the 3D transformation logic.
│ ├── renderer/
│ │ ├── util/
│ │ │ └── DrawingUtils.kt // Shared, static helper functions for drawing math.
│ │ ├── BallRenderer.kt // Draws all ball and ghost ball elements.
│ │ ├── LineRenderer.kt // Draws all line and label elements.
│ │ ├── TableRenderer.kt // Draws table surface and pockets (banking mode).
│ │ ├── RailRenderer.kt // Draws table rails and diamonds (banking mode).
│ │ └── OverlayRenderer.kt // The coordinator. Initializes and calls other renderers.
│ └── state/
│ ├── OverlayState.kt // An immutable snapshot of the entire scene's state.
│ └── ScreenState.kt // Defines UI-level states like Toast messages.
├── domain/
│ ├── StateReducer.kt // Pure function to handle state changes from events.
│ └── UpdateStateUseCase.kt // Pure function for complex, derived state calculations (e.g.,
matrices).
└── ui/
├── composables/ // Small, reusable UI components.
├── MainViewModel.kt // The coordinator of state and events.
├── MainScreen.kt // The main Composable screen, assembles components.
└── MainScreenEvent.kt // Defines all possible user interactions.

**The Golden Rule**: The `ViewModel` handles event dispatch and conversion (e.g., screen to logical
coordinates). The `StateReducer` is the only component that can create or modify the `OverlayState`
based on processed events. The `View` and `Renderer` components are "dumb" components that only
receive state and display it. `UpdateStateUseCase` calculates derived state like matrices.

## 3. Rendering Pipeline (Conceptual)

To avoid rendering artifacts, the following order of operations is conceptually followed:

1. **ViewModel**: Orchestrates event handling. For position-based events, converts screen
   coordinates to logical coordinates and dispatches new internal events to the `StateReducer`.
2. **StateReducer**: Receives events (including internal logical position events) and computes a new
   `OverlayState`. This includes updating logical positions and radii based on Global Zoom.
3. **UpdateStateUseCase**: Takes the new `OverlayState` and calculates derived properties like
   `pitchMatrix`, `railPitchMatrix`, `inversePitchMatrix`, and `isImpossibleShot`. Returns the
   updated `OverlayState`.
4. **ViewModel**: Emits the final `OverlayState` to the UI.
5. **Renderer (`ProtractorOverlayView` via `OverlayRenderer`):**
    * Receives the final `OverlayState`.
    * **Banking Mode Specifics:**
        * `canvas.concat(state.pitchMatrix)`: Applies perspective for table surface and Banking
          Ball.
        * `TableRenderer.draw()`: Draws table surface and pockets.
        * `LineRenderer.drawLogicalLines()`: Draws Banking Shot Line (with reflections).
        * `BallRenderer.drawLogicalBalls()`: Draws logical representation of Banking Ball.
        * `canvas.restore()`
        * `canvas.save()`
        * `canvas.concat(state.railPitchMatrix)`: Applies perspective for lifted rails.
        * `RailRenderer.draw()`: Draws rails and diamonds.
        * `canvas.restore()`
    * **Protractor Mode Specifics:**
        * `canvas.concat(state.pitchMatrix)`: Applies perspective for all logical elements.
        * `LineRenderer.drawLogicalLines()`: Draws Protractor Shot Line, Tangent Lines, Protractor
          Aiming and Angle Lines.
        * `BallRenderer.drawLogicalBalls()`: Draws logical representations of `ProtractorUnit` and
          optional `ActualCueBall`.
        * `canvas.restore()`
    * **Screen Space Elements (for both modes, if applicable):**
        * `BallRenderer.drawScreenSpaceBalls()`: Draws the 3D "ghost" effect for `ActualCueBall` (in
          banking mode, this is the primary visual of the ball on the table; in protractor mode, for
          the optional `ActualCueBall` and `ProtractorUnit` components). This method internally uses
          `state.pitchMatrix` to project logical centers and determine visual radii appropriately
          for each mode.

## 4. Key Implementation Learnings & Mandates

This section captures critical design decisions and implementation details that have been clarified
through development iterations. Adherence to these points is mandatory to prevent regressions.

* **A. Operational Modes: Protractor vs. Banking**
  The application has two mutually exclusive primary modes:
    * Protractor Mode: The default mode. `ProtractorUnit` (Target Ball, Ghost Cue Ball), its
      associated lines (Aiming, Tangent, Angle), and an optional user-draggable `ActualCueBall` are
      active.
    * Banking Mode: Activated by the menu. The `ProtractorUnit` is hidden. A wireframe pool table (
      surface, rails, pockets, diamonds) is displayed, logically anchored at the view center. The
      `ActualCueBall` becomes the "Banking Ball," which is always visible and user-draggable on the
      table plane. The table can be rotated by the user.
* **B. State Transition for Banking Mode**
  Entering/Exiting Banking Mode is handled by `StateReducer.ToggleBankingMode`:
    * **Entering:** `isBankingMode = true`. `ActualCueBall` ("Banking Ball") is created/reset to the
      logical view center. A "fit table to screen" zoom is calculated, setting the global
      `zoomSliderPosition`, which then determines the logical radius for `ActualCueBall` and
      `ProtractorUnit` (even if hidden). `bankingAimTarget` is initialized. `tableRotationDegrees`
      is reset to 0.
    * **Exiting:** `isBankingMode = false`. `bankingAimTarget` is cleared. `ActualCueBall` is
      typically cleared (user can re-toggle if needed for protractor mode). Zoom resets to default.
* **C. Rendering of Lifted 3D Objects (Rails)**
  To create the illusion of an object being "lifted" above the logical plane (e.g., the pool table
  rails in banking mode), a dedicated perspective matrix (`railPitchMatrix`) MUST be used.
    * The `OverlayState` contains both a base `pitchMatrix` and a separate `railPitchMatrix`.
    * The `railPitchMatrix` is generated in `UpdateStateUseCase` by calling
      `Perspective.createPitchMatrix` and passing a positive floating-point value to the `lift`
      parameter. The `lift` amount is calculated proportionally to the table's logical dimensions (
      derived from `ActualCueBall.radius` in banking mode).
    * The Android Camera's Y-axis is inverted; therefore, to lift an object "up" (closer to the
      viewer), a POSITIVE Y translation must be applied within the `Perspective` helper:
      `camera.translate(0f, lift, 0f)`.
    * `RailRenderer` draws rail primitives on the canvas transformed by `railPitchMatrix`. The
      rails' logical size and position are determined relative to the table's playing surface
      *before* this lift matrix is applied.
* **D. Rendering of Rotated Objects (The Table)**
  The user-controlled table rotation (`tableRotationDegrees`) in Banking Mode is applied to both
  `pitchMatrix` and `railPitchMatrix` via `matrix.preRotate(...)` in `UpdateStateUseCase`. This
  rotation occurs around the view's logical center *after* the initial 90-degree banking mode
  rotation but *before* the final perspective projection. Renderers themselves should not apply 2D
  rotations for the table itself.
* **E. Table and Rail Rendering (Primitives)**
  Previously, SVG assets were considered for complex shapes. However, for the banking mode table and
  rails, rendering is now done using primitive shapes (stroked rectangles for the table surface,
  stroked lines for rails, stroked circles for pockets and diamonds). This is handled by
  `TableRenderer` and `RailRenderer`.
* **F. Table Rotation in Banking Mode**
  When in Banking Mode, the user can rotate the displayed table using a horizontal slider.
    * State: `OverlayState.tableRotationDegrees`.
    * Event: `MainScreenEvent.TableRotationChanged` updates this state.
    * Logic: The `StateReducer` handles the event and resets `tableRotationDegrees` to 0 when
      Banking Mode is entered or when a full view reset occurs.
    * Rendering: `UpdateStateUseCase` applies this rotation via `preRotate(...)` to `pitchMatrix`
      and `railPitchMatrix`.
* **G. ActualCueBall Sizing in Banking Mode (Key for Tilt/Rotation Stability)**
  When `isBankingMode` is true, the visual screen-space radius of the `ActualCueBall` (the "Banking
  Ball") must accurately reflect its logical size (determined by global zoom) as viewed on the
  tilted/rotated table plane.
    * Its logical position (`actualCueBall.center`) is user-draggable and is not affected by phone
      tilt.
    * Its logical radius (`actualCueBall.radius`) is solely determined by the global zoom level set
      in `StateReducer`.
    * In `BallRenderer.drawScreenSpaceBalls`:
        * The on-screen Y-coordinate (`effectiveCenterY`) is the direct projection of the
          `ActualCueBall`'s logical center using the `pitchMatrix`. No additional "lift" is applied
          to its Y-position.
        * The on-screen visual radius (`visualRadiusOnScreen`) is determined by:
            1. Taking two logical points representing a horizontal diameter of the ball *at its
               current logical center* (e.g., `logicalCenter.x ± logicalRadius, logicalCenter.y`).
            2. Projecting these two logical points to screen space using the current
               `state.pitchMatrix`.
            3. The screen distance between these projected points, halved, gives the
               `visualRadiusOnScreen`.
        * This method ensures the ball's visual size accurately reflects perspective foreshortening
          based on the full transformation of the plane it resides on (including tilt and table
          rotation), using its zoom-defined logical radius as the basis.
* **H. Conditional UI in Banking Mode**
    * The "Toggle Actual Cue Ball" FAB is hidden (the Banking Ball is always active).
    * The "Calculate Bank" menu item text changes to "Back to the Balls."
* **I. Cue Ball Zoom Behavior in Banking Mode**
  When zooming while in Banking Mode, the `ActualCueBall` ("Banking Ball") must appear to stay fixed
  relative to the table's surface.
    * `StateReducer` handles zoom events. If `isBankingMode`, `actualCueBall.center` (logical) is
      adjusted by scaling its vector from the view's logical center (zoom pivot) by the inverse of
      the zoom factor change.
* **J. Touch Handling and Pinch-to-Zoom (`ProtractorOverlayView`)**
    * `ScaleGestureDetector.onTouchEvent(event)` is always called.
    * The `ScaleGestureDetector.SimpleOnScaleGestureListener`'s `onScaleBegin` method sets
      `interactionMode = SCALING` and manages a `gestureInProgress` flag to coordinate with
      single-touch gesture detection.
    * Single-touch gesture determination (in `determineSingleTouchMode` called on `ACTION_DOWN`)
      only proceeds if `interactionMode` is not already `SCALING`.
    * Touch slop for moving the `ActualCueBall` (especially in banking mode) is increased for better
      usability.
* **K. (Formerly L) Visual Sizing of Objects on a Projected Plane (Banking Ball Case Study -
  Summarized by G)**
  The successful approach for the Banking Ball's visual size stability during tilt/rotation relies
  on projecting the ball's *logical diameter* (which is only affected by global zoom) onto the
  screen using the complete `pitchMatrix`. The length of this projected diameter determines the
  visual screen diameter. This directly ties its apparent size to how its constant logical size is
  transformed by the current perspective of the plane it's on.

* **On Impossible Shots (Protractor Mode):** The sole trigger for a warning event is comparing the
  distance from the player's perspective point (A: `ActualCueBall` if visible, else screen
  bottom-center anchor) to the `ProtractorUnit.GhostCueBall` (G) and the
  `ProtractorUnit.TargetBall` (T). Impossible if A-G > A-T.
* **The Overhead Anomaly (Protractor Ghosts):** Lift for protractor ghost balls is
  `radius * sin(pitch)` to ensure concentricity at 0° pitch.
* **State-Driven UI Consistency:** Flags like `areHelpersVisible` drive multiple UI parts.

## 5. Future Development Plan (Renumbered from old Section 5)

* **Bank/Kick Shot Calculator (Refinement):**
    * Improve reflection logic in `LineRenderer` for multi-rail shots (currently supports up to 2
      banks).
    * Consider pocket geometry for line termination/success.
* **Object/Table Detection (Computer Vision):** The ultimate goal.
    * Use OpenCV or ML Kit to detect table boundaries and ball positions.
    * Project screen coordinates to Logical Plane to auto-place `ActualCueBall` and potentially
      align `ProtractorUnit`.
* **"English" / Spin Visualization:** Add UI controls to simulate sidespin, altering tangent lines
  or shot paths.