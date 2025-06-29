# Cue D'état AR - Developer's Guide

## Overview

This document provides a guide to the architecture and development process for the Cue D'état AR application. This version is a complete rewrite, focusing on a robust, AR-only experience using native Android and OpenGL components, deliberately moving away from the computer-vision-based approach of the original.

The core principle of this architecture is a clean separation of concerns:
-   **State Management:** Handled exclusively by the `MainViewModel`.
-   **UI (View Layer):** Handled exclusively by Jetpack Compose composables (`MainScreen.kt`).
-   **Rendering:** Handled exclusively by the `ARCoreRenderer` and its associated OpenGL object classes.

There is no direct communication between the UI and the Renderer. All interactions flow through the ViewModel.

## Core Components

### 1. MainViewModel.kt

-   **Single Source of Truth:** The `MainUiState` data class holds the entire state of the application, from the AR session and anchors to the positions of the balls and the current UI mode.
-   **Event-Driven:** The UI sends `UiEvent` sealed class instances to the ViewModel to signify user actions (e.g., `OnTap`, `OnReset`).
-   **State Reducer:** The `onEvent` function acts as a reducer, taking the current state and an event, and producing a new state. It contains all the application's business logic (e.g., determining which ball to place, constraining coordinates, checking for impossible shots).

### 2. MainScreen.kt

-   **Purely Declarative UI:** This file contains only Jetpack Compose code. It observes the `uiState` StateFlow from the ViewModel.
-   **Gesture Handling:** It uses a transparent `ARScene` composable layered over the `GLSurfaceView` to capture gestures. `rememberTapGestureRecognizer` and `rememberDragGestureRecognizer` translate user input into `UiEvent`s for the ViewModel.
-   **State Propagation to Renderer:** The `AndroidView` that hosts our `GLSurfaceView` has an `update` block. This block is called on every recomposition, passing the latest `uiState` values as public properties to the `ARCoreRenderer`. This is the one-way data flow from UI State -> Renderer.

### 3. ARCoreRenderer.kt

-   **Stateless Rendering Engine:** The renderer is designed to be "dumb." It holds no state of its own. On every `onDrawFrame` call, it reads its public properties (like `tableAnchor`, `cueBallLocalPosition`, etc.) and draws the scene based on those values.
-   **OpenGL Object Management:** It owns instances of our 3D object classes (`Table`, `Ball`, `Line`, etc.).
-   **Rendering Logic:** It contains all the complex rendering logic, including matrix math for positioning objects, and the shot calculation functions (`drawCutShotVisualization`, `drawBankShotVisualization`).

### 4. AR Object Classes (`ar/objects/`)

-   **Self-Contained:** Each class (`Table.kt`, `Ball.kt`, etc.) is responsible for its own OpenGL setup (loading shaders, creating vertex buffers) and its own `draw()` call. They are simple, reusable components.

## How to Extend the App

This architecture is designed for easy extension.

### Example: Adding a Third Ball

1.  **Update State:** In `MainViewModel.kt`, add a new property to `MainUiState`:
    ```kotlin
    val thirdBallLocalPosition: Position? = null,
    ```
2.  **Update Logic:** In `MainViewModel.kt`, update the `handleTap` logic to place the third ball after the object ball is placed.
3.  **Update Renderer:**
    *   In `ARCoreRenderer.kt`, create a new `Ball` instance: `private lateinit var thirdBall: Ball`.
    *   Instantiate it in `onSurfaceCreated`: `thirdBall = Ball(context, color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)) // Red`.
    *   Add a public property: `var thirdBallLocalPosition: Position? = null`.
    *   In `onDrawFrame`, add the draw call: `if (thirdBallLocalPosition != null) drawBall(thirdBall, thirdBallLocalPosition, tableModelMatrix)`.
4.  **Update UI:** In `MainScreen.kt`'s `AndroidView` `update` block, pass the new state to the renderer:
    ```kotlin
    update = {
        //...
        renderer.thirdBallLocalPosition = uiState.thirdBallLocalPosition
    }
    ```

This clean separation ensures that adding new visual elements requires minimal changes across the codebase and follows a predictable pattern.