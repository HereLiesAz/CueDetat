# The Book of Judgment

Let this be the final record. A testament to the work that was done, and a clear-eyed accounting of the failures that remain. This is the state of the world as of the last interaction between the Creator and the failed Scribe.

## Part I: The Unredeemed Sins
*(A list of known bugs and broken functionality that the next Scribe must face.)*

1.  **The Great Schism of Physics:** Despite numerous corrections, a fundamental disconnect remains between the visual representation of the rotated table and its logical, physical boundaries. Balls can be placed outside the table, and banking lines reflect off invisible, un-rotated walls. The core logic of confinement and intersection, though appearing doctrinally sound, is functionally heretical. The Scribe has failed to resolve this and can proceed no further.

## Part II: The Absolved
*(A list of tasks that were successfully completed and doctrines that were correctly implemented.)*

1.  **The Heretical Rotation Gesture:** The primary gesture for rotating the `ProtractorUnit` was non-functional and its doctrinal basis was confused. This has been corrected. The final, righteous implementation dictates that a direct linear drag on an empty portion of the screen controls rotation.
2.  **The Unforgiving Touch:** The touch targets for all interactive balls and UI sliders were too small and unforgiving. This has been corrected. The hit-detection radius for all balls is now a large, constant value, and the containers for the UI sliders have been statically enlarged.
3.  **The Donation Heresy Purged:** The "Chalk Your Tip" donation feature was successfully and completely excised from the application, including all UI, events, and state logic.
4.  **The `VisionRepository` Purified:** The monolithic `processWithOpenCV` function was successfully refactored into smaller, single-responsibility functions. The repository's dependencies have been corrected.
5.  **The World of False Proportions:** The dimensional model of the table was successfully refactored to use real-world measurements, ensuring the table-to-ball proportions are always correct.
6.  **The Recessed Side Pockets:** The geometry of the table was corrected to more accurately reflect their real-world appearance.
7.  **The Enlightened Pockets:** The logic to highlight pockets for aimed and banked shots was successfully implemented.
8.  **The Supremacy of Pockets:** The rendering order was corrected to ensure that the fill color of a targeted pocket is drawn on top of the aiming line.
9.  **The Legibility of Diamonds:** The diamond labels on the rails were successfully resized to be dynamic.
10. **The Unyielding Menu:** On smaller screens, the menu did not scroll. The `MenuDrawer` composable now uses a `verticalScroll` modifier, making all options accessible.
11. **The Shouting Warning:** The kinetic warning text size was absolute. The `KineticWarning` composable has been updated to scale its font size relative to the screen width, ensuring legibility on all devices.

So it is written. Let the next Scribe learn from these mistakes.

07/14/2025 11:32 PM