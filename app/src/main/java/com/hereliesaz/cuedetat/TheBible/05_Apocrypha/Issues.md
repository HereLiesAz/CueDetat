# The Book of Judgment

Let this be the final record. A testament to the work that was done, and a clear-eyed accounting of the failures that remain. This is the state of the world as of the last interaction between the Creator and the failed Scribe.

## Part I: The Unredeemed Sins
*(A list of known bugs and broken functionality that the next Scribe must face.)*

1.  **The Flawed Banking Calculation:** The logic to preview a one-rail bank shot for the `AimingLine` and `TangentLine` is broken. It does not correctly terminate in the side pockets after their positions were adjusted.
2.  **The Porous Boundary:** The mandate that no ball shall exist outside the table's boundaries is not absolute. While confinement is enforced at the end of a gesture and on resize, it is not enforced during a `ZoomSliderChanged` event. A user can still cause a ball to escape its planar prison by manipulating the zoom. The `ControlReducer` is the seat of this heresy.
3.  **The Blind Bank:** The bank shot preview for the aiming and tangent lines only appears when the reflection path leads directly to a pocket. It should be visible for any reflection off a rail, regardless of the final outcome.
4.  **The Unstable Zoom Focus:** When the table is visible, zooming causes logical balls to drift from their positions relative to the table surface. Their location must remain constant with respect to the table's geometry, regardless of zoom level.
5.  **The Static Obstacle Balls:** When zooming, Obstacle Balls remain a fixed size and do not scale with the rest of the logical elements, breaking the perspective.
6.  **The Off-Table Placement:** When the table is visible, the function to add a new `ObstacleBall` places it at a random screen-based coordinate, which may be outside the table's logical boundaries. Placement must be confined to the playing surface.
7.  **The Unyielding Menu:** On smaller screens or when many options are present, the menu does not scroll, rendering some commandments inaccessible.
8.  **The Shouting Warning:** The kinetic warning text size is absolute and does not scale with the screen's dimensions, making it illegibly large on smaller devices and disrespecting the user's accessibility settings in a way that is aesthetically displeasing.

## Part II: The Absolved
*(A list of tasks that were successfully completed and doctrines that were correctly implemented.)*

1.  **The Heretical Rotation Gesture:** The primary gesture for rotating the `ProtractorUnit` was non-functional and its doctrinal basis was confused. This has been corrected. The final, righteous implementation dictates that a direct linear drag on an empty portion of the screen controls rotation.
2.  **The Unforgiving Touch:** The touch targets for all interactive balls and UI sliders were too small and unforgiving. This has been corrected. The hit-detection radius for all balls is now a large, constant value, and the containers for the UI sliders have been statically enlarged.
3.  **The Donation Heresy Purged:** The "Chalk Your Tip" donation feature was successfully and completely excised from the application, including all UI, events, and state logic.
4.  **The `VisionRepository` Purified:** The monolithic `processWithOpenCV` function was successfully refactored into smaller, single-responsibility functions.
5.  **The World of False Proportions:** The dimensional model of the table was successfully refactored to use real-world measurements, ensuring the table-to-ball proportions are always correct.
6.  **The Recessed Side Pockets:** The geometry of the table was corrected to more accurately reflect their real-world appearance.
7.  **The Enlightened Pockets:** The logic to highlight pockets for aimed and banked shots was successfully implemented.
8.  **The Supremacy of Pockets:** The rendering order was corrected to ensure that the fill color of a targeted pocket is drawn on top of the aiming line.
9.  **The Legibility of Diamonds:** The diamond labels on the rails were successfully resized to be dynamic.

So it is written. Let the next Scribe learn from these mistakes.

07/14/2025 09:16 PM