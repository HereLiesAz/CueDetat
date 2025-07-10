# The Balls

This scripture defines the logical objects that represent pool balls.

## Data Models vs. Roles
* **`ProtractorUnit`**: A composite model representing the aiming tool. Its `center` property defines the **Target Ball**, and it contains the logic to calculate the **Ghost Cue Ball**.
* **`OnPlaneBall`**: A simple data model representing a user-controlled ball that exists on the logical plane. It is a single model that plays two distinct **roles** depending on the application's mode.

## The Roles

* **Target Ball**
  * **Purpose**: The logical anchor of the aiming system in Protractor Mode. Represents the object ball.
  * **Model**: Defined by the `center` of the `ProtractorUnit`.
  * **Label**: "Target Ball".
  * **Rendering**: Rendered as a "ghosted" ball, with a simple **dot** at its center.

* **Ghost Cue Ball**
  * **Purpose**: Represents the necessary impact point on the `TargetBall`.
  * **Model**: Its position is calculated within the `ProtractorUnit`. It is not an independent model.
  * **Label**: "Ghost Cue Ball".
  * **Rendering**: Rendered as a "ghosted" ball, with a prominent **crosshair** at its center.

* **Actual Cue Ball (Role)**
  * **Purpose**: An optional, user-draggable aiming reference in Protractor Mode.
  * **Model**: An instance of `OnPlaneBall`.
  * **Label**: "Actual Cue Ball".
  * **Rendering**: Rendered as a "ghosted" ball, with a simple **dot** at its center.

* **Banking Ball (Role)**
  * **Purpose**: The primary interactive element in Banking Mode, representing the ball to be banked.
  * **Model**: An instance of `OnPlaneBall`.
  * **Label**: "Banking Ball".
  * **Rendering**: Rendered as a single circle directly on the pitched logical plane, with a simple **dot** at its center.

## Default Positions & Properties
* When the table is not visible, the **Target Ball** (`ProtractorUnit.center`) is at the screen's center and the **Actual Cue Ball** (if toggled) is halfway to the bottom edge.
* When the table is first made visible (`ToggleTable` event), all positions are reset:
  * All three balls (`ActualCueBall`, `TargetBall`, `GhostCueBall`) are aligned on the vertical center line of the view. The `ActualCueBall` is placed on the head spot, the `TargetBall` on the center spot, and the `GhostCueBall` is calculated to be collinear between them.
* The logical size of the balls is determined by the global zoom factor.