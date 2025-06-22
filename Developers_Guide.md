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
          logical position is fixed at the view's center (`state.protractorUnit.center`). Its
          logical radius is set by Global Zoom (`state.protractorUnit.radius`).
        * **Ghost Cue Ball (Protractor):** The second ball in the `ProtractorUnit`. Its *absolute
          logical position* (`state.protractorUnit.protractorCueBallCenter`) is calculated by the
          `ProtractorUnit` class based on the Target Ball's center, the unit's radius, and
          `state.protractorUnit.rotationDegrees`. Its logical radius is the same as the Target
          Ball's.
          Both Target and Ghost Cue Ball have a 2D logical representation (drawn by
          `BallRenderer.drawLogicalBalls`) and a 3D screen-space "ghost" effect (drawn by
          `BallRenderer.drawScreenSpaceBalls` with lift).
    * **ActualCueBall:** A user-draggable logical ball.
        * **In Protractor Mode (Optional):** Can be toggled by the user. Used for visualizing shots
          originating from a specific point (e.g., jump shots). Rendered with a "lifted" 3D ghost
          effect.
          Its logical radius (`state.actualCueBall.radius`) is set by Global Zoom.
        * **In Banking Mode (Mandatory, becomes the "Banking Ball"):** Always visible and represents
          the
          cue ball on the table. Its logical radius is set by Global Zoom. Rendered *on* the table
          plane (no lift for its Y-position, visual size reflects perspective on the plane).
    * **Table Visuals (Banking Mode Only):** Wireframe representation of the pool table surface,
      rails,
      pockets, and diamonds.
        * Logically anchored at the view's center (`viewWidth/2, viewHeight/2`).
        * Its logical scale is determined by `tableToBallRatio * ActualCueBall.radius` (where
          `ActualCueBall.radius` is the current Global Zoom-scaled radius when in banking mode).
        * The table surface is rendered on the `pitchMatrix`. Rails are rendered on the
          `railPitchMatrix`.
        * Can be rotated by the user via `tableRotationDegrees`.
    * **Lines:**
        * **Protractor Shot Line (Protractor Mode):** From `ActualCueBall.center` (if visible, else
          a default
          screen anchor's logical projection) through the `ProtractorUnit.GhostCueBall`'s absolute
          logical center. Drawn in absolute logical coordinates.
        * **Aiming Line (Protractor Mode):** From `ProtractorUnit.GhostCueBall`'s local position
          through `ProtractorUnit.TargetBall`'s local position (which is 0,0 in the transformed
          canvas). Drawn relative to the transformed ProtractorUnit canvas.
        * **Tangent Lines & Angle Lines (Protractor Mode):** Relative to the transformed
          ProtractorUnit canvas, originating from the Ghost Cue Ball's local position or Target
          Ball's local position respectively.
        * **Banking Shot Line (Banking Mode):** From `ActualCueBall.center` (the "Banking Ball")
          towards
          `bankingAimTarget`, with reflections off logical table boundaries. Drawn in absolute
          logical coordinates.

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

**The Golden Rule**: ViewModel orchestrates. StateReducer computes state. UpdateStateUseCase
computes derived state. Renderers display.

## 3. Rendering Pipeline (Conceptual)

To avoid rendering artifacts and ensure correct transformations, the following conceptual order is
maintained:

1. **ViewModel**: Orchestrates event handling. For position-based events from
   `ProtractorOverlayView` (which are in screen coordinates), it uses `Perspective.screenToLogical`
   with the `inversePitchMatrix` to convert them to logical coordinates and then dispatches new
   internal `UpdateLogical...` events to the `StateReducer`.
2. **StateReducer**: Receives all `MainScreenEvent` types (including internal logical position
   events) and computes a new `OverlayState`. This state contains all fundamental logical
   properties: positions, radii (derived from Global Zoom), rotations, mode flags, and
   theme/luminance choices for drawn elements.
3. **UpdateStateUseCase**: Takes the new `OverlayState` from the reducer. It calculates derived
   geometric properties essential for rendering, primarily:
    * `pitchMatrix`: The main perspective transformation matrix based on `pitchAngle`, view
      dimensions, and potentially table rotation (if in banking mode).
    * `railPitchMatrix`: A separate perspective matrix for banking mode rails, incorporating a
      calculated "lift" based on table dimensions.
    * `inversePitchMatrix`: The inverse of `pitchMatrix`, used by the ViewModel for
      screen-to-logical conversions.
    * `hasInverseMatrix`: A flag indicating if `pitchMatrix` was invertible.
    * `isImpossibleShot`: A boolean flag for protractor mode aiming.
      It returns the `OverlayState` augmented with these calculated matrices and flags.
4. **ViewModel**: Receives the fully processed `OverlayState` (with matrices) from
   `UpdateStateUseCase` and emits it to the UI (`MainScreen`). It also handles discrete events like
   showing dialogs or navigating. The `appControlColorScheme` in `OverlayState` is updated here
   based on the `MaterialTheme.colorScheme` from `MainScreen`.
5. **Renderer (`ProtractorOverlayView` via `OverlayRenderer`):**
    * Receives the final `OverlayState`.
    * `PaintCache.updateColors()` is called (from `MainScreen`'s `AndroidView.update` lambda),
      passing the `OverlayState` (for `isForceLightMode`, `luminanceAdjustment`) and `systemIsDark`
      status. This configures all `Paint` objects for the custom drawing.
    * **`OverlayRenderer.draw()`:**
        * `canvas.save()`
        * `canvas.concat(state.pitchMatrix)`: Applies the primary perspective transformation to the
          canvas. All subsequent "logical" drawing happens on this transformed canvas.
        * **Logical Elements Drawing (on `pitchMatrix`-transformed canvas):**
            * `BallRenderer.drawLogicalBalls()`:
                * Draws `ActualCueBall` (if any) at its absolute logical coordinates (
                  `state.actualCueBall.center`).
                * **Protractor Mode:** Draws `ProtractorUnit.TargetBall` at its absolute logical
                  center (`state.protractorUnit.center`). Draws `ProtractorUnit.GhostCueBall` at its
                  absolute logical center (`state.protractorUnit.protractorCueBallCenter`). No local
                  canvas transforms (translate/rotate) are used *within this method* for these
                  protractor components; they rely on their pre-calculated absolute logical
                  positions.
                * **Banking Mode:** Only `ActualCueBall` (as Banking Ball) is drawn by this method,
                  using its absolute logical coordinates.
            * `LineRenderer.drawLogicalLines()`:
                * **Protractor Mode:**
                    * `ProtractorShotLine`: Drawn using absolute logical coordinates (from
                      `ActualCueBall` or anchor, to `ProtractorUnit.GhostCueBall.center`).
                    * For Tangent Lines, Protractor Aiming Line, and Angle Lines: `LineRenderer`
                      performs a *local* `canvas.save()`, then `canvas.translate()` to the
                      `ProtractorUnit.TargetBall`'s logical center, then `canvas.rotate()` by
                      `ProtractorUnit.rotationDegrees`. Lines are then drawn using coordinates
                      *relative to this local, rotated system* (e.g., Ghost Cue Ball is at
                      `(0, 2*radius)` in this local unrotated frame before the canvas rotation takes
                      effect). `canvas.restore()` is called afterwards.
                * **Banking Mode:** `BankingShotLine` (with reflections) is drawn using absolute
                  logical coordinates for the `ActualCueBall` (Banking Ball) and `bankingAimTarget`.
                  Table boundaries for reflection are also calculated in logical space.
            * `TableRenderer.draw()` (Banking Mode Only): Draws table surface and pockets at their
              logical positions (centered in the view).
        * `canvas.restore()` (from the initial `concat(state.pitchMatrix)`)
        * **Lifted Rails (Banking Mode Only):**
            * `canvas.save()`
            * `canvas.concat(state.railPitchMatrix)`: Applies the rail-specific perspective (with
              lift).
            * `RailRenderer.draw()`: Draws rails and diamonds at their logical positions (centered
              with the table).
            * `canvas.restore()`
        * **Screen Space Elements (3D "Ghosts" - drawn last, directly on screen canvas, no further
          matrix concat by `OverlayRenderer`):**
            * `BallRenderer.drawScreenSpaceBalls()`: This method internally uses `state.pitchMatrix`
              to project the *logical centers* of balls to get their *screen anchor positions*. It
              then applies "lift" (for protractor ghosts) or calculates perspective-correct visual
              radii.
                * **Protractor Mode:** Renders 3D ghosts for `ProtractorUnit.TargetBall`,
                  `ProtractorUnit.GhostCueBall`, and the optional `ActualCueBall`.
                * **Banking Mode:** Renders the 3D ghost for `ActualCueBall` (the Banking Ball),
                  ensuring it appears *on* the table surface (no Y-lift for its center) and its
                  visual size is stable during tilt/rotation (see point L below).

## 4. Core Operational Modes & Entity Behavior

The application operates in two distinct modes: Protractor Mode and Banking Mode. The
`ActualCueBall` entity behaves differently depending on the active mode. The table itself (in
banking mode) is always logically anchored at the center of the view.

### 4.1. Protractor Mode

* **`ProtractorUnit`**:
    * Comprises a `TargetBall` (logical center) and a `GhostCueBall`.
    * The `ProtractorUnit.center` is fixed at the logical view center. Its logical radius is
      determined by Global Zoom.
    * `ProtractorUnit.rotationDegrees` is user-controlled.
    * The `ProtractorUnit.GhostCueBall.center` (absolute logical) is calculated by `ProtractorUnit`
      based on its center, radius, and rotation.
    * Visuals: Both components have a 2D logical representation and a 3D screen-space "ghost" effect
      with lift.
* **`ActualCueBall` (Optional in Protractor Mode)**:
    * Toggled by the user. Logical center is user-draggable. Logical radius by Global Zoom.
    * Visuals: Rendered with a "lifted" 3D ghost effect.
* **Lines**:
    * `ProtractorShotLine`: From `ActualCueBall.center` (if visible, else default anchor) to
      `ProtractorUnit.GhostCueBall.center`.
    * `AimingLine`, `TangentLines`, `AngleLines`: Drawn relative to the `ProtractorUnit`'s local
      coordinate system (see Rendering Pipeline and Point M).

### 4.2. Banking Mode (`isBankingMode = true`)

* **Table Visuals**:
    * Logically anchored at the view center. Scale relative to `ActualCueBall.radius` (Global Zoom).
    * Rotated by `tableRotationDegrees`. Surface on `pitchMatrix`, rails on `railPitchMatrix`.
* **`ActualCueBall` (Serves as the "Banking Ball")**:
    * Always present. Logical center is user-draggable. Logical radius by Global Zoom.
    * Visuals: Position projected by `pitchMatrix`. Visual size stable during tilt/rotation (see
      Point L). Rendered *on* the plane.
* **`bankingAimTarget`**: Logical point set by user drag.
* **"Banking Shot Line"**: From `ActualCueBall.center` to `bankingAimTarget`, with reflections.
* **Protractor Unit and its lines are NOT drawn.**

### 4.3. Global Zoom and Radii

* `zoomSliderPosition` controls Global Zoom via `ZoomMapping`.
* `StateReducer.getCurrentLogicalRadius()` calculates the *logical radius* for `ProtractorUnit` and
  `ActualCueBall` based on Global Zoom and view dimensions, ensuring consistent logical sizing.

## 5. Key Implementation Learnings & Mandates (Chronological where possible)

* **A. Unidirectional Data Flow**: `ViewModel` orchestrates, `StateReducer` computes `OverlayState`,
  `UpdateStateUseCase` computes derived matrices/flags, `Renderers` display. State is the single
  source of truth.
* **B. Coordinate Systems**: Precision is vital. `ProtractorOverlayView` sends screen coords;
  `ViewModel` converts to logical using `inversePitchMatrix`; `StateReducer` works with logical;
  `Renderers` use `pitchMatrix` to project logical to screen.
* **C. Impossible Shots (Protractor Mode)**: Distance check: A (anchor) to G (GhostCue) vs. A to T (
  TargetBall). Impossible if A-G > A-T.
* **D. Overhead Anomaly (Protractor Ghosts Lift)**: Lift for protractor ghosts is
  `radius * sin(pitch)` for concentricity at 0° pitch.
* **E. State-Driven UI Consistency**: Flags like `areHelpersVisible` drive multiple UI parts (
  overlay text, FAB appearance, etc.).
* **F. Label Placement**: `LineTextRenderer` uses dynamic offsets (multiples of ball radius) for
  consistent label distance from lines across zoom levels.
* **G. Domain Layer Purity**: `StateReducer` and `UpdateStateUseCase` must not depend on UI-layer
  components (e.g., `ColorScheme` objects). Theme/luminance choices for *drawn elements* are flags
  in `OverlayState`, interpreted by `PaintCache`.
* **H. Zoom Functionality (Pinch & Slider)**:
    * `ProtractorOverlayView` uses `ScaleGestureDetector`. Raw `scaleFactor` sent via
      `ZoomScaleChanged` event.
    * `StateReducer` applies this factor to the current zoom (derived from `zoomSliderPosition`),
      clamps to `MIN/MAX_ZOOM`, and updates `zoomSliderPosition`. It also updates logical radii of
      all elements.
    * **Banking Mode Zoom Anchor**: To keep the `ActualCueBall` (Banking Ball) fixed relative to the
      *table center* during zoom, `StateReducer` scales the vector from the view center to the
      ball's center by the inverse of the zoom change.
* **I. Rendering of Lifted 3D Objects (Rails)**: `railPitchMatrix` uses a `lift` proportional to
  table's logical dimensions (derived via `ActualCueBall.radius` in banking mode). `RailRenderer`
  draws rails in their correct logical size/position on this lifted matrix.
* **J. Rendering of Rotated Objects (Table)**: `tableRotationDegrees` is applied via `preRotate` to
  `pitchMatrix` and `railPitchMatrix` in `UpdateStateUseCase`, pivoting around view center.
* **K. Table and Rail Rendering (Primitives)**: Banking mode table/rails are drawn using primitive
  stroked shapes by `TableRenderer` and `RailRenderer`.
* **L. Visual Sizing of Banking Ball (Tilt/Rotation Stability):**
  To ensure the `ActualCueBall` (as "Banking Ball") in Banking Mode has a visual size primarily
  driven by global zoom and natural perspective, without erratic resizing during pure phone tilt or
  table rotation:
    * Its logical radius (`actualCueBall.radius`) is solely determined by global zoom (via
      `StateReducer`).
    * Its logical center (`actualCueBall.center`) is user-draggable and fixed during tilt/rotation.
    * In `BallRenderer.drawScreenSpaceBalls` (banking mode):
        1. The ball's screen-projected center (`screenProjectedCenter`) is found using
           `pitchMatrix`.
        2. Its visual screen radius (`visualRadiusOnScreen`) is determined by:
            * Defining two logical points representing a horizontal diameter of the ball *at its
              current logical center* (e.g., `logicalCenter.x ± logicalRadius, logicalCenter.y`).
            * Projecting these two logical points to screen space using the current
              `state.pitchMatrix`.
            * The screen distance between these projected points, halved, gives the
              `visualRadiusOnScreen`.
        3. This method directly measures how the ball's logical diameter appears on screen under the
           full current perspective (tilt, table rotation, zoom via logical radius).
* **M. Protractor Unit 2D Logical Drawing (Rotation Fix):**
  To prevent "double rotation" of the 2D logical `ProtractorUnit.GhostCueBall`:
    * **`BallRenderer.drawLogicalBalls` (Protractor Mode):** Draws `ProtractorUnit.TargetBall` and
      `ProtractorUnit.GhostCueBall` using their *absolute logical coordinates* (
      `state.protractorUnit.center` and `state.protractorUnit.protractorCueBallCenter` respectively)
      directly onto the main canvas (already transformed by `state.pitchMatrix`). No local canvas
      rotation is applied *by BallRenderer* for these.
    * **`LineRenderer.drawLogicalLines` (Protractor Mode for Tangent/Aiming/Angle lines):** Uses a
      *local* `canvas.save/translate(to_TargetBall_center)/rotate(by_ProtractorUnit_rotation)`.
      Lines are then drawn using coordinates *relative to this local, rotated system*, with the
      Ghost Cue Ball's position calculated as its *unrotated* offset from the Target Ball (e.g.,
      `PointF(0, 2 * radius)`). The canvas rotation orients these correctly.
* **N. Theme Control for Drawn Elements vs. App UI:**
    * The app's Material Theme (menus, sliders, etc.) follows system dark/light (via
      `CueDetatTheme`). This is stored in `OverlayState.appControlColorScheme`.
    * Colors of custom-drawn elements (`ProtractorOverlayView`) are controlled by `PaintCache`.
      `PaintCache.updateColors()` uses `uiState.isForceLightMode`, `uiState.luminanceAdjustment`,
      and `systemIsDark` status to modify colors from its internal light/dark palettes for `Paint`
      objects.

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