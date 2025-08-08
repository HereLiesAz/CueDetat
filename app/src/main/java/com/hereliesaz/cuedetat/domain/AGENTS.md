# Agent Instructions for the Domain Layer

This document provides guidelines for working with the `domain` layer of the application, which is responsible for state management and business logic.

## State Management (`CueDetatState`)

The `CueDetatState` data class, defined in `UiModel.kt`, is the **single, immutable source of truth** for the entire application.

### Key Principles

- **Immutability**: `CueDetatState` is a `data class` and must be treated as immutable. To modify state, you must create a new instance of the class using the `copy()` method. Direct mutation of state properties is forbidden.
- **Single Source of Truth**: All data required to render the UI must be derived from `CueDetatState`. Do not introduce alternative state-holding mechanisms.

### State Categories

The properties within `CueDetatState` can be grouped into several categories:

- **Core Logical Model**: Represents the primary objects on the virtual pool table (e.g., `protractorUnit`, `onPlaneBall`, `table`).
- **UI & Mode Controls**: Flags and enums that control the application's UI and operational mode (e.g., `isBankingMode`, `isCameraVisible`, `orientationLock`).
- **Sensor & Perspective Data**: Raw data from device sensors (`currentOrientation`) and the calculated matrices used for 3D projection (`pitchMatrix`).
- **Derived State**: Properties calculated by a `UseCase` based on the core state (e.g., `isGeometricallyImpossible`, `aimedPocketIndex`). These are marked with `@Transient` and are not persisted.

### Modifying State

- **State Reducers**: All state modifications must be performed within a `StateReducer`. A reducer is a pure function that takes the current state and an event and returns a new state.
- **Justification**: Any additions to `CueDetatState` must be clearly justified to avoid state pollution. New properties should have a singular, well-defined purpose.
