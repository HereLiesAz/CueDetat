# 20: The Doctrine of the Bank Calculator

This document details the sacred mechanics of the Banking Calculator, a core feature of the application.

## Core Components & Interaction

* **Banking Ball (`OnPlaneBall`)**: The primary interactive element, representing the cue ball. The user drags this to position it on the logical table.
* **Aiming Target (`bankingAimTarget`)**: A logical point controlled by the user's finger. A drag gesture anywhere on the screen (that is not on the Banking Ball) moves this target.
* **Calculated Path (`bankShotPath`)**: The true, calculated path of the shot, reflecting off the table rails. This is what is rendered to the user.

## Functional Mandates

* **Default Orientation:** Upon entering Banking Mode, the table **must** default to a "portrait" orientation (`90f` rotation).
* **Responsiveness:** The aiming interaction **must** be instantaneous. The `GestureReducer` must correctly identify the aiming gesture and the `ViewModel` must take a "fast path", updating only the `bankingAimTarget` and re-calculating the bank shot path without engaging the full state update pipeline.
* **Reflection Logic:** The path is calculated using pure vector reflection. The direction vector is initialized once and then updated with each subsequent reflection. The calculation must account for up to a maximum of **four** reflections.
* **Color Progression & Pocketing Visualization:**
    * Each segment of the `bankShotPath` must be rendered in a progressively more muted and thinner style, from brightest/widest on the first segment to dimmest/thinnest on the last.
    * If a pocket is made, the rendering of the `bankShotPath` **must terminate** at the point of collision, and **all** segments of the path must turn pure white to indicate a successful shot.
* **Stability**: The logical positions of the `BankingBall` and the `bankingAimTarget` **must remain stable** when the user zooms or rotates the view.