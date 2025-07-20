# 4.1. Feature Specification: Protractor Mode

This document specifies the behavior of the primary shot visualization mode.

## Core Components

* **`TargetBall`**: The logical anchor of the system, representing the object ball.
* **`GhostCueBall`**: A calculated logical ball showing the required impact point on the
  `TargetBall`. Its position is determined by the `rotationDegrees` of the `ProtractorUnit`.
* **`ActualCueBall`**: An optional, user-draggable logical ball representing the real-world position
  of the cue ball. It provides the origin for the **Shot Guide Line**.

## Functional Visualization

1. The user aligns the **`TargetBall`** over the real-world object ball.
2. The user performs a **direct linear drag** on an empty part of the screen to aim the **Aiming
   Line** at a pocket. This sets the `rotationDegrees` and moves the **`GhostCueBall`** into the
   correct logical position.
3. If the **`ActualCueBall`** is enabled, the user drags it to match the real-world cue ball.
4. The rendered **Shot Line** now displays the exact path the user must shoot.
5. The **Tangent Lines** show the path the cue ball will take after impact if no spin is applied.
6. The **Spin Control** can be used to visualize post-contact trajectories with english.