# The Balls

This scripture defines the logical objects that represent pool balls.

## Data Models vs. Roles
* **`ProtractorUnitModel`**: A composite model representing the aiming tool. Its `center` property defines the **Target Ball**, and it contains the logic to calculate the **Ghost Cue Ball**.
* **`OnPlaneBallModel`**: A simple data model representing a user-controlled ball that exists on the logical plane. It is a single model that plays two distinct **roles** depending on the application's mode.

## The Roles

* **Target Ball**
  * **Purpose**: The logical anchor of the aiming system in Protractor Mode. Represents the object ball.
  * **Model**: Defined by the `center` of the `ProtractorUnitModel`.
  * **Label**: "T".
  * **Rendering**: Rendered as a "ghosted" ball, with both an on-plane shadow and a lifted 3D circle.

* **Ghost Cue Ball**
  * **Purpose**: Represents the necessary impact point on the `TargetBall`.
  * **Model**: Its position is calculated within the `ProtractorUnitModel`. It is not an independent model.
  * **Label**: "G".
  * **Rendering**: Rendered as a "ghosted" ball, with both an on-plane shadow and a lifted 3D circle.

* **Actual Cue Ball (Role)**
  * **Purpose**: An optional, user-draggable aiming reference in Protractor Mode.
  * **Model**: An instance of `OnPlaneBallModel`.
  * **Label**: "A".
  * **Rendering**: Rendered as a single circle directly on the pitched logical plane.

* **Banking Ball (Role)**
  * **Purpose**: The primary interactive element in Banking Mode, representing the ball to be banked.
  * **Model**: An instance of `OnPlaneBallModel`.
  * **Label**: "B".
  * **Rendering**: Rendered as a single circle directly on the pitched logical plane.

## Default Positions & Properties
* The **Target Ball** (`ProtractorUnitModel.center`) must be positioned at the absolute center of the logical table.
* The **Actual Cue Ball** role, when first enabled, should position its `OnPlaneBallModel` instance at the table's head spot.
* The default `rotationDegrees` of the `ProtractorUnitModel` must be `-90f`.
* The logical size of the balls is determined by the global zoom factor.