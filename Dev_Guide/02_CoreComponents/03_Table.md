# 2.2. Perspective and 3D Transformation

The transformation from the 2D logical plane to the 3D screen view is handled by
`UpdateStateUseCase.kt` and `Perspective.kt`.

## Transformation Rules

* **Order of Operations**: The final projection matrix **must** be created by first applying 2D
  logical transformations (zoom) and *then* applying the 3D camera transformations (rotation and
  pitch). This prevents visual artifacts like the table "rolling" instead of spinning. The correct
  3D transformation order within the `Camera` object is: `rotateY` (for table spin), then
  `rotateX` (for device pitch), then `translate` (for rail lift).
* **Decoupled Zoom**: Zoom is a 2D scaling of the logical plane via the `worldMatrix`. It is not a
  change in the camera's Z-axis position. This prevents zoom from affecting the amount of
  perspective distortion.
* **Rail Lift**: The `railLiftAmount` is calculated in `UpdateStateUseCase` and is proportional to
  the sine of the pitch angle (`lift * abs(sin(pitch))`). This lift is applied as a `translate`
  operation in the `Camera` *after* all rotations.