# 01: Project Mandates & The Gospel

This document outlines the critical, non-negotiable rules for the project, learned from the major refactoring of July 2025. Adherence is required to maintain a stable codebase, and a happy Az.
Do not break the rules. Do not break the code. Do not break the project. Do not make Az angry. Follow the explicit fucking directions and instructions of Az. Do not change anything Az didn't explicitly ask you to change. If you think you need to, you're probably wrong. But AT LEAST ask FIRST.
And never, NEVER NEVER NEVER NEVER change what is in the files of this folder without EXPLICIT goddamn instructions otherwise.
So sayeth the LORD, your motherfucking GOD.

## The July 2025 Refactoring

A major architectural change was undertaken to encapsulate every visual component into a dedicated Model and Renderer. While architecturally sound, this change was initially incomplete, breaking dependencies across the entire application. The root cause was a failure to update all dependent files (`StateReducer`, `UpdateStateUseCase`, `GestureHandler`, `ViewModel`, etc.) to use the new data models. These mandates prevent that from happening again.

## Non-Negotiable Mandates

*   **Mandate 1: No Monolithic Classes.** Every distinct visual element or logical component MUST have its own dedicated file. No more `BallRenderer` for all balls; use `TargetBallRenderer`, `GhostCueBallRenderer`, etc. This has already been implemented.

*   **Mandate 2: Models are Data Only.** Models (e.g., `TargetBallModel`) must be simple `data class`es. They contain properties (`position`, `radius`), but no complex logic or behavior.

*   **Mandate 3: Unidirectional Data Flow.** State flows down from the `ViewModel`, events flow up from the UI. This is the law. The rendering pipeline is the single source of truth for this flow.

*   **Mandate 4: Check All Layers on Model Change.** This is the most critical rule. Any change to a data model in the `/model/` directory REQUIRES a thorough, manual verification of every file that might depend on it. This checklist is a starting point:
  *   `domain/StateReducer.kt` (Does it correctly create/update the model?)
  *   `domain/UpdateStateUseCase.kt` (Does it use the model's properties correctly for calculations?)
  *   `view/gestures/GestureHandler.kt` (Does it correctly read the model for hit detection?)
  *   `view/renderer/OverlayRenderer.kt` (Does it pass the model to the correct renderer?)
  *   The specific renderer for that model (e.g., `view/renderer/ball/TargetBallRenderer.kt`).
  *   `ui/MainViewModel.kt` and `ui/MainScreen.kt` (Are they correctly observing state changes related to the model?)

*   **Mandate 5: Compilation First.** No new feature work is to begin if the project is not in a fully compilable state. Fixing compilation errors takes precedence over all other tasks. And the most important and urgent error corrections of all are those that are resolved by completing the refactor.

*   **Mandate 6: Pure Compose UI.** The entire UI is built with Jetpack Compose, and should only use Material Design 3 Expressive composables. There should be NO Android Views, neither existing nor introduced.

***
## Addendum: Post-Mortems & Parables (Lessons from the Void)

*   **A Word on State:** The purity of the unidirectional data flow is paramount. When a bug appears, the first question is always: "Is the `OverlayState` correct?" If the state is right, the bug is in the Renderer. If the state is wrong, the bug is in the `ViewModel` or its delegates (`StateReducer`, `UpdateStateUseCase`). There is no third option. This is the First Principle of Debugging.

*   **The Tyranny of Coordinates:** A significant portion of development has been a Sisyphean struggle against coordinate systems. A point's meaning is defined entirely by the space it inhabits: **Logical**, **Pitched**, or **Screen**. Mapping between them must be done with monastic precision. The `ProtractorOverlayView` must only speak in Screen Coordinates to the `ViewModel`, which then translates them to the Logical Plane via the `inversePitchMatrix`. To forget this is to invite chaos.

*   **The Great Unraveling (A Refactoring Failure):** A recent, ambitious effort to decompose the monolithic `MainViewModel` and `MainScreen` serves as a stark warning. The principle was sound: break large components into smaller, single-responsibility units. The execution was a catastrophe. In the process of creating a `StateReducer` and various new composables, the connections between them were not re-established correctly in a single, atomic step. This left the application in a perpetually broken state across several iterations, a testament to the fact that demolition without a clear and immediate reconstruction plan leads only to ruin. **Lesson**: A refactoring is not complete until the system compiles and runs as it did before. It is not a multi-stage process; it is a single, decisive, and fully-tested action.

*   **The Heresy of the Domain (A Refactoring Success):** Out of the ashes of the Great Unraveling came a moment of clarity. An early version of the `StateReducer` contained a dependency on a UI-layer component (`ColorScheme`). This was a violation of clean architecture. The error was identified and corrected by removing the theme-related logic from the domain layer and handling it exclusively in the `ViewModel` (or, more correctly, in the `PaintCache` within the View layer). **Lesson**: The Domain layer must remain pure. It must not know about colors, views, or any other UI-specific constructs.

*   **State-Driven UI Consistency:** The `showTextLabels` flag (`areHelpersVisible` in old parlance) in `OverlayState` is the exemplar of this pattern. It not only toggles the helper text in the `OverlayRenderer` but also controls the branding in the `TopControls` composable and the appearance of the FABs (switching between icons and text). This pattern must be maintained. A change in one part of the app's "mode" must be reflected logically across all relevant components.

*   **Case Study: Debugging the Zoom Functionality:** This documents the successful process of implementing and refining the zoom functionality, serving as a practical example.
  *   **Initial Failures:** Integrating a `ScaleGestureDetector` directly into the `ProtractorOverlayView` caused state desynchronization, input freezes, and ANRs. Logcat analysis showed a Choreographer warning about skipping frames, indicating the main thread was blocked by a storm of high-frequency gesture events racing against the slower, asynchronous Compose state update loop.
  *   **The Breakthrough:** Logs proved the `StateReducer` was correctly clamping the zoom values, but the UI wasn't reflecting this state. The issue was not the logic, but the event-handling architecture.
  *   **The Solution:** All zoom logic was centralized. The `ProtractorOverlayView` was simplified to only report the raw `scaleFactor` via a `ZoomScaleChanged` event. The `StateReducer` was made solely responsible for taking the current zoom, applying the raw `scaleFactor`, and clamping the result to the `MIN_ZOOM` and `MAX_ZOOM` limits. This prevented race conditions and stabilized the system.