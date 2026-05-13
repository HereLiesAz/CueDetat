# 5.1. Historical Lessons: State & Event Management

This document archives key bug fixes and architectural decisions related to state management,
serving as a guide to avoid repeating past mistakes.

### Case Study: Purity of the State Reducer

* **Issue:** A rotational drag gesture was behaving unpredictably on subsequent uses.
* **Cause:** The `StateReducer` was stateful. It contained a `lastAngle` property to calculate the
  delta between drag events. As a singleton, this property persisted across gestures, causing
  incorrect calculations.
* **Lesson Learned:** Reducers **must** be pure, stateless functions. Their output should depend
  only on their inputs (the current state and the event). Transient data needed for a single,
  continuous gesture must be stored in the `OverlayState` itself, not as a property of the reducer.

### Case Study: Specificity of Events

* **Issue:** A "lingering" spin path indicator, which was supposed to fade out after a drag,
  disappeared instantly.
* **Cause:** A generic `GestureEnded` event was firing for all interactions, including the end of a
  spin control drag. This caused the `GestureReducer` to reset the interaction mode, prematurely
  clearing the `lingeringSpinOffset` state that the fade-out animation depended on.
* **Lesson Learned:** Use specific events for specific interactions. A generic `GestureEnded` is too
  broad. A more specific event, `SpinSelectionEnded`, was created to be handled exclusively by the
  `SpinReducer`, allowing the gesture to end and the linger/fade animation to begin correctly.

### Case Study: Initialization and Default State

* **Issue:** On first launch, the aiming protractor defaulted to a horizontal (`0f`) angle instead
  of the specified vertical shot.
* **Cause:** The default values in the `OverlayState` data class did not match the specification.
* **Lesson Learned:** The default values within a state class are the genesis of the application's
  world. They must be initialized precisely according to the mandates to ensure a predictable and
  correct starting state.

### Case Study: State Flow Across UI Systems (Compose vs. Android View)

* **Issue:** The application had two conflicting sources of truth for the light/dark theme, causing
  visual inconsistencies.
* **Cause:** An incorrect assumption that a Compose `MaterialTheme` would automatically apply its
  properties to a classic `AndroidView`. A redundant `isForceLightMode` flag was added to a
  different state object.
* **Lesson Learned:** An `AndroidView` within a Compose hierarchy does not automatically obey
  Compose theming. All necessary state, including theme-defining flags, must be passed down from the
  single source of truth (`OverlayState`) and applied within the `AndroidView`'s update logic (in
  this case, the `PaintCache`).