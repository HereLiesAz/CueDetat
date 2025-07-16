# The Third Dimension - Perspective

## Perspective Transformation

The transformation from the 2D logical plane to the 3D screen view is a sacred and precise process, governed by the `UpdateStateUseCase` and `Perspective.kt`.

### Mandates of Transformation

*   **Doctrine of Rightful Order:** The final projection matrix **must** be created by first applying all 2D logical transformations (zoom and rotation) and *then* applying the 3D camera transformations (pitch/tilt). To do otherwise is to invite the "wobble" heresy.
*   **Doctrine of True Spin:** The rotation of the table **must** be a 2D rotation of the logical plane *before* any perspective is applied. This is handled in `UpdateStateUseCase.kt`. The `Perspective.kt` class itself must remain ignorant of table rotation and only handle the camera's tilt.
*   **Doctrine of Decoupled Zoom:** Zoom is a 2D scaling of the logical plane. It is not a change in the camera's Z-axis position. This decoupling prevents zoom from affecting the perspective distortion (the "tilt" effect).
*   **Doctrine of Unified Sizing:** The on-screen radius of a 3D "ghost" ball **must** be identical to its 2D "shadow" counterpart. This is achieved in `DrawingUtils.kt` by projecting two logical points (the ball's center and a point on its edge) using the final `pitchMatrix` and measuring their screen-space distance. Using a `flatMatrix` for this purpose is a corrected heresy, as it failed to account for zoom.
*   **Doctrine of Rail Lift:** The `railLiftAmount` calculation in `UpdateStateUseCase` **must** be proportional to the sine of the pitch angle (`lift * abs(sin(pitch))`). This ensures the rails appear flush with the table at a 0° pitch and rise naturally with the tilt.