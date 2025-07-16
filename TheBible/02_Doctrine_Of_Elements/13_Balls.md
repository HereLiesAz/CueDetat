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
  * **Rendering**: Rendered as a "ghosted" ball, with a simple **dot** at its center. Its on-plane shadow also has a white dot at its center.

* **Ghost Cue Ball**
  * **Purpose**: Represents the necessary impact point on the `TargetBall`.
  * **Model**: Its position is calculated within the `ProtractorUnit`. It is not an independent model.
  * **Label**: "Ghost Cue Ball".
  * **Rendering**: Rendered as a "ghosted" ball, with a prominent **crosshair** at its center. Its on-plane shadow has a white dot at its center.

* **Actual Cue Ball (Role)**
  * **Purpose**: An optional, user-draggable aiming reference in Protractor Mode.
  * **Model**: An instance of `OnPlaneBall`.
  * **Label**: "Actual Cue Ball".
  * **Rendering**: Rendered as a "ghosted" ball, with a simple **dot** at its center. Its on-plane shadow also has a white dot at its center.

* **Banking Ball (Role)**
  * **Purpose**: The primary interactive element in Banking Mode, representing the ball to be banked.
  * **Model**: An instance of `OnPlaneBall`.
  * **Label**: "Banking Ball".
  * **Rendering**: Rendered as a single circle directly on the pitched logical plane, with a simple **dot** at its center.

## Default Positions & Properties
* **When the table is not visible**, the `TargetBall` (`ProtractorUnit.center`) is at the screen's center and the `ActualCueBall` (if toggled) is halfway to the bottom edge.
* **When the table is first made visible** (via `ToggleTable` or `ToggleOnPlaneBall` while the table was previously visible), all positions are reset to a table-centric layout:
  * The `TargetBall` is placed at the absolute center of the table (and view).
  * The `ActualCueBall` is placed on the head spot (horizontally centered, halfway between the table's center and the bottom rail).
  * The `aimingAngle` is set to `-90f` to align the `GhostCueBall` directly below the `TargetBall` for a straight-in shot.
* The logical size of the balls is determined by the global zoom factor.

## Automated Detection
* As of the computer vision update, the positions of balls can be automatically detected. These detected positions are rendered as distinct visual markers (semi-transparent blue dots) for verification. This system works in parallel with the user-controlled logical balls.

## Addendum: The Magnifier - A Tool of Truth
* **Purpose**: To provide a magnified view for precise placement of the interactive balls (`TargetBall`, `ActualCueBall`, `BankingBall`).
* **Activation**: The magnifier **must** become visible only when the user begins a drag gesture on one of the aforementioned balls.
* **Behavior**: It must follow the user's finger (the `sourceCenter`) throughout the drag.
* **Deactivation**: It **must** disappear immediately when the gesture concludes.
* **Implementation**: This is achieved using the `Modifier.magnifier()` in Compose, bound to state flags (`isMagnifierVisible`, `magnifierSourceCenter`) controlled by the `GestureReducer`.