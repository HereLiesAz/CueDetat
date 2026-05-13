# 4.4. Feature Specification: Obstruction Detection

This document defines the logic for detecting interference from other balls on the table.

## I. Wide Path Visualization

To visualize the physical space a moving ball occupies, both the **Aiming Line** and the **Shot
Guide Line** are rendered with a translucent under-stroke.

* **Width:** The stroke width of this under-stroke **must** be exactly equal to the diameter of a
  ball (`radius * 2`).
* **Appearance:** This "pathway" is rendered with low opacity (20%) and a distinct color. It is
  drawn *before* the main aiming and shot lines.

## II. Obstacle Balls

* **Creation:** A dedicated button dispatches an `AddObstacleBall` event, creating a new,
  user-draggable `OnPlaneBall` on the logical plane.
* **Interaction:** The user can drag any obstacle ball. This uses the
  `InteractionMode.MOVING_OBSTACLE_BALL`.
* **Reset:** All obstacle balls are cleared when the `Reset` event is dispatched.

## III. Collision Detection

* **Responsibility:** The `UpdateStateUseCase` is responsible for collision detection.
* **Logic:** For every `ObstacleBall`, it calculates the shortest distance from that ball's center
  to the center-line of both the Shot Guide Path and the Aiming Path.
* **Collision Condition:** A collision occurs if this distance is less than the diameter of a ball (
  `radius * 2`).
* **Result:** If a collision is detected, the `isObstructed` flag in the `OverlayState` must be set
  to `true`, which engages the warning system.