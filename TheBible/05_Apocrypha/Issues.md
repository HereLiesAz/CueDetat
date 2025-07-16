# 35: Issue Tracker & Project Roadmap

**MANDATE:** This document is for clear, technical issue tracking only. All thematic or religious jargon from other documents is forbidden here. Communication must be precise and unambiguous.

---
## Open Issues
*(A list of known bugs and required features.)*

### High Priority Bugs
1.  **Zoom Controls Affect Pitch:** Both the zoom slider and the pinch-to-zoom gesture incorrectly control the camera's pitch (vertical tilt) instead of the camera's Z-axis (zoom). The pinch-to-zoom gesture is completely unresponsive.
2.  **Ball Anchoring:** When zoomed, all logical balls (`ProtractorUnit`, `OnPlaneBall`) maintain their position relative to the screen, not the logical table, causing a visual "drift" against the table surface. This indicates their final rendered positions are not being correctly derived from a single, unified transformation matrix.

### Feature Implementation
1.  **Table Pan:** The user must be able to move the table vertically on the logical plane.
2.  **Perspective Scaling:** Logical objects do not currently scale with distance; a ball at the far end of the table appears the same size as one at the near end. This must be implemented as part of the perspective projection.
3.  **Contextual CV Snapping:** The auto-snapping feature is too aggressive. It should only snap a user-placed ball to a detected object if the ball is placed within a small proximity threshold of that object.
4.  **CV Visualization Tools:**
    * A developer toggle is needed to display the CV's color/shape mask on-screen to aid in parameter tuning.
    * A new UI workflow is required to allow the user to calibrate the CV pipeline for a specific table's felt color.
5.  **Interactive Tutorial:** The current full-screen tutorial is blocking. It needs to be redesigned as a non-blocking, contextual overlay. A "virtual table" background should also be available for users not in front of a physical table.
6.  **User Feedback Channel:** A "Send Feedback" option should be added to the menu that opens the user's default email client.

### Low Priority / Polish
1.  **Shot Guide Line Origin:** When the `ActualCueBall` is hidden, the `ShotGuideLine`'s origin is not correctly anchored and must be fixed to originate from the bottom-center of the screen.
2.  **App Tagline:** The tagline "May your shot be better than your excuses" is missing from the splash screen.

---
## Resolved Issues
*(A log of completed tasks and fixed bugs.)*

* **Table Orientation:** Corrected the rendering logic in `TableRenderer` and `RailRenderer` to ensure the table's default 0-degree state is "portrait."
* **Unified Table Model:** Refactored the codebase to use a single `Table.kt` model as the source of truth for all table geometry, rotation, and visibility, resolving inconsistencies between the rails and the table surface.
* **Coordinate System Unification:** Corrected the `VisionRepository` to transform screen-space coordinates from CV detections into logical-space coordinates before updating the state, resolving a major cause of object drift.
* **Matrix Transformation Order:** Corrected the matrix multiplication order in `UpdateStateUseCase` to ensure rotation is applied to logical coordinates before the perspective projection, fixing visual warping.
* **Gesture Input Logic:** Standardized various gesture inputs (`drag`, `zoom`, `rotation`) to correctly update the state via the `StateReducer`.
* **UI/UX:**
    * Resolved an issue where the menu did not scroll on smaller screens.
    * Made warning text font size dynamic and relative to screen width.
    * Enlarged touch targets for UI sliders.
* **Dependency Injection & Rendering:** Corrected multiple DI and rendering chain errors, including a `NoClassDefFoundError` for `LineTextRenderer` and ensuring all renderers are correctly injected and used.
* **Feature Removal:** Fully removed the deprecated in-app updater and donation features.