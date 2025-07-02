# Developer's Guide: Cue D'état Lite

This document provides essential context for maintaining and developing the Cue D'état Lite Android application. It is intended for both human developers and AI assistants.

## Core Concepts & Architecture

The application's primary function is to overlay aiming graphics on a live camera feed of a pool table. The architecture is a blend of traditional Android Views (for camera and custom drawing) and Jetpack Compose (for the UI and state management), all orchestrated by a central `MainViewModel`.


CueDetatLite
+-- .git
|  +-- (files omitted for brevity)
+-- .github
|  \-- workflows
|     +-- blank.yml
|     \-- nextjs.yml
+-- .gradle
|  +-- (files omitted for brevity)
+-- .idea
|  +-- (files omitted for brevity)
+-- .kotlin
|  +-- (files omitted for brevity)
+-- app
|  +-- build
|  |  +-- (files omitted for brevity)
|  +-- release
|  |  +-- (files omitted for brevity)
|  +-- src
|  |  \-- main
|  |     +-- java
|  |     |  \-- com
|  |     |     \-- hereliesaz
|  |     |        \-- cuedetatlite
|  |     |           +-- data
|  |     |           |  \-- SensorRepository.kt
|  |     |           +-- domain
|  |     |           |  +-- StateReducer.kt
|  |     |           |  +-- UpdateStateUseCase.kt
|  |     |           |  +-- WarningManager.kt
|  |     |           |  \-- WarningText.kt
|  |     |           +-- ui
|  |     |           |  +-- composables
|  |     |           |  |  +-- ActionFabs.kt
|  |     |           |  |  +-- CameraBackground.kt
|  |     |           |  |  +-- KineticWarning.kt
|  |     |           |  |  +-- LuminanceDialog.kt  <-- NEW
|  |     |           |  |  +-- MenuDrawer.kt
|  |     |           |  |  +-- TopControls.kt
|  |     |           |  |  \-- ZoomControls.kt
|  |     |           |  +-- theme
|  |     |           |  |  +-- Color.kt
|  |     |           |  |  +-- Shape.kt
|  |     |           |  |  +-- Theme.kt
|  |     |           |  |  \-- Type.kt
|  |     |           |  +-- MainScreen.kt
|  |     |           |  +-- MainScreenEvent.kt
|  |     |           |  +-- MainViewModel.kt
|  |     |           |  +-- MenuAction.kt
|  |     |           |  +-- UiEvents.kt
|  |     |           |  +-- VerticalSlider.kt
|  |     |           |  \-- ZoomMapping.kt
|  |     |           +-- utils
|  |     |           |  +-- SingleEvent.kt
|  |     |           |  \-- ToastMessage.kt
|  |     |           +-- view
|  |     |           |  +-- gestures
|  |     |           |  |  \-- GestureHandler.kt  <-- NEW
|  |     |           |  +-- model
|  |     |           |  |  +-- ActualCueBall.kt
|  |     |           |  |  +-- ILogicalBall.kt  <-- RENAMED
|  |     |           |  |  +-- Perspective.kt
|  |     |           |  |  +-- ProtractorUnit.kt
|  |     |           |  |  \-- TableModel.kt
|  |     |           |  +-- renderer
|  |     |           |  |  +-- text
|  |     |           |  |  |  +-- BallTextRenderer.kt
|  |     |           |  |  |  \-- LineTextRenderer.kt
|  |     |           |  |  +-- util
|  |     |           |  |  |  \-- DrawingUtils.kt
|  |     |           |  |  +-- BallRenderer.kt
|  |     |           |  |  +-- LineRenderer.kt
|  |     |           |  |  +-- OverlayRenderer.kt
|  |     |           |  |  +-- RailRenderer.kt
|  |     |           |  |  \-- TableRenderer.kt
|  |     |           |  +-- state
|  |     |           |  |  +-- OverlayState.kt
|  |     |           |  |  \-- ScreenState.kt
|  |     |           |  +-- PaintCache.kt
|  |     |           |  \-- ProtractorOverlayView.kt
|  |     |           +-- MainActivity.kt
|  |     |           \-- MyApplication.kt
|  |     +-- res
|  |     |  +-- (files omitted for brevity)
|  |     +-- AndroidManifest.xml
|  |     \-- ic_launcher-playstore.png
|  +-- .gitignore
|  +-- build.gradle.kts
|  \-- proguard-rules.pro
+-- build
|  +-- (files omitted for brevity)
+-- gradle
|  +-- wrapper
|  |  +-- gradle-wrapper.jar
|  |  \-- gradle-wrapper.properties
|  \-- libs.versions.toml
+-- .gitignore
+-- backup_for_ai.ps1
+-- build.gradle.kts
+-- Developers_Guide.md
+-- gradle.properties
+-- gradlew
+-- gradlew.bat
+-- LICENSE
+-- local.properties
+-- (old project context files)
+-- README.md
\-- settings.gradle.kts


### Key Components

1.  **`MainViewModel`**: The central brain of the application. It holds the canonical state, processes user and sensor events, and orchestrates updates between the UI and the rendering logic.
2.  **`OverlayState`**: A comprehensive data class holding *all* state information for the UI and rendering, including screen dimensions, sensor data, zoom levels, user settings, and the state of the objects on the screen.
3.  **`ScreenState`**: A nested data class within `OverlayState` that specifically holds the state of the logical objects to be rendered (e.g., the target ball, aiming angle).
4.  **`ProtractorOverlayView`**: A custom Android `View` responsible for all the graphical rendering (balls, lines, etc.). It is placed on top of the camera preview. It receives the `OverlayState` from the ViewModel and uses a set of `renderer` classes to draw on its `Canvas`.
5.  **`GestureHandler`**: A dedicated class that encapsulates all touch and gesture detection logic (drags, pinches). It is initialized by and receives events from `ProtractorOverlayView`, processes them, and sends the appropriate `MainScreenEvent` to the ViewModel.
6.  **`UpdateStateUseCase`**: A domain-layer class responsible for taking the current `OverlayState` and calculating derived values, most importantly the perspective matrices for rendering based on sensor data (pitch/roll) and zoom level.
7.  **`StateReducer`**: A domain-layer class responsible for handling discrete state changes, such as moving the target ball or resetting the view. It ensures state transitions are predictable.
8.  **Jetpack Compose UI (`MainScreen.kt`, etc.)**: All UI controls (buttons, sliders, menus) are built with Compose. They read state from the `MainViewModel` and send `MainScreenEvent`s back to it to signal user actions.

### Data Flow & Rendering Loop

1.  **Event Occurs**: A user drags a finger (`GestureHandler`), moves a slider (`ZoomControls`), or a new sensor reading arrives (`SensorRepository`).
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

*   **State Unification:** State has been successfully centralized into the `OverlayState` and `ScreenState` data classes, managed exclusively by the `MainViewModel`. This is a stable pattern that should be maintained.
*   **UI Migration to Compose:** The entire UI layer has been successfully migrated to Jetpack Compose.
*   **Gesture Logic Abstraction:** Touch and gesture logic was successfully extracted from `ProtractorOverlayView` into a dedicated `GestureHandler` class. This cleans up the View and isolates responsibilities.
*   **Simplified Event Handling:** `ProtractorOverlayView` now uses a single `onEvent` listener, which simplifies its integration with Compose and the ViewModel.
*   **Stable Aiming Mechanic:** The pivot to a target-ball-centric, angle-based aiming system was a major success.

## Failures & Lessons Learned

*   **Manual Dependency Injection:** The initial project used manual DI within the `MyApplication` class. This was brittle and has been mostly replaced by Hilt and a custom ViewModel factory.
*   **Dual-Movable Balls:** The original concept of allowing the user to drag both a cue ball and a target ball was a failure. The current single-movable-target system is the correct approach.
*   **Build & Dependency Issues:** The project has been sensitive to dependency conflicts, particularly with CameraX and Guava. See the troubleshooting section below.
*   **Incorrect State Access:** Bugs arose from different parts of the app trying to access state from the wrong source (e.g., a Composable reading from `overlayState` when it should have been `uiState`).
    *   `MainViewModel.uiState`: For high-level UI elements in Compose (`MainScreen`).
    *   `MainViewModel.overlayState`: For the detailed rendering logic within `ProtractorOverlayView`.

## Troubleshooting

### `Cannot access class 'ListenableFuture'` Build Error

This has been a recurring and persistent issue. It indicates a classpath conflict with the Guava library, which CameraX depends on.

**Solution Steps:**

1.  **Explicit Dependency:** Ensure the `build.gradle.kts` file for the `:app` module has an explicit dependency on both `guava` and `listenablefuture`. Refer to the `libs.versions.toml` and `app/build.gradle.kts` files in the project for the correct implementation.
2.  **Robust CameraX Initialization:** The `CameraBackground.kt` composable has been refactored to use a standard `LaunchedEffect` and `suspendCoroutine` pattern. This seems to be more robust for the Gradle build system than the previous `addListener` pattern. **Do not revert to the old pattern.**
3.  **Invalidate Caches:** If the error persists after verifying the dependencies, the final step is to clear Gradle's caches. In Android Studio, go to **File > Invalidate Caches / Restart...** and select **Invalidate and Restart**. This is often necessary to force Gradle to re-evaluate the now-correct dependency graph.

## Guidance for AI Development

*   **State is King:** **DO NOT** modify state directly within a `View` or `Composable`. All state changes **MUST** go through the `MainViewModel` by sending a `MainScreenEvent`. The ViewModel is the single source of truth for `OverlayState`.
*   **Understand the Data Flow:** Before making a change, trace the data flow for your feature: Event -> ViewModel -> State Change -> UseCase -> State Emission -> UI Update/Redraw.
*   **Rendering Logic Lives in Renderers:** To change *how* something is drawn (e.g., add a new line, change a color scheme), modify the appropriate class in `app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/`. The `PaintCache` class manages `Paint` objects and styling.
*   **Gesture Logic Lives in `GestureHandler`:** To change *how* a gesture is interpreted (e.g., add a double-tap), the change should be made in `app/src/main/java/com/hereliesaz/cuedetatlite/view/gestures/GestureHandler.kt`.
*   **UI Controls are in Compose:** To add a new UI element, create a new `@Composable` function in the `ui/composables` directory and integrate it into `MainScreen.kt`.
*   **Aiming is Angle-Based:** To modify aiming, you should be modifying the `aimingAngleDegrees` in the `ProtractorUnit` state. Do not attempt to add a second movable ball. The position of the Ghost Ball is always *calculated* and should not be stored as independent state.