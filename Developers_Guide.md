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

Discrete Actions: Events that happen at a specific moment, such as the start or end of a gesture (
GestureStarted, GestureEnded). These are handled directly within the onEvent function to manage
state that should not change during a continuous gesture.

Delayed Warning Text Logic: To prevent the sarcastic warning text from flickering during a gesture,
its display is managed by the discrete GestureStarted and GestureEnded events:

GestureStarted: When a touch gesture begins, the warningText in the OverlayState is immediately set
to null. This clears any existing warning text from the screen.

GestureEnded: When the user lifts their finger, this event is fired. The ViewModel checks if the
current state is an isImpossibleShot. If it is, a new sarcastic warning is selected and set in the
OverlayState, causing it to appear after the user has committed to their shot. This separates the
immediate geometric feedback (red lines) from the delayed textual feedback (sarcasm).

