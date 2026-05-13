# 4.2. Feature Specification: Banking Mode

This document specifies the behavior of the Banking Calculator feature.

## Core Components & Interaction

* **Banking Ball (`OnPlaneBall`)**: The primary interactive element, representing the cue ball. The
  user drags this to position it on the logical table.
* **Aiming Target (`bankingAimTarget`)**: A logical point controlled by the user's finger. A drag
  gesture anywhere on the screen (that is not on the Banking Ball) moves this target.
* **Calculated Path (`bankShotPath`)**: The calculated, reflected path of the shot.

## Functional Requirements

* **Default Orientation:** Upon entering Banking Mode, the table **must** default to a "portrait"
  orientation (`90f` rotation).
* **Responsiveness:** The aiming interaction must be instantaneous. The `ViewModel` takes a "fast
  path" for this gesture, updating only the `bankingAimTarget` and re-calculating the bank shot path
  without engaging the full state update pipeline.
* **Reflection Logic:** The path is calculated using pure vector reflection for up to a maximum of *
  *four** reflections (five line segments).
* **Color Progression & Pocketing:**
* Each segment of the `bankShotPath` must be rendered in a progressively more muted and thinner
  style.
* If a pocket is made, the rendering of the `bankShotPath` **must terminate** at the point of
  collision, and **all** segments of the path must turn pure white.
* **Stability**: The logical positions of the `BankingBall` and `bankingAimTarget` must remain
  stable during view zoom or rotation.