# 20: The Doctrine of the Bank Calculator

This document details the sacred mechanics of the Banking Calculator, a core feature of the application.

## Core Components & Interaction

*   **Banking Ball (`OnPlaneBall`)**: The primary interactive element, representing the cue ball. The user drags this to position it on the logical table.
*   **Aiming Target (`bankingAimTarget`)**: A logical point controlled by the user's finger. A drag gesture anywhere on the screen (that is not on the Banking Ball) moves this target.
*   **Bank Shot Path**: A multi-segment line that originates from the Banking Ball and aims toward the Aiming Target. This is the **initial vector only** and is not rendered.
*   **Calculated Path (`bankShotPath`)**: The true, calculated path of the shot, reflecting off the table rails. This is what is rendered to the user.

## Functional Mandates

*   **Responsiveness:** The aiming interaction **must** be instantaneous. When the user drags their finger to aim, the `MainViewModel` must take a "fast path", updating only the `bankingAimTarget` and re-calculating the bank shot path without engaging the full state update pipeline. This ensures the line keeps up with the user's finger.
*   **Confinement:** The rendered `bankShotPath` must be strictly confined to the logical boundaries of the currently selected table size. Its segments must begin and end on the logical rails.
*   **Reflection Logic:** The path is calculated using pure vector reflection. It must calculate up to a maximum of 4 reflections (rails).
*   **Color Progression:** Each segment of the `bankShotPath` must be rendered in a progressively more muted shade of yellow, from brightest (`BankLine1Yellow`) on the first segment to dimmest (`BankLine4Yellow`) on the last, signifying a decay of energy.
*   **Pocketing Visualization:**
  *   The calculation must detect if the final segment of the path collides with a pocket's geometry.
  *   If a pocket is made, the rendering of the `bankShotPath` **must terminate** at the point of collision.
  *   The pocket that was made must be filled with pure white to indicate a successful shot.

*   **Stability**: The logical positions of the `BankingBall` and the `bankingAimTarget` **must remain stable** when the user zooms or rotates the view. These actions are for observation, not alteration.