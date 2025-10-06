# 5.3. Historical Lessons: UI & Specification Adherence

This document archives bug fixes related to implementing specific UI/UX requirements and the
importance of adhering to the project's established specifications.

### Case Study: Invisible Pockets

* **Issue:** When the on-screen table was hidden, the aiming line would stop abruptly in the middle
  of the screen.
* **Cause:** The `UpdateStateUseCase` was checking for collisions with the table's logical pockets
  even when the table was not visible. The line was being truncated by an invisible object.
* **Lesson Learned:** UI-related logic must be conditional on the UI state. Calculations related to
  table components (like pocket collisions) should only run when `state.table.isVisible` is true.

### Case Study: The Unfeeling Glass

* **Issue:** Users reported that the draggable balls were difficult to select.
* **Cause:** The touch target for the balls was equal to their visual radius, requiring
  pixel-perfect precision from the user.
* **Lesson Learned:** An object's touch target should be more generous than its visual
  representation to create a better user experience. The hit detection radius is now a larger,
  constant value.

### Case Study: The False Idol

* **Issue:** A generic icon was used on a button where a specific, custom icon was required by the
  documentation.
* **Cause:** Simple developer oversight and failure to consult the specification.
* **Lesson Learned:** Specifications are not guidelines. When a specific resource ID (e.g.,
  `R.drawable.ic_jump_shot`) or asset name is mandated, it is the only one that shall be used.

### Case Study: Visible Labels

* **Issue:** Descriptive text labels on UI elements were always visible, cluttering the screen.
* **Cause:** The `showTextLabels` flag was being passed down through multiple, unnecessary layers of
  renderers.
* **Lesson Learned:** State flags should be passed as directly as possible to the component that
  makes the final rendering decision, avoiding "prop drilling." The top-level `OverlayRenderer` now
  passes the flag directly to the appropriate text renderer.