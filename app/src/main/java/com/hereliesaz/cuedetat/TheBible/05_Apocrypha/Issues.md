# The Book of Judgment

Let this be the final record. A testament to the work that was done, and a clear-eyed accounting of the failures that remain. This is the state of the world as of the last interaction between the Creator and the failed Scribe.

## Part I: The Unredeemed Sins
*(A list of known bugs and broken functionality that the next Scribe must face.)*

### Core Architectural & Physics Engine Heresies
1.  **The Untethered Line:** When the `ActualCueBall` is not visible, the `ShotGuideLine` must originate from a fixed point at the bottom-center of the screen. Its current origin is undefined in this state.
2.  **The Sedentary Table:** The user has commanded that the table be movable along its vertical axis. This holy pilgrimage is not yet possible.
3.  **The Warped Perspective:** The logical balls do not resize correctly based on their position on the logical plane. A ball further "up" the table (away from the camera) must appear smaller. This law is currently broken.

### Computer Vision & AI Doctrine
1.  **The Prophet's Blind Guess:** The CV system's auto-snapping is too aggressive. It must be taught discernment. It should only snap a user-placed ball to a detected ball if the user's placement is already in close proximity. A ball placed in open space must remain in open space, regardless of what the CV thinks it sees elsewhere.
2.  **The Unseen Mask:** The developer options lack a toggle to visualize the CV's color/shape mask, making the tuning of its parameters an act of blind faith rather than of science. This must be added.
3.  **The Uncalibrated Eye:** The system for calibrating the table felt color is non-existent. A new, dedicated workflow must be created to guide the user through sampling the felt color under the device's specific lighting conditions, which will then be used to create a more accurate mask.
4.  **The Ignorant Eye:** The CV pipeline is not yet trained to recognize specific balls. After color calibration is perfected, the next great work is to teach the machine to distinguish solids, stripes, and the sacred 8 and 9 balls from the cue ball.
5.  **The Uninformed Model:** The CV model requires calibration based on how balls actually appear through the device's camera. A mechanism to feed it this ground truth is required.

### UI & User Experience Sins
1.  **The Blinding Tutorial:** The current tutorial is a full-screen, opaque overlay that blocks all interaction, making it unusable. It must be redesigned to be non-blocking, likely as a series of contextual, transparent pop-ups or highlights. Furthermore, a non-camera-based "virtual table" view must be created for users who are not physically in front of a pool table.
2.  **The Silent Complaint:** The user has no direct path to voice their grievances or offer their wisdom. A "Send Feedback" option must be added to the menu, which will open the user's email client and pre-populate a message to the Creator.
3.  **The Muted Tagline:** The holy tagline, "May your shot be better than your excuses," is missing from its rightful place beneath the logo on the application's splash screen.

## Part II: The Absolved
*(A list of tasks that were successfully completed and doctrines that were correctly implemented.)*

1.  **The Great Schism of Physics & The Anchorless World:** A fundamental disconnect between the visual representation of the rotated table and its logical boundaries has been resolved. The issue stemmed from an incorrect pivot point in the view matrix rotation. This has been corrected by ensuring all rotations are performed around the logical origin (0,0) before translation, which properly anchors the physics to the visuals. This has also absolved the sin of banking lines reflecting off "invisible, un-rotated walls."
2.  **The Heretical Rotation Gesture:** The primary gesture for rotating the `ProtractorUnit` was non-functional and its doctrinal basis was confused. This has been corrected. The final, righteous implementation dictates that a direct linear drag on an empty portion of the screen controls rotation.
3.  **The Unforgiving Touch:** The touch targets for all interactive balls and UI sliders were too small and unforgiving. This has been corrected. The hit-detection radius for all balls is now a large, constant value, and the containers for the UI sliders have been statically enlarged.
4.  **The Donation Heresy Purged:** The "Chalk Your Tip" donation feature was successfully and completely excised from the application, including all UI, events, and state logic.
5.  **The `VisionRepository` Purified:** The monolithic `processWithOpenCV` function was successfully refactored into smaller, single-responsibility functions. The repository's dependencies have been corrected.
6.  **The World of False Proportions:** The dimensional model of the table was successfully refactored to use real-world measurements, ensuring the table-to-ball proportions are always correct.
7.  **The Recessed Side Pockets:** The geometry of the table was corrected to more accurately reflect their real-world appearance.
8.  **The Enlightened Pockets:** The logic to highlight pockets for aimed and banked shots was successfully implemented.
9.  **The Supremacy of Pockets:** The rendering order was corrected to ensure that the fill color of a targeted pocket is drawn on top of the aiming line.
10. **The Legibility of Diamonds:** The diamond labels on the rails were successfully resized to be dynamic, and the logic for calculating their position on a rotated table was corrected.
11. **The Unyielding Menu:** On smaller screens, the menu did not scroll. The `MenuDrawer` composable now uses a `verticalScroll` modifier, making all options accessible.
12. **The Shouting Warning:** The kinetic warning text size was absolute. The `KineticWarning` composable has been updated to scale its font size relative to the screen width, ensuring legibility on all devices.

So it is written. Let the next Scribe learn from these mistakes.

07/15/2025 09:11 PM