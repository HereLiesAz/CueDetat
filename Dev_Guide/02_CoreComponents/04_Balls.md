# 2.4. Ball Components

This document defines the logical objects that represent pool balls.

## Data Models vs. Roles

* **`ProtractorUnit`**: A composite model for the aiming tool. Its `center` property defines the *
  *Target Ball**, and it contains the logic to calculate the **Ghost Cue Ball**.
* **`OnPlaneBall`**: A simple data model for a user-controlled ball on the logical plane. It can
  fulfill different roles based on the application's mode.

## Roles

* **Target Ball**
* **Purpose**: Represents the object ball in Protractor Mode. It is the anchor of the aiming system.
* **Model**: Defined by the `center` of the `ProtractorUnit`.
* **Rendering**: Rendered with a simple **dot** at its center.

* **Ghost Cue Ball**
* **Purpose**: Represents the required impact point on the `TargetBall`.
* **Model**: Its position is a calculated property of the `ProtractorUnit`.
* **Rendering**: Rendered with a prominent **crosshair** at its center.

* **Actual Cue Ball**
* **Purpose**: An optional, user-draggable reference for shot visualization in Protractor Mode.
* **Model**: An instance of `OnPlaneBall`.
* **Rendering**: Rendered with a simple **dot** at its center.

* **Banking Ball**
* **Purpose**: The primary interactive ball in Banking Mode.
* **Model**: An instance of `OnPlaneBall`.
* **Rendering**: Rendered with a simple **dot** at its center.

## Default Positions

* Upon reset, or when the table is first made visible, all positions are set to a table-centric
  layout:
* The `TargetBall` is placed at the absolute center of the table (`0,0`).
* The `ActualCueBall` is placed on the head spot (horizontally centered, halfway between the table's
  center and the bottom rail).
* The `ProtractorUnit` `rotationDegrees` is set to `0f` for a straight initial shot.