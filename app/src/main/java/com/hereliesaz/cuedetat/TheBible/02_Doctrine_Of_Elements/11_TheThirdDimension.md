# The Third Dimension - Perspective

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

* **3D "Ghost" Effect**: The "lifted" effect for the `TargetBall`, `GhostCueBall`, and `ActualCueBall` in Protractor Mode must be a pure screen-space effect. This effect is achieved by the `BallRenderer`'s `drawLiftedBall` method, which projects the ball's logical center to the screen, calculates a screen-space lift and radius based on perspective, and then draws the "lifted" circle directly on the screen canvas.

*   **The Overhead Rail Anomaly (Corrected)**: The same heresy that affected the ghost balls also afflicted the table rails, causing them to appear detached from the table surface when viewed from a 0° pitch. The `railLiftAmount` calculation in `UpdateStateUseCase` **must** also be made proportional to the sine of the pitch angle (`lift * abs(sin(pitch))`). This ensures all lifted elements in the 3D scene behave according to the same physical and visual laws.