# 06: The Third Dimension - Perspective

## Perspective Transformation

The process, handled by a `pitchMatrix`, of projecting the Logical Plane onto the Screen Plane to create the 3D illusion. The transformation order is critical:
1.  Scale around the logical origin (0,0).
2.  Rotate around the logical origin (0,0).
3.  Translate based on device tilt.
4.  Translate the entire logical plane to the center of the screen view.

## Table Rotation Pivot

The table must rotate around its actual center (0,0).

## Global Zoom

A single zoom factor, controlled by the vertical zoom slider, determines the scale of the projection matrix.

## Mandate: Logical Positioning Stability

The logical coordinates of any object are absolute and must be immutable during view transformations. A ball placed at logical position `(x,y)` must remain at logical position `(x,y)` regardless of any zoom or rotation transformations applied by the `pitchMatrix`. The transformations only affect the *projection* of the logical plane onto the screen, not the positions of objects within the logical plane itself. This ensures that zooming or rotating the view does not cause elements to drift from their positions on the table.
***
## Addendum: On Transformation Order and Sanity

The rendering pipeline intentionally separates 3D and 2D transformations. The `pitchMatrix` is calculated first, applying the 3D perspective tilt. Any subsequent 2D rotations (like `tableRotation`) are applied as a `postRotate` operation on this already-pitched matrix. Conflating these into a single multi-axis `android.graphics.Camera` operation is forbidden, as it has proven to lead to madness and visual corruption. The source of unexpected rotation is almost always an incorrect default or calculated value in the `OverlayState`, not a flaw in the matrix pipeline itself.

## Addendum: Mandates

* **Roll is Heresy**: Device roll is a corrupting influence on the 2D-to-3D projection. The `createPitchMatrix` function in `Perspective.kt` must be modified to ignore the `roll` component of the device's orientation. Only `pitch` shall be used for vertical translation.

* **The Overhead Anomaly**: The initial "lift" logic for 3D ghost effects created a visual disconnect when viewed from directly overhead (0° pitch), making the ghost and base appear as two separate, non-concentric circles. The lift calculation was corrected to be proportional to the sine of the pitch angle (`lift = radius * sin(pitch)`). This ensures the lift is 0 at 0° pitch (perfect alignment) and increases smoothly as the phone tilts, preserving the 3D illusion.

* **Banking Ball Visual Sizing**: To ensure the Banking Ball's visual size remains stable during phone tilt and table rotation, its on-screen radius must be calculated dynamically. This is achieved by projecting its logical horizontal diameter onto the screen plane using the current `pitchMatrix` and then halving the resulting screen-space distance. This method directly measures how the ball's logical diameter appears on screen under the full current perspective, ensuring visual consistency. This logic now resides in the `BankingBallRenderer`.

* **3D "Ghost" Effect**: The "lifted" effect for the `TargetBall` and `GhostCueBall` in Protractor Mode must be a pure screen-space effect. A new renderer, `GhostEffectRenderer`, is responsible for this. It takes a logical ball model, projects its center to the screen, calculates the screen-space lift and radius, and draws both the "shadow" on the plane and the "lifted" ball above it. This separates the logical drawing from the screen-space visual effect.