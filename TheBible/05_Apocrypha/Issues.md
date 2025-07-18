--- FILE: TheBible/05_Apocrypha/Issues.md ---

# 35: Issue Tracker & Project Roadmap

**MANDATE:** This document is for clear, technical issue tracking only. All thematic or religious jargon from other documents is forbidden here. Communication must be precise and unambiguous.

---
## Open Issues
*(A list of known bugs and required features.)*

### High Priority Bugs
*No known high-priority bugs at this time.*

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

* **Table and Ball Rotation De-Sync:** Corrected a double-rotation issue where the `Table` data class was pre-calculating its own rotated coordinates while the `UpdateStateUseCase` was also applying a world rotation via the matrix. All rotational logic was removed from `Table.kt`, consolidating the responsibility for orientation solely within the transformation matrix.