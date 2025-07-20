# 1.1. Core Architectural Mandates

These are the fundamental rules governing the application's structure. Violating these principles
will compromise the stability and maintainability of the codebase.

* **Mandate 1: Unidirectional Data Flow (UDF)**
  State must only flow down from state holders to the UI. Events must only flow up from the UI to be
  processed. There are no exceptions.

* **Mandate 2: ViewModel as Orchestrator**
  The ViewModel's role is to orchestrate data flow. It receives UI events, forwards them to the
  appropriate processor (e.g., a State Reducer), and exposes the resulting state. It contains no
  complex business logic or mutable state of its own.

* **Mandate 3: Single Responsibility Principle**
  Each class must have a single, well-defined purpose. Reducers reduce state, UseCases perform
  complex calculations, and Renderers draw the UI. A class that attempts multiple roles must be
  refactored.

* **Mandate 4: Holistic Model Updates**
  Any change to a data model (e.g., in `OverlayState`) requires a thorough review of all components
  that consume it (Reducers, UseCases, Renderers). Failure to account for the downstream impact of a
  model change is a primary source of bugs.

* **Mandate 5: Atomic Refactoring**
  Refactoring that introduces a new architectural pattern must be applied across the entire relevant
  codebase at once. Leaving parts of the code in an old pattern while others use a new one creates
  an unstable, inconsistent state that is difficult to debug and maintain.

* **Mandate 6: Absolute Constraints**
  A logical constraint (e.g., a ball must remain within the table's boundaries) must be enforced at
  every possible state transition. It is not sufficient to enforce it only during user-driven
  gestures; it must also be re-verified after events like screen resizing or table visibility
  changes.