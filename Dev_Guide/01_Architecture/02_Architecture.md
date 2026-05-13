# 1.3. MVI Architecture Overview

The application uses a Model-View-Intent (MVI) architecture, enforcing a strict Unidirectional Data
Flow (UDF).

## Components

* **View (`MainActivity`, `MainScreen`, Composables):** The UI layer. Responsible only for
  displaying state and forwarding user interactions (events) to the ViewModel. Contains no business
  logic.
* **ViewModel (`MainViewModel`):** The orchestrator. Receives events from the View, forwards them to
  the `StateReducer`, subscribes to data flows (e.g., sensors), and exposes the resulting state via
  a `StateFlow`.
* **State Reducer (`StateReducer` and sub-reducers):** The only component allowed to modify state.
  Takes the current state and an event, and produces a new state as a pure function. Logic is
  subdivided into specialized reducers (e.g., `GestureReducer`, `ControlReducer`).
* **Use Cases (e.g., `UpdateStateUseCase`):** Contain complex business logic and calculations. They
  are invoked after a state has been reduced to derive new properties of the state (e.g.,
  calculating line intersections, obstruction). They do not modify state directly.
* **State (`OverlayState`):** A single, immutable data class that represents the entire state of the
  application. The UI is a pure function of this state.

## Data Flow

1. A user **interacts** with the **View**.
2. The View creates an **Event** (e.g., `MainScreenEvent.Drag`) and sends it to the **ViewModel**.
3. The ViewModel receives the Event and forwards it to the **StateReducer**.
4. The **StateReducer** takes the current `OverlayState` and the Event and produces a new, immutable
   `OverlayState`.
5. The ViewModel runs the `UpdateStateUseCase` on the new state to calculate derived properties.
6. The final, updated `OverlayState` is emitted from the ViewModel's `StateFlow`.
7. The **View**, observing the `StateFlow`, re-renders itself based on the new state.

## Rendering Pipeline Specification

The final projection matrix must be constructed in a precise order within the `UpdateStateUseCase`
to ensure correct visual behavior.

1. **World Transformations (2D):** A `worldMatrix` is created to handle 2D transformations on the
   logical plane (zoom).
2. **Camera Transformations (3D):** A separate `perspectiveMatrix` is created using
   `android.graphics.Camera`. This matrix is responsible for applying 3D transformations: first the
   table rotation (around the Y-axis), then the device pitch/tilt (around the X-axis).
3. **Final Assembly:** The final `pitchMatrix` is constructed in this order:
   a. Start with the `worldMatrix` (containing the zoom).
   b.  `postConcat` the `perspectiveMatrix` (containing the 3D rotation and tilt).
   c.  `postTranslate` the result to center it on the screen.