# Project Development Guide: Cue D'état

This document outlines the core architecture, concepts, and future direction of the Cue D'état
application. It serves as a single source of truth to prevent regressions and ensure consistent
development. Consider it a note-to-self for the AI working on this project, and keep it updated
accordingly with ANYTHING that will be useful to the next AI in the next chat.

NEVER change what is written here, only add to it. Always include anything that you note to yourself as a matter of clarification.

## 1. Core Concepts & Official Terminology

A precise vocabulary is critical. The following terms are to be used exclusively.

* **Logical Plane**: An abstract, infinite 2D coordinate system (like graph paper) where all aiming
  geometry is defined and calculated. This is the "world" of the simulation.
* **Screen Plane**: The physical 2D plane of the device's screen. This is the "window" through which
  the user views the Logical Plane.
* **Perspective Transformation**: The process, handled by a single `pitchMatrix`, of projecting the
  Logical Plane onto the Screen Plane to create the 3D illusion. Crucially, this transformation must
  always pivot around the absolute center of the view.
* **On-Screen Elements**:
   * **Protractor Unit**: The primary aiming apparatus. It consists of two components that are
     always linked.
      * **Target Ball**: The logical and visual center of the Protractor Unit. The user drags this
        on-screen to move the entire unit.
      * **Ghost Cue Ball**: The second ball in the unit. Its position on the Logical Plane is always
        derived from the Target Ball's position plus the user-controlled rotation angle.
   * **Actual Cue Ball**: A separate, independent entity representing the real-world cue ball.
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
├── view/
│   ├── model/
│   │   ├── LogicalPlane.kt      // Defines the abstract geometry (ProtractorUnit, ActualCueBall).
│   │   └── Perspective.kt       // Manages the 3D transformation logic.
│   ├── renderer/
│ │ ├── util/
│ │ │ └── DrawingUtils.kt // Shared, static helper functions for drawing math.
│ │ ├── BallRenderer.kt // Draws all ball and ghost ball elements.
│ │ ├── LineRenderer.kt // Draws all line and label elements.
│ │ └── OverlayRenderer.kt // The coordinator. Initializes and calls other renderers.
│   ├── state/
│   │   └── OverlayState.kt      // An immutable snapshot of the entire scene's state.
│   └── ProtractorOverlayView.kt   // The Android View, handles touch input.
└── ui/
└── MainViewModel.kt         // The single source of truth. Manages state and business logic.

**The Golden Rule**: The `ViewModel` is the only component that can create or modify the
`OverlayState`. The `View` and `Renderer` components are "dumb" components that only receive state
and display it.

## 3. Rendering Pipeline

To avoid rendering artifacts, the following order of operations is mandatory:

1. **ViewModel**: Calculates the single, centrally-pivoted `pitchMatrix` based on sensor input. It
   also calculates the logical positions of all objects. This is packaged into an `OverlayState`
   object.
2. **Renderer**: Receives the `OverlayState`.
1. `canvas.concat(pitchMatrix)`: Applies the 3D perspective to the entire canvas once.
2. **Draw Logical Plane**: All elements that exist in the 3D world (Protractor Unit, Actual Cue
   Ball's base, all lines and their labels) are drawn onto this single transformed canvas at their
   logical (x, y) coordinates.
3. **Draw Screen Space**: Elements that don't exist on the 3D plane (the "ghost" effects for the
   balls) are drawn last, without the `pitchMatrix`, using the projected coordinates of their
   logical counterparts.

## 4. Notes from the Void (Lessons Learned)

* **A Word on State**: The purity of the unidirectional data flow is paramount. When a bug appears,
  the first question is always: "Is the `OverlayState` correct?" If the state is right, the bug is
  in the Renderer. If the state is wrong, the bug is in the ViewModel. There is no third option.

* **The Tyranny of Coordinates**: A significant portion of development has been a Sisyphean struggle
  against coordinate systems. A point's meaning is defined entirely by the space it inhabits:
  Logical, Pitched, or Screen. Mapping between them must be done with monastic precision. The
  `ProtractorOverlayView` must only speak in Screen Coordinates to the ViewModel, which then
  translates them to the Logical Plane.

* **On Impossible Shots**: Early warning systems relied on crude angle checks and physical overlap
  detection. These have been deprecated and removed. The sole trigger for a warning event is now a
  more elegant and geometrically sound check. It compares the distance from the player's perspective
  point (A) to the GhostCueBall (G) and the TargetBall (T). A shot is deemed impossible if the
  distance A-G is greater than the distance A-T, as this implies aiming "behind" the target. If the
  `ActualCueBall` is visible, its center serves as point A. If the `ActualCueBall` is hidden, point
  A defaults to the logical point corresponding to the bottom-center of the screen. This unified
  logic applies universally, providing a single, robust principle for all aiming scenarios.

* **The Overhead Anomaly**: The initial "lift" logic correctly placed the 3D ghost ball "on top" of
  the 2D base, but this created a visual disconnect when viewed from directly overhead (0° pitch).
  From this angle, the ghost appeared as a separate circle floating above the base, rather than
  being perfectly aligned with it. The illusion of a single 3D object was broken. The lift
  calculation was corrected to be proportional to the sine of the pitch angle (
  `lift = radius * sin(pitch)`). This ensures the lift is 0 at 0° pitch (making the ghost and base
  concentric) and increases smoothly as the phone tilts, preserving the 3D illusion across all
  viewing angles.

* **State-Driven UI Consistency**: The `areHelpersVisible` flag in `OverlayState` is an example of a
  single state driving multiple UI changes. It not only toggles the helper text on the
  `OverlayRenderer` but also controls the branding in the `TopControls` composable and the
  appearance of the FABs (switching between icons and text). This pattern should be maintained to
  ensure a consistent and predictable UI. A change in one part of the app's "mode" should be
  reflected logically across all relevant components.

* **The Labyrinth of Label Placement**: The seemingly simple act of placing a text label next to a
  line became a tragicomedy of errors.
   * **Initial Diagnosis**: It was assumed that a small, fixed horizontal offset (`hOffset`) would
     suffice. This failed spectacularly, producing no visible change. This failure was, in itself, a
     success: it proved the logical coordinate space was vastly larger than assumed, and that our
     understanding of scale was flawed.
   * **The Red Herring of `drawTextOnPath`**: The primary tool, `drawTextOnPath`, was treated as a
     black box. Its `vOffset` parameter was discovered to be the silent culprit, pushing labels an
     enormous perpendicular distance away from their intended paths, even when the horizontal offset
     was small. The realization was that we were trying to finesse a sledgehammer.
   * **The Refactor That Wasn't**: An attempt to solve the problem by refactoring the
     `OverlayRenderer` into smaller, more specialized classes was architecturally sound but executed
     poorly. It severed dependencies and broke the build, proving that a good idea implemented badly
     is often worse than a bad idea implemented well. The subsequent decision to revert the
     refactor, fix the build, and *then* re-implement the refactor correctly was a critical lesson
     in not being afraid to retreat and regroup.
   * **The Rotational Farce**: A subsequent attempt to use a more "direct" coordinate calculation
     with `canvas.rotate()` resulted in all labels comically stacking on top of each other, anchored
     to the wrong point and rotated into nonsense. This was a valuable lesson in humility.
   * **The Final, Simple Truth**: The solution, as is often the case, was to stop fighting the tool
     and understand the environment. By returning to `drawTextOnPath` and setting its perpendicular
     offset (`vOffset`) to zero, we regained control. The horizontal offset (`hOffset`) was then
     made dynamic—a multiple of the ball's on-screen radius—ensuring the labels now sit a
     predictable, scalable distance from their origin points.

## 5. Future Development Plan

The current foundational architecture is designed to support future expansion.

* **Bank/Kick Shot Calculator**: A virtual table boundary can be added to the Logical Plane. The
  AimingLine can then be reflected off these boundaries to project multi-rail shots.
* **Object/Table Detection (Computer Vision)**: The ultimate goal.
   * Use OpenCV or ML Kit to detect the boundaries of the pool table. These screen coordinates can
     be projected back to the Logical Plane to define the virtual table.
   * Detect the screen coordinates of the cue ball and object balls. These can be used to
     automatically place the `ActualCueBall` and `ProtractorUnit` on the Logical Plane, removing the
     need for manual positioning.
* **"English" / Spin Visualization**: Add UI controls to simulate sidespin, which would alter the
  path of the tangent lines.
