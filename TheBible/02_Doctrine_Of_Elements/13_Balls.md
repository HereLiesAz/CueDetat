# The Balls

This scripture defines the logical objects that represent pool balls.

## Data Models vs. Roles
* **`ProtractorUnit`**: A composite model representing the aiming tool. Its `center` property defines the **Target Ball**, and it contains the logic to calculate the **Ghost Cue Ball**.
* **`OnPlaneBall`**: A simple data model representing a user-controlled ball that exists on the logical plane. It is a single model that plays two distinct **roles** depending on the application's mode.

## The Roles

* **Target Ball**
  * **Purpose**: The logical anchor of the aiming system in Protractor Mode. Represents the object ball.
  * **Model**: Defined by the `center` of the `ProtractorUnit`.
  * **Rendering**: Rendered as a "ghosted" ball, with a simple **dot** at its center. Its on-plane shadow also has a white dot at its center.

* **Ghost Cue Ball**
  * **Purpose**: Represents the necessary impact point on the `TargetBall`.
  * **Model**: Its position is calculated within the `ProtractorUnit`. It is not an independent model.
  * **Rendering**: Rendered as a "ghosted" ball, with a prominent **crosshair** at its center. Its on-plane shadow has a white dot at its center.

* **Actual Cue Ball (Role)**
  * **Purpose**: An optional, user-draggable aiming reference in Protractor Mode.
  * **Model**: An instance of `OnPlaneBall`.
  * **Rendering**: Rendered as a "ghosted" ball, with a simple **dot** at its center. Its on-plane shadow also has a white dot at its center.

* **Banking Ball (Role)**
  * **Purpose**: The primary interactive element in Banking Mode, representing the ball to be banked.
  * **Model**: An instance of `OnPlaneBall`.
  * **Rendering**: Rendered as a single circle directly on the pitched logical plane, with a simple **dot** at its center.

## Default Positions & Properties
* When the table is not visible, the `TargetBall` (`ProtractorUnit.center`) is at the screen's center (logical `0,0`) and the `ActualCueBall` (if toggled) is anchored to the bottom of the screen.
* Upon reset, or when the table is first made visible, all positions are set to a table-centric layout:
  * The `TargetBall` / `BankingBall` is placed at the absolute center of the table (`0,0`).
  * The `ActualCueBall` is placed on the head spot (horizontally centered, halfway between the table's center and the bottom rail).
  * The `ProtractorUnit` `rotationDegrees` is set to `0f` to align the `GhostCueBall` directly below the `TargetBall` on the Y-axis, creating a straight initial shot.
* The logical size of the balls is determined by the global zoom factor.