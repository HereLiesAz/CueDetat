# 04: Core Operational Modes

The application operates in two distinct modes, toggled from the menu.

## Protractor Mode
* This is the default aiming mode for standard cut shots.
* **Visible Elements:** `ActualCueBall`, `TargetBall`, `GhostCueBall`.
* **Functionality:** The user drags the `TargetBall` and `ActualCueBall` to match the real balls on the table. The `aimingAngle` can be adjusted with a rotational drag gesture, moving the `GhostCueBall` around the `TargetBall`. The rendered lines show the required shot path.
* **Table Integration:** When the table is made visible in this mode, it provides a frame of reference and enables predictive banking calculations for the aiming line.

## Banking Mode
* **Visible Elements:** `ActualCueBall` (as "Banking Ball") and the `Table Visuals`.
* **Default Orientation:** The table defaults to a "portrait" orientation (`tableRotationDegrees = 90f`), matching the default for a visible table in Protractor Mode.
* **Functionality:** Allows the user to calculate multi-rail kick and bank shots.

***
## Addendum: Detailed Mode Specifications

### Protractor Mode

* **Active Elements**:
  * `TargetBall`: The logical and visual anchor of the protractor.
  * `GhostCueBall`: Calculated position representing the required impact point on the `TargetBall`.
  * `ActualCueBall` (Optional): A user-draggable reference point for shot visualization.
  * All associated lines (Aiming, Shot, Tangent, Fixed Angle Guides).
* **Interaction**:
  * **Aiming**: A rotational drag gesture around the `TargetBall` adjusts the `aimingAngle`, which moves the `GhostCueBall` in an orbit.
  * **Positioning**: The `ActualCueBall` (if visible) and `TargetBall` are draggable on the logical plane.
  * **Zoom**: The vertical slider controls the Global Zoom.

### Banking Mode

* **Active Elements**:
  * `ActualCueBall` (as the "Banking Ball"): The primary interactive element, user-draggable on the table plane.
  * `bankingAimTarget`: A user-defined logical point that sets the initial vector of the bank shot calculation.
  * Table & Rails: The wireframe table becomes visible.
  * Banking Shot Line.
* **Interaction**:
  * **Cue Position**: The Banking Ball is dragged to position it on the logical table.
  * **Aiming**: A drag gesture anywhere else on the screen moves the `bankingAimTarget`, updating the calculated reflection path in real-time.
  * **Table Rotation**: The horizontal slider rotates the entire table and banking shot apparatus around the view's center.