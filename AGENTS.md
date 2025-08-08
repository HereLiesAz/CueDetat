# Agent Instructions for Cue d'Etat

This document provides high-level guidelines for working on the Cue d'Etat codebase.

## Project Persona

The application's persona is a cynical, witty, and technically precise physics expert. All user-facing text, especially warnings and tutorial messages, must adhere to this tone.

- **Voice**: Darkly humorous, slightly condescending, and sharp. Avoid pleasantries.
- **Content**: Use technically correct terminology but with a flippant, dismissive tone.

## Core Architecture (MVI)

This project follows a strict Model-View-Intent (MVI) architecture with a Unidirectional Data Flow (UDF).

- **`View`**: Renders state and forwards user events to the `ViewModel`. Contains no business logic.
- **`ViewModel`**: Orchestrates data flow. Receives events, passes them to the `StateReducer`, and exposes the final, updated state.
- **`StateReducer`**: The only component that can modify state. It's a pure function that takes the current state and an event and produces a new state.
- **`UseCase`**: Contains complex business logic and calculations. It is called by the `ViewModel` after the state has been reduced to calculate derived properties. **UseCases do not modify state directly.**
- **`State` (`CueDetatState`)**: A single, immutable data class representing the entire application state.

**Data Flow:**
1.  `View` sends an `Event` to the `ViewModel`.
2.  `ViewModel` sends the `Event` to the `StateReducer`.
3.  `StateReducer` creates a new `State`.
4.  `ViewModel` runs `UseCases` on the new `State` to calculate derived data.
5.  `ViewModel` emits the final `State` to the `View`.
6.  `View` re-renders.

## Architectural Mandates

1.  **Unidirectional Data Flow**: State flows down, events flow up. No exceptions.
2.  **ViewModel as Orchestrator**: The ViewModel contains no complex business logic. It only directs the flow of data.
3.  **Single Responsibility**: Each class has one well-defined purpose.
4.  **Holistic Model Updates**: When changing a data model, you must review and update all consuming components.
5.  **Atomic Refactoring**: Architectural changes must be applied across the entire relevant codebase at once.
6.  **Absolute Constraints**: Logical constraints (e.g., a ball must be on the table) must be enforced at every state transition.
