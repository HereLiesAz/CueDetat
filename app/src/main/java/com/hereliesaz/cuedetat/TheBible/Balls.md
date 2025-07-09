# 17: The Balls

* **Default Positions:**
  * The **Target Ball** must be positioned at the absolute center of the logical table: `(0,0)`.
  * The **Actual Cue Ball** must be positioned at the table's head spot (center of the second diamonds).
  * The default `aimingAngle` must be set so that the **Shot Line** and **Aiming Line** are perfectly vertical and overlapping, pointing straight "up" towards the top of the screen (an angle of -90 or 270 degrees).
* **Proportions:** The size of the balls is constant in logical space.
* Their on-screen size is determined by the global zoom factor. They must appear proportionate to the table.
***
## Addendum: Detailed Ball Specifications

* **`TargetBall`**
  * **Purpose**: The logical and visual anchor of the aiming system in Protractor Mode. Represents the object ball.
  * **2D Logical**: A circle whose `center` is user-draggable on the logical plane.
  * **3D Screen-Space**: Rendered with a calculated vertical "lift" to appear floating above the logical plane.
  * **Label**: "T".

* **`GhostCueBall`**
  * **Purpose**: Represents the necessary impact point on the `TargetBall` in Protractor Mode.
  * **2D Logical**: A circle whose `center` is calculated based on the `TargetBall`'s center and the current `aimingAngle`.
  * **3D Screen-Space**: Rendered with a calculated vertical "lift".
  * **Label**: "G".

* **`ActualCueBall`**
  * **Purpose (Protractor Mode)**: An optional, user-draggable aiming reference.
  * **Purpose (Banking Mode)**: The primary interactive element, representing the cue ball on the table.
  * **2D Logical**: A circle whose `center` is user-controlled.
  * **3D Screen-Space**: Rendered directly on the projected table plane (no lift). Its visual size is dynamically calculated to ensure stability during tilt and rotation.
  * **Label**: "A".

* **`BankingBall`**
  * This is not a distinct model. The `ActualCueBallModel` is used to represent the cue ball in Banking Mode. The label changes to "B".