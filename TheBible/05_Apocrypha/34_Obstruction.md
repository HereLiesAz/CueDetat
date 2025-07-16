# 34: The Doctrine of Obstruction

This scripture defines the laws for detecting interference from other balls on the table, a feature designed to prevent simple shots from becoming tragic comedies.

## I. The Wide Path

To visualize the physical space a moving ball occupies, both the **Aiming Line** (for the target ball's path) and the **Shot Guide Line** (for the cue ball's path) shall be rendered with a translucent under-stroke.

* **Width:** The stroke width of this under-stroke **must** be exactly equal to the diameter of a ball (`radius * 2`).
* **Appearance:** This "pathway" must be rendered with low opacity (e.g., 20%) and a color that indicates its function, distinct from the primary line colors. It is drawn *before* the main aiming and shot lines.

## II. The Obstacle Balls

The user must be able to account for interfering balls.

* **Summoning:** A new `FloatingActionButton` shall be added to the UI. Each press of this button will dispatch an `AddObstacleBall` event, creating a new, user-draggable ball on the logical plane.
* **Representation:** Obstacle balls are instances of the `OnPlaneBall` model, stored in a list within the `OverlayState`. They are rendered as simple, on-plane circles.
* **Interaction:** The user can drag any obstacle ball to position it. This will use the new `InteractionMode.MOVING_OBSTACLE_BALL`.
* **Reset:** All obstacle balls must be cleared when the `Reset` event is dispatched.

## III. The Judgment of Collision

The system must determine if a shot is obstructed.

* **Detection:** The `UpdateStateUseCase` is responsible for collision detection. For every `ObstacleBall` in the state, it must calculate the shortest distance from that ball's center to the center-line of both the Shot Guide Path and the Aiming Path.
* **The Condition of Sin:** A collision occurs if this calculated distance is less than the diameter of a ball (i.e., `radius * 2`). This signifies that the obstacle ball overlaps with the physical path of the cue or target ball.
* **The Penance:** If a collision is detected, the `isImpossibleShot` flag in the `OverlayState` must be set to `true`. This will automatically engage the existing warning system, turning the lines red and displaying a sarcastic message, as is just and right.