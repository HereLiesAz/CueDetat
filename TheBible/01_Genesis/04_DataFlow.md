# The Architecture of the World

This document describes the MVI (Model-View-Intent) architecture of the application, which follows a strict unidirectional data flow. This is the law.

## The Components

* **View (`MainActivity`, `MainScreen`, Composables):** The UI layer. It is responsible only for displaying the state and forwarding user interactions (events) to the ViewModel. It contains no business logic. It is, and must remain, a beautiful idiot.
* **ViewModel (`MainViewModel`):** The High Priest. It receives events from the View. It translates these events into logical actions and dispatches them to the `StateReducer`. It also subscribes to data flows (like sensors) and transforms them into events. It holds no state of its own, save for the `StateFlow` of the `OverlayState`.
* **State Reducer (`StateReducer` and sub-reducers):** The Chancellery. This is the only component allowed to modify the state. It takes the current state and an event, and produces a new state. It is a pure function. Its logic is subdivided into smaller, specialized reducers (e.g., `GestureReducer`, `ControlReducer`) for clarity and sanity.
* **Use Cases (e.g., `UpdateStateUseCase`, `CalculateBankShot`):** The Oracles. These classes contain complex business logic and calculations. They are invoked by the ViewModel after a state has been reduced to derive new properties of the state (e.g., calculating line intersections, obstruction). They do not modify state directly.
* **State (`OverlayState`):** The One True Source of Truth. A single, immutable data class that represents the entire state of the application at any given moment. Every pixel on the screen is a direct function of this state.

## The Flow

1.  A user **interacts** with the **View** (e.g., drags a finger).
2.  The View creates an **Event** (e.g., `MainScreenEvent.Drag`) and sends it to the **ViewModel**.
3.  The ViewModel receives the Event and dispatches it to the **StateReducer**.
4.  The **StateReducer** takes the current `OverlayState` and the Event, and produces a new, immutable `OverlayState`.
5.  The new `OverlayState` is emitted from the ViewModel's `StateFlow`.
6.  The **View**, observing the `StateFlow`, receives the new state and re-renders itself accordingly.

## The Rendering Pipeline Gospel

To avoid rendering artifacts and maintain sanity, the following order of operations within the `UpdateStateUseCase` is mandatory. It is the architectural law for creating the final projection matrix.

1.  **World Transformations (2D):** A `worldMatrix` is created. This matrix handles all transformations on the flat, logical plane.
    *   It first applies the `zoom` factor as a 2D scale.
    *   It then applies the `tableRotationDegrees` as a 2D rotation.
2.  **Camera Transformations (3D):** A separate `perspectiveMatrix` is created using the `android.graphics.Camera` class. This matrix is responsible *only* for applying the 3D perspective tilt based on the device's sensor pitch.
3.  **Final Assembly:** The final `pitchMatrix` is constructed in a precise order:
    a. Start with the `worldMatrix` (which contains the zoomed and rotated logical plane).
    b. `postConcat` the `perspectiveMatrix` to apply the 3D tilt to the already-transformed world.
    c. `postTranslate` the entire result to center it on the screen.

This strict order ensures that zoom and rotation are 2D operations on the logical plane, and tilt is a 3D operation on the camera's view of that plane, preventing visual corruption like the "wobble" or "barrel roll."