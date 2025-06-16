Project Development Guide: Cue D'état
This document outlines the core architecture, concepts, and future direction of the Cue D'état
application. It serves as a single source of truth to prevent regressions and ensure consistent
development. Consider it a note-to-self for the AI working on this project, and keep it updated
accordingly with ANYTHING that will be useful to the next AI in the next chat.

NEVER change what is written here, only add to it. Always include anything that you note to yourself as a matter of clarification.

1. Core Concepts & Official Terminology
   A precise vocabulary is critical. The following terms are to be used exclusively.

Logical Plane: An abstract, infinite 2D coordinate system (like graph paper) where all aiming
geometry is defined and calculated. This is the "world" of the simulation.

Screen Plane: The physical 2D plane of the device's screen. This is the "window" through which
the user views the Logical Plane.

Perspective Transformation: The process, handled by a single pitchMatrix, of projecting the
Logical Plane onto the Screen Plane to create the 3D illusion. Crucially, this transformation must
always pivot around the absolute center of the view.

On-Screen Elements:

Protractor Unit: The primary aiming apparatus. It consists of two components that are
always linked.

Target Ball: The logical and visual center of the Protractor Unit. The user drags this
on-screen to move the entire unit.

Ghost Cue Ball: The second ball in the unit. Its position on the Logical Plane is
always derived from the Target Ball's position plus the user-controlled rotation angle.

Actual Cue Ball: A separate, independent entity representing the real-world cue ball.

Its visibility is toggled by the user via a FAB.

It has a 2D Base, which exists on the Logical Plane. The user drags the ball on the
screen, and the app calculates the corresponding position for this base on the Logical
Plane.

It has a 3D Ghost, which is a visual representation that appears to "float" above the 2D
base.

Shot Line: The line representing the player's line of sight to the cue ball.

It must be drawn on the Logical Plane to adhere to perspective.

Its path is defined as a ray originating from an anchor point and passing through the center
of the Ghost Cue Ball.

Anchor Points:

If the Actual Cue Ball is visible, the anchor is the center of its 2D Base.

If the Actual Cue Ball is hidden, the anchor is the logical point corresponding to the
bottom-center of the screen.

Aiming Line: The line representing the path the Target Ball will take upon impact.

It is always the line of centers between the Ghost Cue Ball and the Target Ball, extending
through the Target Ball.

2. Architectural Model & File Structure
   The architecture strictly separates data, logic, and presentation.

3. ViewModel and State Management (MainViewModel.kt)
   The MainViewModel serves as the central nervous system of the application. It does not perform
   complex calculations itself, but rather orchestrates the flow of information between the user
   interface, background services (like the SensorRepository), and the state-modification logic.

Single Source of Truth: The ViewModel holds the canonical application state in a
MutableStateFlow<OverlayState>. The UI observes this flow and redraws only when the state object
changes. This ensures a predictable, unidirectional data flow.

Event-Driven Logic: All user interactions and sensor updates are funneled through a single entry
point: the onEvent(event: MainScreenEvent) function. This function acts as a triage center,
delegating tasks based on the event type.

Continuous vs. Discrete Events: The onEvent function makes a critical distinction between two types
of events:

Continuous State Updates: Events that happen in real-time during a gesture (e.g., UnitMoved,
RotationChanged). These are passed to the updateContinuousState function, which runs the
StateReducer and UpdateStateUseCase to calculate the new geometry and immediately update the _
uiState. This allows visual elements like the red warning lines to appear instantly.

Delayed Warning Text Logic: To prevent the sarcastic warning text from flickering during a gesture,
its display is managed by the discrete GestureStarted and GestureEnded events:

GestureStarted: When a touch gesture begins, the warningText in the OverlayState is immediately set
to null. This clears any existing warning text from the screen.

GestureEnded: When the user lifts their finger, this event is fired. The ViewModel checks if the
current state is an isImpossibleShot. If it is, a new sarcastic warning is selected and set in the
OverlayState, causing it to appear after the user has committed to their shot. This separates the
immediate geometric feedback (red lines) from the delayed textual feedback (sarcasm).

4. Key Implementation Learnings & Mandates (June 16, 2025)

   This section captures critical design decisions and implementation details that have been
   clarified
   through development iterations. Adherence to these points is mandatory to prevent regressions.

   A. Operational Modes: Protractor vs. Banking
   The application has two mutually exclusive primary modes:
   - Protractor Mode: The default mode. All standard aiming tools (Protractor Unit, Target Ball,
     Ghost Cue Ball, Tangent Lines, etc.) are active. The user moves these tools to line up shots.
   - Banking Mode: Activated by the "Calculate Bank" menu item. In this mode, the Protractor
     Unit and its related elements are hidden. A static, wireframe pool table is displayed instead
     as a fixed frame of reference. The `ActualCueBall` is the primary interactive element.

   B. State Transition for Banking Mode
   Entering Banking Mode is a complex state change handled by the StateReducer and must perform the
   following actions simultaneously:
   1. Set isBankingMode = true.
   2. Force the ActualCueBall to be visible and positioned at the exact logical center of the view
      (viewWidth / 2f, viewHeight / 2f).
   3. Calculate and apply a new zoom level to ensure the entire wireframe table is visible on the
      screen. This is not a user-controlled zoom.
   4. All other aiming elements (Protractor Unit, warnings, etc.) are disabled/hidden.

   C. Rendering of Lifted 3D Objects (Rails)
   To create the illusion of an object being "lifted" above the logical plane (e.g., the pool table
   rails), a dedicated perspective matrix MUST be used.
   - The OverlayState must contain both a base pitchMatrix and a separate railPitchMatrix.
   - The railPitchMatrix is generated in the UpdateStateUseCase by calling
     Perspective.createPitchMatrix and passing a positive floating-point value to the `lift`
     parameter.
   - The Android Camera's Y-axis is inverted; therefore, to lift an object "up" (closer to the
     viewer), a POSITIVE Y translation must be applied within the Perspective helper.
     `camera.translate(0f, lift, 0f)`.

   D. Rendering of Rotated Objects (The Table)
   Any rotation of a logical object (like the 90-degree rotation of the table in Banking Mode) MUST
   be applied to its transformation matrix *before* the perspective projection. This is done by
   calling `matrix.preRotate(...)` in the `UpdateStateUseCase` after the matrix is created but
   before it is set in the state. Renderers themselves should not apply 2D rotations.

   E. High-Fidelity Shape Rendering (Vector Assets)
   For any complex, static shape required by the design (e.g., the pool table rails), programmatic
   approximation using lines and arcs is FORBIDDEN due to repeated failures.
   - The mandatory approach is to use user-provided SVG assets.
   - The SVG's path data string (the `d` attribute) MUST be parsed using
     `androidx.core.graphics.PathParser.createPathFromPathData()`.
   - The renderer must then scale the parsed path to fit the dynamic logical dimensions required by
     the application state.

The architecture strictly separates data, logic, and presentation.

