# The Third Dimension - Perspective

## Perspective Transformation

The transformation from the 2D logical plane to the 3D screen view is a sacred and precise process, governed by the `UpdateStateUseCase.kt` and `Perspective.kt`.

### Mandates of Transformation

* **Doctrine of Rightful Order:** The final projection matrix **must** be created by first applying all 2D logical transformations (zoom and rotation) and *then* applying the 3D camera transformations (pitch/tilt). To do otherwise is to invite the "roll" heresy, where the tilt is applied relative to a fixed world axis instead of the viewer's screen.
* **Doctrine of Decoupled Zoom:** Zoom is a 2D scaling of the logical plane. It is not a change in the camera's Z-axis position. This decoupling prevents zoom from affecting the perspective distortion (the "tilt" effect).
* **Doctrine of Unified Sizing:** The on-screen radius of a 3D "ghost" ball **must** remain constant regardless of its position on the rotated table. Its size is a function of zoom and pitch only, not its perceived distance. This is achieved by calculating a single `visualBallRadius` in the `UpdateStateUseCase` using a matrix that is free from rotation, and passing this singular truth down to all renderers.
* **Doctrine of Rail Lift:** The `railLiftAmount` calculation in `UpdateStateUseCase` **must** be proportional to the sine of the pitch angle (`lift * abs(sin(pitch))`). This ensures the rails appear flush with the table at a 0° pitch and rise naturally with the tilt.