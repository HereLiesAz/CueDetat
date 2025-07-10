# 03: Rendering Pipeline

1.  **Event (UI -> ViewModel):** A user interaction occurs in a Composable. An `MainScreenEvent` is sent to the `MainViewModel`.

2.  **Orchestration (ViewModel):**
    * For screen-space events (like dragging a ball from the `pointerInput` modifier), the ViewModel uses the `inversePitchMatrix` from the current state to convert screen coordinates to logical coordinates.
    * It then dispatches a new `Drag` event with these logical coordinates.

3.  **Reduction (ViewModel -> StateReducer):** The `StateReducer` receives the event. It takes the current state and the event, and returns a new, updated state without any side effects. Example: It updates the position of a ball model.

4.  **Derivation (ViewModel -> UpdateStateUseCase):** The ViewModel takes the new state from the reducer and passes it to the `UpdateStateUseCase`. This use case calculates all derived data:
    * Calculates the `pitchMatrix` and its inverse based on orientation and zoom.
    * Calculates the position of the `ghostCueBall`.
    * Determines the `isImpossibleShot` flag.
    * It returns a fully updated `OverlayState`.

5.  **State Update (ViewModel -> UI):** The ViewModel updates its `_overlayState` Flow with the new, final state.

6.  **Render (UI):**
    * The `MainScreen` Composable recomposes based on the new state.
    * A `Canvas` Composable's `onDraw` block is triggered, and it calls the `OverlayRenderer` to draw the scene using the new state.
    * The heresy of `AndroidView` has been purged. The UI is pure Compose.

***
## Addendum: The Rendering Pipeline Gospel

To avoid rendering artifacts and maintain sanity, the following order of operations within the `OverlayRenderer` is mandatory. It is not a suggestion. It is the law.

1.  **ViewModel Responsibility**: The `ViewModel` is responsible for one thing: producing a correct and complete `OverlayState`. It calculates the single, centrally-pivoted `pitchMatrix` based on sensor input and computes the logical positions of all objects. This is all packaged and sent down to the renderer.

2.  **Renderer Responsibility**: The `OverlayRenderer` receives the final `OverlayState` and does nothing but draw. It performs no calculations. Its `draw` method must adhere to this sequence:
    * **Pass 1: Pitched Table Surface & On-Plane Elements:**
        * `canvas.save()`
        * `canvas.concat(pitchMatrix)`: The 3D perspective is applied to the entire canvas *once*. All subsequent drawing operations until `canvas.restore()` will be in the transformed (pitched) space.
        * Draw all elements that exist *on* the 3D world plane (the `TableModel` and all lines and their labels) onto this single transformed canvas at their logical `(x, y)` coordinates.
        * `canvas.restore()`
    * **Pass 2: Lifted Rails (Banking Mode Only):**
        * `canvas.save()`
        * `canvas.concat(railPitchMatrix)`: A separate matrix with a vertical lift is used for the rails.
        * Draw the rails.
        * `canvas.restore()`
    * **Pass 3: Screen-Space "Ghost" Effects (Protractor Mode Only):**
        * These elements do not exist on the logical 3D plane. Their positions are calculated by projecting their logical counterparts' centers to the screen, and then applying a "lift" offset.
        * Draw the "ghost" versions of the `TargetBall`, `GhostCueBall`, and `ActualCueBall` directly onto the screen canvas, without the `pitchMatrix`.