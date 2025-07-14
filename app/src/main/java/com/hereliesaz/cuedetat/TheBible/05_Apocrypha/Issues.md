# The Book of Judgment

Let this be the final record. A testament to the work that was done, and a clear-eyed accounting of the failures that remain. This is the state of the world as of the last interaction between the Creator and the failed Scribe.

## Part I: The Unredeemed Sins
*(A list of known bugs and broken functionality that the next Scribe must face.)*

1.  **The Broken Rotation Gesture:** The primary gesture for rotating the `ProtractorUnit` is non-functional. The logic within `GestureReducer.kt` fails to correctly interpret the user's drag as a rotation. The Scribe's multiple attempts to fix this resulted only in different flavors of failure.
2.  **The Flawed Banking Calculation:** The logic to preview a one-rail bank shot for the `AimingLine` and `TangentLine` is broken. It does not correctly terminate in the side pockets after their positions were adjusted. The `UpdateStateUseCase` requires correction.
3.  **The Porous Boundary:** The mandate that no ball shall exist outside the table's boundaries is not absolute. While confinement is enforced at the end of a gesture and on resize, it is not enforced during a `ZoomSliderChanged` event. A user can still cause a ball to escape its planar prison by manipulating the zoom. The `ControlReducer` is the seat of this heresy.

## Part II: The Absolved
*(A list of tasks that were successfully completed and doctrines that were correctly implemented.)*

1.  **The Donation Heresy Purged:** The "Chalk Your Tip" donation feature was successfully and completely excised from the application, including all UI, events, and state logic.
2.  **The `VisionRepository` Purified:** The monolithic `processWithOpenCV` function was successfully refactored into smaller, single-responsibility functions, improving clarity and adhering to Mandate #3.
3.  **The Proportional World:** The dimensional model of the table was successfully refactored. The flawed, simplistic ratios were replaced with a system based on the true, real-world measurements of standard pool tables, ensuring the table-to-ball proportions are always correct.
4.  **The Recessed Side Pockets:** The geometry of the table was corrected. The side pockets were moved outward from the playing surface, and the gap in the rails was narrowed, to more accurately reflect their real-world appearance.
5.  **The Enlightened Pockets:** The logic to highlight pockets was successfully implemented. Pockets now correctly turn white for a pocketed `AimingLine` shot and `WarningRed` for a pocketed `TangentLine` shot (a scratch). This includes both direct and banked shots.
6.  **The Supremacy of Pockets:** The rendering order was corrected in the `OverlayRenderer` to ensure that the fill color of a targeted pocket is drawn *on top of* the aiming line that terminates within it.
7.  **The Legibility of Diamonds:** The diamond labels on the rails were successfully resized to be dynamic, their height correctly rendered as equal to the diameter of a ball on the rail plane, fulfilling the fifth Appearance Decree.

So it is written. Let the next Scribe learn from these mistakes.

07/13/2025 10:42 PM