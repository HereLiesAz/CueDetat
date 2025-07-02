# Developer's Guide: Cue D'état Lite

This document provides essential context for maintaining and developing the Cue D'état Lite Android application. It is intended for both human developers and AI assistants.

## Core Concepts & Architecture

The application's primary function is to overlay aiming graphics on a live camera feed of a pool table. The architecture is a blend of traditional Android Views (for camera and custom drawing) and Jetpack Compose (for the UI and state management), all orchestrated by a central `MainViewModel`.

### Key Components

1.  **`MainViewModel`**: The central brain of the application. It holds the canonical state, processes user and sensor events, and orchestrates updates between the UI and the rendering logic.
2.  **`OverlayState`**: A comprehensive data class holding *all* state information for the UI and rendering, including screen dimensions, sensor data, zoom levels, user settings, and the state of the objects on the screen.
3.  **`ScreenState`**: A nested data class within `OverlayState` that specifically holds the state of the logical objects to be rendered (e.g., the target ball, aiming angle).
4.  **`ProtractorOverlayView`**: A custom Android `View` responsible for all the graphical rendering (balls, lines, etc.). It is placed on top of the camera preview. It receives the `OverlayState` from the ViewModel and uses a set of `renderer` classes to draw on its `Canvas`. It is also the primary source of user touch events (drags, pinches).
5.  **`UpdateStateUseCase`**: A domain-layer class responsible for taking the current `OverlayState` and calculating derived values, most importantly the perspective matrices for rendering based on sensor data (pitch/roll) and zoom level.
6.  **`StateReducer`**: A domain-layer class responsible for handling discrete state changes, such as moving the target ball or resetting the view. It ensures state transitions are predictable.
7.  **Jetpack Compose UI (`MainScreen.kt`, etc.)**: All UI controls (buttons, sliders, menus) are built with Compose. They read state from the `MainViewModel` and send `MainScreenEvent`s back to it to signal user actions.

### Data Flow & Rendering Loop

1.  **Event Occurs**: A user drags a finger (`ProtractorOverlayView`), moves a slider (`ZoomControls`), or a new sensor reading arrives (`SensorRepository`).
2.  **Event Sent**: An appropriate `MainScreenEvent` is sent to the `MainViewModel`.
3.  **ViewModel Processes**:
   *   The `MainViewModel` receives the event.
   *   It updates the relevant properties in a new `OverlayState` object (e.g., `zoomSliderPosition`, `aimingAngleDegrees`, `currentOrientation`).
   *   For state changes like moving a ball, it uses the `StateReducer`.
4.  **UseCase Calculates Derived State**: The ViewModel passes the new `OverlayState` to the `UpdateStateUseCase`. This use case calculates the complex perspective matrices needed for rendering based on the current orientation and zoom.
5.  **State Emitted**: The ViewModel emits the final, updated `OverlayState` to its subscribers.
6.  **UI Recomposition**:
   *   Jetpack Compose UI elements observe the state and recompose if necessary (e.g., the zoom slider thumb moves).
   *   The `AndroidView` wrapper for `ProtractorOverlayView` receives the new state and calls `view.updateState()`.
7.  **Custom View Redraw**: `ProtractorOverlayView`'s `updateState` method triggers an `invalidate()`, causing its `onDraw()` method to be called. It then uses its internal `renderer` classes to draw the scene on the canvas using the new state and matrices.

## Core Aiming Mechanic (As of July 2025)

**SUCCESSFUL REFACTOR:** The aiming mechanic has been fundamentally changed. Previous attempts to manage separate cue and ghost balls were complex and buggy. The current system is more stable and predictable.

*   **No Movable Cue Ball:** The user does **not** directly drag a "cue ball" or "ghost ball" on the screen.
*   **Target Ball is Key:** The user can drag and place the **Target Ball**. The entire view (camera and overlay) is centered on this Target Ball.
*   **Rotation via Aiming Angle:** The "Ghost Ball" is now a calculated entity. Its position is determined by the `aimingAngleDegrees` property in the `ProtractorUnit`. It is always placed perfectly tangent to the Target Ball, at a distance of two ball radii, opposite the aiming angle.
*   **Gesture Control:** A single-finger drag gesture that does not start on the Target Ball will modify the `aimingAngleDegrees`, causing the Ghost Ball and the aiming line to rotate around the Target Ball.

## Key Successes & Refactors

*   **State Unification:** Initially, state was scattered. It has been successfully centralized into the `OverlayState` and `ScreenState` data classes, managed exclusively by the `MainViewModel`. This is a stable pattern that should be maintained.
*   **UI Migration to Compose:** The entire UI layer (buttons, menus, sliders) has been successfully migrated to Jetpack Compose, removing legacy XML layouts and simplifying UI logic.
*   **Simplified Event Handling:** The `ProtractorOverlayView` was refactored from having multiple specific listeners (`onRotate`, `onScale`, etc.) to a single `onEvent: (MainScreenEvent) -> Unit` listener. This greatly simplifies the communication bridge between the View and the ViewModel.
*   **Stable Aiming Mechanic:** The pivot to a target-ball-centric, angle-based aiming system (see above) resolved numerous bugs and state management headaches related to object collision and positioning. **This is the current "correct" way to handle aiming.**

## Failures & Lessons Learned

*   **Manual Dependency Injection:** The initial project used manual DI within the `MyApplication` class. This became brittle and led to runtime crashes. The project now uses Hilt for application-level dependencies, which is more robust. ViewModel dependencies are still provided manually via a custom `ViewModelProvider.Factory` in `MainActivity`—this pattern is acceptable but could be fully migrated to Hilt in the future if desired.
*   **Dual-Movable Balls:** The original concept of allowing the user to drag both a cue ball and a target ball was a failure. It created an unmanageable state, complex gesture detection, and confusing UX. The refactor to a single movable target ball with rotational aiming was the solution.
*   **Incorrect State Access:** Many bugs arose from different parts of the app trying to access state from the wrong source (e.g., a Composable reading from `overlayState` when it should have been `uiState`). It's crucial to understand which state object is for which consumer:
   *   `MainViewModel.uiState`: For high-level UI elements in Compose (`MainScreen`).
   *   `MainViewModel.overlayState`: For the detailed rendering logic within `ProtractorOverlayView`.

## Guidance for AI Development

*   **State is King:** **DO NOT** modify state directly within a `View` or `Composable`. All state changes **MUST** go through the `MainViewModel` by sending a `MainScreenEvent`. The ViewModel is the single source of truth for `OverlayState`.
*   **Understand the Data Flow:** Before making a change, trace the data flow for your feature: Event -> ViewModel -> State Change -> UseCase -> State Emission -> UI Update/Redraw.
*   **Rendering Logic Lives in Renderers:** If you need to change *how* something is drawn (e.g., add a new line, change a color scheme), the change should be in the appropriate class within `app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/`. The `PaintCache` class is the correct place to manage `Paint` objects and their styling.
*   **Gesture Logic Lives in the View:** If you need to change *how* a gesture is interpreted (e.g., add a double-tap), the change should be made in the `onTouchEvent` method of `ProtractorOverlayView.kt`.
*   **UI Controls are in Compose:** If you need to add a new button or slider, it should be a new `@Composable` function, likely placed in the `app/src/main/java/com/hereliesaz/cuedetatlite/ui/composables/` directory and integrated into `MainScreen.kt`.
*   **Aiming is Angle-Based:** To modify aiming, you should be modifying the `aimingAngleDegrees` in the `ProtractorUnit` state. Do not attempt to add a second movable ball. The position of the Ghost Ball is always *calculated* and should not be stored as independent state.