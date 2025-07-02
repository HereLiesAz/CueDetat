# Project Development Guide: Cue D'état
This document outlines the core architecture, concepts, and future direction of the Cue D'état
application. It serves as a single source of truth to prevent regressions and ensure consistent
development. Consider it a note-to-self for the AI working on this project, and keep it updated
accordingly with ANYTHING that will be useful to the next AI in the next chat.

NEVER change what is written here, only add to it. Always include anything that you note to yourself as a matter of clarification.

## 1. Core Concepts & Official Terminology
A precise vocabulary is critical. The following terms are to be used exclusively.

*   **Logical Plane:** An abstract, infinite 2D coordinate system (like graph paper) where all aiming
    geometry is defined and calculated. This is the "world" of the simulation. The origin (0,0) of
    this plane is conceptually at the top-left, but pivot points for transformations are usually
    the center of the view (`viewWidth/2, viewHeight/2` in logical units).
*   **Screen Plane:** The physical 2D plane of the device's screen. This is the "window" through
    which the user views the Logical Plane.
*   **Perspective Transformation:** The process, primarily handled by a `pitchMatrix` (and
    `railPitchMatrix` for lifted elements), of projecting the Logical Plane onto the Screen Plane
    to create the 3D illusion. Crucially, this transformation must always pivot around the absolute
    center of the view (logical coordinates `viewWidth/2, viewHeight/2` map to screen coordinates
    `viewWidth/2, viewHeight/2` as the pivot).
*   **Global Zoom:** A single zoom factor, controlled by `zoomSliderPosition` and `ZoomMapping.kt`,
    that determines the base logical radius for all interactive elements.
*   **On-Screen Elements:**

    *   **Protractor Unit (Protractor Mode Only):** The primary aiming apparatus for cut shots.
        *   **Target Ball (Protractor):** The logical and visual center of the `ProtractorUnit`. Its
            logical position is user-draggable. Its logical radius is set by Global Zoom.
        *   **Ghost Cue Ball (Protractor):** The second ball in the `ProtractorUnit`. Its *absolute
            logical position* is calculated based on the Target Ball's center, the unit's radius, and
            `state.protractorUnit.aimingAngleDegrees`. Its logical radius is the same as the Target
            Ball's. Both Target and Ghost Cue Ball have a 2D logical representation (drawn by
            `BallRenderer.drawLogicalBalls`) and a 3D screen-space "ghost" effect (drawn by
            `BallRenderer.drawScreenSpaceGhosts` with lift).
    *   **ActualCueBall:** A user-draggable logical ball.
        *   **In Protractor Mode (Optional):** Can be toggled by the user. Used for visualizing shots
            originating from a specific point. Rendered with a "lifted" 3D ghost effect.
            Its logical radius is set by Global Zoom.
        *   **In Banking Mode (Mandatory, becomes the "Banking Ball"):** Always visible and represents
            the
            cue ball on the table. Its logical radius is set by Global Zoom. Rendered *on* the table
            plane.
    *   **Table Visuals (Banking Mode Only):** Wireframe representation of the pool table surface,
        rails,
        pockets, and diamonds.
        *   Logically anchored at the view's center.
        *   Its logical scale is determined by `tableToBallRatio * ActualCueBall.radius`.
        *   The table surface is rendered on the `pitchMatrix`. Rails are rendered on the
            `railPitchMatrix`.
    *   **Lines:**
        *   **Protractor Shot Line (Protractor Mode):** From `ActualCueBall.center` (if visible, else
            a default
            screen anchor's logical projection) through the `ProtractorUnit.GhostCueBall`'s absolute
            logical center, extending to infinity. Drawn in absolute logical coordinates.
        *   **Aiming Line (Protractor Mode):** From `ProtractorUnit.GhostCueBall`'s center
            through `ProtractorUnit.TargetBall`'s center, extending to infinity.
        *   **Tangent Lines & Angle Lines (Protractor Mode):** Originate from the Ghost Cue Ball's center. The tangent line indicates the cue ball's path post-collision, with one half becoming solid to show the direction. Angle lines show standard cut angles.
*   **Gestures:** All gestures are handled by the `GestureHandler` class.
    *   **Pinch-to-Zoom:** Controls the Global Zoom.
    *   **Single-finger drag:** Behavior is contextual. On a ball, it moves that ball. Off a ball, it controls the `aimingAngleDegrees` of the protractor using a continuous rotational input.
    *   **Tap:** Not currently implemented for specific actions, but recognized.

## 2. Architectural Model & File Structure

The architecture strictly separates data, domain logic, and UI presentation following an MVI pattern.

`com/hereliesaz/cuedetatlite/`
├── `data/`
│ ├── `SensorRepository.kt` // Provides sensor data as a Flow.
├── `di/`
│ └── `AppModule.kt` // Hilt module for providing dependencies.
├── `domain/`
│ ├── `StateReducer.kt` // Pure function: (Current State, Event) -> New State.
│ ├── `UpdateStateUseCase.kt` // Pure function: Calculates derived state (matrices, radii, impossible shots).
│ └── `WarningManager.kt` // Manages warning logic.
├── `ui/`
│ ├── `composables/` // Self-contained, reusable UI pieces (Buttons, Sliders, Dialogs).
│ ├── `theme/` // Material 3 Theme, Colors, Typography, Shapes.
│ ├── `MainViewModel.kt` // Orchestrates events, manages side effects, holds state.
│ ├── `MainScreen.kt` // The root Composable that builds the entire UI.
│ └── `MainScreenEvent.kt` // Sealed class defining all user and system events.
└── `view/`
├── `gestures/`
│ └── `GestureHandler.kt` // Encapsulates all MotionEvent logic.
├── `model/` // Data classes representing logical objects.
├── `renderer/` // Classes responsible for drawing on the Canvas.
├── `state/` // Data classes for UI state (`OverlayState`, `ScreenState`).
└── `ProtractorOverlayView.kt` // Custom Android View, embedded in Compose, that handles all Canvas drawing.

**The Golden Rule**: ViewModel orchestrates. StateReducer computes primary state. UpdateStateUseCase computes derived state. Renderers display.

## 3. Rendering Pipeline (Conceptual)

1.  **Event (UI -> ViewModel):** A user interaction (e.g., drag) occurs in a Composable or `ProtractorOverlayView`. An `MainScreenEvent` is sent to the `MainViewModel`.
2.  **Orchestration (ViewModel):**
    *   For screen-space events (like `BallMoved`), the ViewModel uses the `inversePitchMatrix` from the current state to convert screen coordinates to logical coordinates.
    *   It then dispatches a new event with these logical coordinates.
    *   It handles side-effects like showing dialogs or triggering warning text timers.
3.  **Reduction (ViewModel -> StateReducer):** The ViewModel sends the event to the `StateReducer`. The reducer takes the current `ScreenState` and the event, and returns a new, updated `ScreenState` without any side effects.
4.  **Derivation (ViewModel -> UpdateStateUseCase):** The ViewModel takes the new `ScreenState` and the current `OverlayState`, and passes them to the `UpdateStateUseCase`. This use case calculates all derived data:
    *   Calculates the definitive logical radius for all balls based on the `zoomSliderPosition`.
    *   Calculates the `pitchMatrix`, `railPitchMatrix`, and their inverse.
    *   Determines the `isImpossibleShot` flag.
    *   It returns a fully updated `OverlayState`.
5.  **State Update (ViewModel -> UI):** The ViewModel updates its `_overlayState` Flow with the new state from the use case.
6.  **Render (UI):**
    *   The `MainScreen` and its child Composables recompose based on the new state.
    *   The `AndroidView` containing `ProtractorOverlayView` is updated. Its `update` block calls `view.updateState(newState, systemIsDark)`.
    *   `ProtractorOverlayView.onDraw` is triggered. It calls `OverlayRenderer`, which orchestrates the various sub-renderers (`BallRenderer`, `LineRenderer`, etc.) to draw the scene onto the `Canvas` using the matrices and logical data from the `OverlayState`.

## 4. Core Operational Modes & Entity Behavior

The application operates in two distinct modes: Protractor Mode and Banking Mode.

### 4.1. Protractor Mode

*   **`ProtractorUnit`**: The primary aiming tool. Its `TargetBall` is user-draggable. Its `GhostCueBall`'s position is calculated based on the `aimingAngleDegrees`.
*   **`ActualCueBall` (Optional)**: A user-draggable ball for visualizing specific shot origins.
*   **Lines**: `AimingLine`, `ShotLine`, `TangentLines`, and `AngleLines` are all visible and interactive.

### 4.2. Banking Mode (`isBankingMode = true`)

*   **`ActualCueBall` (as "Banking Ball")**: The primary interactive element. User can drag it on the table.
*   **Table Visuals**: A logically scaled table with rails and pockets is drawn.
*   **`bankingAimTarget`**: A logical point set by a user drag, determining the initial direction of the banking shot path.
*   **Protractor Unit and its lines are NOT drawn.**

## 5. Key Implementation Learnings & Mandates

*   **Unidirectional Data Flow**: State flows down, events flow up. This is non-negotiable.
*   **Coordinate Systems**: `GestureHandler` sends screen coords; `ViewModel` converts to logical using `inversePitchMatrix`; `StateReducer` works only with logical; `Renderers` use `pitchMatrix` to project logical to screen.
*   **Single Source of Truth for Radius:** The `zoomSliderPosition` is the ultimate source of truth for zoom. `UpdateStateUseCase` is the *only* place where this slider position is converted into a definitive logical radius, which is then applied to all balls in the `ScreenState` for that frame. The `StateReducer` no longer calculates radii.
*   **Gesture Handling (`GestureHandler.kt`):** This class encapsulates all `MotionEvent` logic. For rotation, it calculates the *change in angle* between move events relative to the protractor's center to provide a smooth, continuous "dial" interaction, rather than simply pointing at the user's finger.
*   **Warning Logic:** The `MainViewModel` manages the display of warnings. It listens for `GestureEnded` events. If, at that moment, the state's `isImpossibleShot` flag is true, it selects the next warning from the resource array and updates the UI state. The warning is cleared when a new gesture begins.
*   **Pure Compose UI:** The entire UI is built with Jetpack Compose. The `activity_main.xml` file is obsolete and has been removed. The `ProtractorOverlayView` is the only remaining Android `View`, and it is embedded via the `AndroidView` composable. The `VerticalSlider` is a custom-built Composable to meet the design requirements.

## 6. Future Development Plan

*   **Bank/Kick Shot Calculator (Refinement):**
    *   Improve reflection logic in `LineRenderer` for more than 2 banks.
    *   Consider pocket geometry for line termination/success.
*   **Object/Table Detection (Computer Vision):**
    *   Use OpenCV or ML Kit to detect table boundaries and ball positions.
    *   Project screen coordinates to Logical Plane to auto-place `ActualCueBall`.
*   **"English" / Spin Visualization:** Add UI controls to simulate sidespin, altering tangent lines
    or shot paths.
*   **Tutorial Enhancements:** Make the tutorial more interactive, highlighting UI elements
    corresponding to the current step.