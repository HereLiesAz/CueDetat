# 19: Shot Visualization

This file pertains to the primary operational mode of the application, which visualizes a standard cut shot using a system of logical and screen-space balls.
## Core Components

* **`TargetBall`**: The logical anchor of the system, representing the object ball. It is fixed at the logical center of the view.
* **`GhostCueBall`**: A calculated logical ball showing the required impact point on the `TargetBall` to pocket it along the **Aiming Line**. Its position is determined by the user-controlled `aimingAngle`.
* **`ActualCueBall`**: An optional, user-draggable logical ball representing the real-world position of the cue ball. It provides the origin for the **Shot Line**.

## Functional Visualization

The system works by creating a clear, geometric relationship between these three components and their corresponding lines:

1.  The user aligns the **`TargetBall`**'s 3D ghost over the real-world object ball.
2.  The user adjusts the **`aimingAngle`** to align the **Aiming Line** with a pocket. This moves the **`GhostCueBall`** into the correct logical position.
3.  If the **`ActualCueBall`** is enabled, the user drags its 3D ghost to match the real-world cue ball.
4.  The rendered **Shot Line** now displays the exact path the user must strike the `ActualCueBall` along to hit the `GhostCueBall` position, thus executing the visualized shot.
5.  The **Tangent Lines** show the path the cue ball will take after impact if no spin is applied.
## Addendum: Interaction Mandate

* The aiming gesture is a **direct linear drag**. Dragging a finger anywhere on the screen (not on another interactive element) must control the `aimingAngle`. The horizontal (`dx`) and vertical (`dy`) components of the drag must be combined to produce a fluid rotation of the aiming line.
* The previous implementation of a "rotational drag" is heresy and must not be used.