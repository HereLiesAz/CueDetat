# Data Layer

This document provides a reference to the documentation for the data layer of the Cue d'Etat project.

## **Architecture**

The application follows a standard MVI (Model-View-Intent) architecture with a clear separation of concerns. The data layer is responsible for abstracting the origin of the application's data (e.g., local storage, remote server).

For a complete overview of the architecture, please see the **[01 Architecture](./01_Architecture/INDEX.md)** directory.

## **Key Components**

*   **Repositories:** The primary entry point to the data layer. Repositories are responsible for coordinating data from different sources.
*   **Data Sources:** The concrete implementations that fetch data (e.g., `DataStore` for user preferences).

All data flows are managed using Kotlin Coroutines and Flow to ensure a non-blocking, reactive data stream to the upper layers of the application.
