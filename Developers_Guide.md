a# Project Development Guide: Cue D'état

This document outlines the core architecture, concepts, and future direction of the Cue D'état
application. It serves as a single source of truth to prevent regressions and ensure consistent
development. Consider it a note-to-self for the AI working on this project, and keep it updated
accordingly with ANYTHING that will be useful to the next AI in the next chat.

NEVER change what is written here, only add to it. Always include anything that you note to yourself as a matter of clarification.

## 1. Core Concepts & Official Terminology

A precise vocabulary is critical. The following terms are to be used exclusively.

* **Logical Plane**: An abstract, infinite 2D coordinate system (like graph paper) where all aiming
  geometry is defined and calculated. This is the "world" of the simulation. **As of the
  Proportional Scaling refactor, all units on this plane are considered to be in inches.**
* **Screen Plane**: The physical 2D plane of the device's screen. This is the "window" through which
  the user views the Logical Plane.
* **World-to-Screen Transformation**: The process, handled by a single `worldToScreenMatrix`, of
  projecting the
  Logical Plane onto the Screen Plane. This matrix is a combination of a "world" matrix (handling
  the camera's pan and zoom of the logical plane) and a "pitch" matrix (applying the 3D perspective
  tilt).
* **On-Screen Elements**:
    * **Protractor Unit**: The primary aiming apparatus. It consists of two components that are
      always linked.
        * **Target Ball**: The logical and visual center of the Protractor Unit. The user drags this
          on-screen to move the entire unit. Its radius is constant (`STANDARD_BALL_RADIUS`).
        * **Ghost Cue Ball**: The second ball in the unit. Its position on the Logical Plane is
          always derived from the Target Ball's position plus the user-controlled rotation angle.
          Its radius is constant.
    * **Actual Cue Ball**: A separate, independent entity representing the real-world cue ball. Its
      radius is constant.
        * Its visibility is toggled by the user via a FAB.
      * It has a **2D Base**, which exists on the Logical Plane. The user drags the ball on the
        screen, and the app calculates the corresponding position for this base on the Logical
        Plane.
      * It has a **3D Ghost**, which is a visual representation that appears to "float" above the 2D
        base.
    * **Shot Line**: The line representing the player's line of sight to the cue ball.
        * It must be drawn on the Logical Plane to adhere to perspective.
      * Its path is defined as a ray originating from an anchor point and passing through the center
        of the Ghost Cue Ball.
    * **Anchor Points**:
        * If the **Actual Cue Ball** is visible, the anchor is the center of its 2D Base.
      * If the **Actual Cue Ball** is hidden, the anchor is the logical point corresponding to the
        bottom-center of the screen.
    * **Aiming Line**: The line representing the path the Target Ball will take upon impact.
        * It is always the line of centers between the Ghost Cue Ball and the Target Ball, extending
          through the Target Ball.

## 2. Architectural Model & File Structure

The architecture strictly separates data, logic, and presentation.


com/hereliesaz/cuedetat/
├── data/
│ ├── UserPreferencesRepository.kt // Manages saved settings via DataStore.
│ └── ...
├── view/
│   ├── model/
│   │   ├── LogicalPlane.kt      // Defines the abstract geometry (ProtractorUnit, ActualCueBall).
│ │ ├── LogicalTable.kt // Defines the pool table geometry.
│   │   └── Perspective.kt       // Manages the 3D transformation logic.
│   ├── renderer/
│   │   ├── util/
│   │   │   └── DrawingUtils.kt  // Shared, static helper functions for drawing math.
│   │   ├── BallRenderer.kt      // Draws all ball and ghost ball elements.
│   │   ├── LineRenderer.kt      // Draws all line and label elements.
│ │ ├── TableRenderer.kt // Draws the pool table.
│   │   └── OverlayRenderer.kt   // The coordinator. Initializes and calls other renderers.
│   └── state/
│       ├── OverlayState.kt      // An immutable snapshot of the entire scene's state.
│       └── ScreenState.kt       // Defines UI-level states like Toast messages.
├── domain/
│   ├── StateReducer.kt        // Pure function to handle state changes from events.
│   └── UpdateStateUseCase.kt  // Pure function for complex, derived state calculations.
└── ui/
├── composables/ // Small, reusable UI components.
├── MainViewModel.kt // The lean coordinator of state and events.
├── MainScreen.kt // The main Composable screen, assembles components.
└── MainScreenEvent.kt // Defines all possible user interactions.

**The Golden Rule**: The `ViewModel` is the only component that can create or modify the
`OverlayState`. The `View` and `Renderer` components are "dumb" components that only receive state
and display it.

## 3. Rendering Pipeline

To avoid rendering artifacts, the following order of operations is mandatory:

1. **ViewModel**: Calculates the single `worldToScreenMatrix` based on state (pan, zoom, pitch). It
   also calculates the logical positions of all objects. This is packaged into an `OverlayState`
   object.
2. **Renderer**: Receives the `OverlayState`.
    1. `canvas.concat(worldToScreenMatrix)`: Applies the combined pan, zoom, and 3D perspective to
       the entire canvas once.
    2. **Draw Logical Plane**: All elements that exist in the 3D world (Protractor Unit, Actual Cue
       Ball's base, Pool Table, all lines and their labels) are drawn onto this single transformed
       canvas at
       their logical (x, y) coordinates, using logical units (inches) for size.
    3. **Draw Screen Space**: Elements that don't exist on the 3D plane (the "ghost" effects for the
       balls) are drawn last, without the transform matrix, using the projected coordinates of their
       logical counterparts.

## 4. Notes from the Void (Lessons Learned)

* **A Word on State**: The purity of the unidirectional data flow is paramount. When a bug appears,
  the first question is always: "Is the `OverlayState` correct?" If the state is right, the bug is
  in the Renderer. If the state is wrong, the bug is in the ViewModel. There is no third option.

* **The Tyranny of Coordinates**: A significant portion of development has been a Sisyphean struggle
  against coordinate systems. A point's meaning is defined entirely by the space it inhabits:
  Logical, or Screen. Mapping between them must be done with monastic precision. The
  `ProtractorOverlayView` must only speak in Screen Coordinates to the ViewModel, which then
  translates them to the Logical Plane via the `screenToWorldMatrix`.

* **On Impossible Shots**: Early warning systems relied on crude angle checks and physical overlap
  detection. These have been deprecated. The sole trigger for a warning event is now a
  more elegant and geometrically sound check. It compares the distance from the player's perspective
  point (A) to the GhostCueBall (G) and the TargetBall (T). A shot is deemed impossible if the
  distance A-G is greater than the distance A-T.

* **The Overhead Anomaly**: The initial "lift" logic for 3D ghosts failed at a 0° pitch, creating a
  visual disconnect. The lift
  calculation was corrected to be proportional to the sine of the pitch angle (
  `lift = radius * sin(pitch)`). This ensures the lift is 0 at 0° pitch, preserving the 3D illusion
  across all
  viewing angles.

* **The Labyrinth of Label Placement**: An early, tragicomic failure where text labels were being
  drawn at a font size of "38 inches" because the system was conflating logical units with
  display-independent pixels. This led to the creation of the Invisible Kingdom—a perfectly rendered
  world so vast no part of it could be seen. This failure necessitated a fundamental refactor.

* **The Unification of Scale**: The solution to the Invisible Kingdom was to enforce a single,
  unified reality. The **Logical Plane** is now standardized to inches. All object sizes (balls,
  text) are defined in these logical units. The "zoom" slider no longer changes the size of objects;
  it changes the **scale** of the camera's view of the Logical Plane. This was the most critical
  refactor to date and must be the foundation for all future work. A `worldToScreenMatrix` now
  handles all pan, zoom, and pitch transformations in a single, elegant operation.

* **The Severed Hand (A Touch Interaction Failure)**: The Unification of Scale was a success in
  principle but an initial failure in practice because user interaction was not updated to respect
  the new laws of physics. Touch events were happening on the Screen Plane, but the logic still
  thought in terms of the old, broken coordinate system. Nothing moved. The fix required a more
  intelligent `ProtractorOverlayView` that performs hit-testing on the Screen Plane by projecting
  logical object coordinates into screen space using the `worldToScreenMatrix`. Only then can it
  determine user intent (pan camera vs. drag object) and send the correct event to the ViewModel.
  This restored the connection between intent and reality.

## 5. Future Development Plan

The current foundational architecture is designed to support future expansion.

* **Bank/Kick Shot Calculator**: A virtual table boundary can be added to the Logical Plane. The
  AimingLine can then be reflected off these boundaries to project multi-rail shots.
* **Object/Table Detection (Computer Vision)**: The ultimate goal.
    * Use OpenCV or ML Kit to detect the boundaries of the pool table. These screen coordinates can
      be projected back to the Logical Plane to define the virtual table.
    * Detect the screen coordinates of the cue ball and object balls. These can be used to
      automatically place the `ActualCueBall` and `ProtractorUnit` on the Logical Plane, removing
      the need for manual positioning.
* **"English" / Spin Visualization**: Add UI controls to simulate sidespin, which would alter the
  path of the tangent lines.
