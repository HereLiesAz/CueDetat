# Cue D'état AR - Developer's Guide

## Overview

This document provides a guide to the architecture and development process for the Cue D'état AR application. This version is a complete rewrite, focusing on a robust, AR-only experience using native Android and OpenGL components, deliberately moving away from the computer-vision-based approach of the original.

The core principle of this architecture is a clean separation of concerns:
-   **State Management:** Handled exclusively by the `MainViewModel`.
-   **UI (View Layer):** Handled exclusively by Jetpack Compose composables (`MainScreen.kt`).
-   **Rendering:** Handled exclusively by the `sceneview` declarative AR library.

There is no direct communication between the UI and any manual rendering pipeline. All interactions flow through the ViewModel, and the UI is a direct representation of the state.

### Development Mandates
**It is mandatory to use the modern Android XR and Jetpack XR (`sceneview`) libraries wherever available.** Manual OpenGL implementations are to be avoided in favor of the declarative, component-based architecture provided by `sceneview`. This ensures maintainability, leverages the latest platform features, and keeps the project aligned with modern Android development practices.

## Core Components

### 1. MainViewModel.kt

-   **Single Source of Truth:** The `MainUiState` data class holds the entire state of the application, from the AR session and anchors to the positions of the balls and the current UI mode.
-   **Event-Driven:** The UI sends `UiEvent` sealed class instances to the ViewModel to signify user actions (e.g., `OnTap`, `OnReset`).
-   **State Reducer:** The `onEvent` function acts as a reducer, taking the current state and an event, and producing a new state. It contains all the application's business logic (e.g., determining which ball to place, constraining coordinates, checking for impossible shots).

### 2. MainScreen.kt

-   **Purely Declarative UI:** This file contains only Jetpack Compose code. It observes the `uiState` StateFlow from the ViewModel.
-   **AR Scene Definition:** The `ARScene` composable is the root of the AR experience. It declaratively defines the scene's nodes (`TableNode`, `BallNode`, etc.) based on the current `uiState`.
-   **Gesture Handling:** It uses `sceneview`'s built-in gesture recognizers on the scene and its nodes to translate user input into `UiEvent`s for the ViewModel.

### 3. AR Node Classes (`ar/rendering/`)

-   **Self-Contained Composable Nodes:** Each class (`TableNode.kt`, `BallNode.kt`, etc.) is a self-contained `@Composable` function that represents a physical object in the scene. They are simple, reusable components within the `ARScene`.

## How to Extend the App

This architecture is designed for easy extension.

### Example: Adding a Third Ball

1.  **Update State:** In `MainViewModel.kt`, add a new property to `MainUiState`:
    ```kotlin
    val thirdBallPose: BallState? = null,
    ```
2.  **Update Logic:** In `MainViewModel.kt`, update the `handlePlaneTap` logic to place the third ball after the object ball is placed.
3.  **Update UI:** In `MainScreen.kt`, within the `ARScene` composable, add a new `BallNode` that is rendered when `uiState.thirdBallPose` is not null:
    ```kotlin
    uiState.thirdBallPose?.let {
        BallNode(
            id = 2,
            pose = it.pose,
            isSelected = uiState.selectedBall == 2,
            onBallTapped = { id -> onEvent(UiEvent.OnBallTapped(id)) },
            color = Color.Blue // Or some other color
        )
    }
    ```

This clean separation ensures that adding new visual elements requires minimal changes across the codebase and follows a predictable, declarative pattern.
