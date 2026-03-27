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
  Any change to a data model (e.g., in `CueDetatState`) requires a thorough review of all components
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

* **Mandate 7: ONE SINGLE USER INTERACTION for AR Setup**
  The AR setup flow must strictly adhere to a single interaction paradigm. There is NO multi-step
  wizard. The app must NEVER attempt to see the whole table at once, identify pockets
  automatically, or balls automatically during AR setup. The sole interaction required from the
  user is capturing the felt color using a magnifying circle and a capture button (styled like a
  camera app). This action must immediately drop the user into an active AR Tracking state. Any
  fine-tuning of pocket alignment relies entirely on the user rotating/zooming via UI controls.
  Felt color samples must be persisted and saved for as long as the app is installed.

* **Mandate 8: Logical Space Rendering for Massé**
  All Massé shot components, including kicks and paths, must be calculated and drawn in logical
  table space. 2D transformations must occur before 3D perspective algorithms are applied. The
  order of operations must ensure that mathematical consistency is maintained within the logical
  coordinate system.

* **Mandate 9: State Cache Purging**
  Without exception, all caching of any kind related to Massé mode (shot directions, impact
  points, etc.) must be dumped and reset when the user turns off Massé mode. No Massé-specific
  state may persist into other operational modes.
