# Cue D'état AR - Developer's Guide

## Overview

This document provides a guide to the architecture and development process for the Cue D'état AR application. This version is a complete rewrite, focusing on a robust, AR-only experience using native Android and OpenGL components, deliberately moving away from the computer-vision-based approach of the original.

The core principle of this architecture is a clean separation of concerns:
-   **State Management:** Handled exclusively by the `MainViewModel`.
-   **UI (View Layer):** Handled exclusively by Jetpack Compose composables (`MainScreen.kt`).
-   **Rendering:** Handled by a manual OpenGL pipeline within a custom `ARCoreRenderer` class, which is hosted in a `GLSurfaceView`.

There is no direct communication between the UI and the Renderer outside of passing state. All interactions flow through the ViewModel.

### Development Mandates
**It is mandatory to use the official Android XR SDK, ARCore for Jetpack XR SDK, and OpenXR to develop this app. Do NOT confuse Google's ARcore with Android XR or Arcore for Jetpack XR** This ensures full control over the rendering pipeline and avoids third-party scene graph libraries like `sceneview` or deprecated libraries like Sceneform.

## Core Components

### 1. MainViewModel.kt

-   **Single Source of Truth:** The `MainUiState` data class holds the entire state of the application, from the AR session and anchors to the positions of the balls and the current UI mode.
-   **Event-Driven:** The UI sends `UiEvent` sealed class instances to the ViewModel to signify user actions (e.g., `OnTap`, `OnReset`).
-   **State Reducer:** The `onEvent` function acts as a reducer, taking the current state and an event, and producing a new state. It contains all the application's business logic.

### 2. MainScreen.kt

-   **Purely Declarative UI:** This file contains only Jetpack Compose code for the 2D UI elements.
-   **AR View Hosting:** The AR view itself is a classic Android `GLSurfaceView`, which is embedded within the Compose UI using the `AndroidView` composable.
-   **State Propagation:** The `update` block of the `AndroidView` is responsible for passing the latest `UiState` from the Compose world to the public properties of the `ARCoreRenderer` instance on every state change.
-   **Lifecycle Management:** It manages the ARCore `Session` lifecycle, linking it to the Composable's lifecycle events.

### 3. ARCoreRenderer.kt

-   **Manual Rendering Engine:** This class implements `GLSurfaceView.Renderer` and contains all the OpenGL code necessary to draw the scene.
-   **Stateless Rendering:** The renderer is designed to be "dumb." On every `onDrawFrame` call, it reads its public state properties (which are updated by `MainScreen`) and draws the table, balls, and lines based on those values. It holds no business logic.
-   **Event Handling:** It detects raw touch events and uses a callback to notify the `MainScreen`, which then translates them into `UiEvent`s for the ViewModel.

### 4. Renderable Classes (`ar/renderables/`)

-   **Self-Contained OpenGL Objects:** Each class (`Table.kt`, `Ball.kt`, etc.) is responsible for its own OpenGL setup (loading shaders, creating vertex buffers) and its own `draw()` call. They are simple, reusable components within the `ARCoreRenderer`.

## How to Extend the App

This architecture is designed for easy extension.

### Example: Adding a Third Ball

1.  **Update State:** In `MainViewModel.kt`, add a new property to `MainUiState`:
    ```kotlin
    val thirdBallPose: BallState? = null,
    ```
2.  **Update Logic:** In `MainViewModel.kt`, update the `handlePlaneTap` logic to place the third ball.
3.  **Update Renderer:**
    * In `ARCoreRenderer.kt`, create a new `Ball` instance: `private lateinit var thirdBall: Ball`.
    * Instantiate it in `onSurfaceCreated`: `thirdBall = Ball(context, color = floatArrayOf(0.2f, 0.2f, 0.8f, 1.0f)) // Blue`.
    * Add a public property to receive the state: `var thirdBallState: BallState? = null`.
    * In `onDrawFrame`, add the logic to draw the third ball if its state is not null.
4.  **Update UI:** In `MainScreen.kt`'s `AndroidView` `update` block, pass the new state to the renderer:
    ```kotlin
    update = { view ->
        //...
        (view.renderer as? ARCoreRenderer)?.thirdBallState = uiState.thirdBallPose
    }
    ```

This clean separation ensures that adding new visual elements requires minimal changes across the codebase and follows a predictable pattern.
