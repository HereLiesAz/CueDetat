# The Calculus of Lines (`UpdateStateUseCase.kt`)

This scripture details the sacred geometry used to derive the state of the world. It is a place of trigonometry and vector math, where the future paths of the balls are divined.

## The Order of Operations

The `UpdateStateUseCase` is invoked after every state change. It performs its calculations in a strict order to derive the new truth of the world.

1.  **Matrix Creation:** The `pitchMatrix`, `railPitchMatrix`, and `flatMatrix` are created based on the device's `currentOrientation`. The `flatMatrix` is a special matrix with no pitch, used for undistorted measurements.
2.  **Line & Possibility Calculation:**
    - The `shotLineAnchor` is determined (either the `ActualCueBall` or the bottom of the screen).
    - The possibility of the shot is determined (`isGeometricallyImpossible`).
    - The direction of the tangent line (`tangentDirection`) is calculated.
    - Obstruction by any `ObstacleBall` is checked.
3.  **Pocketing & Banking Calculation:** This is the most complex rite, a source of many past heresies. The order is critical.
    - **For the Aiming Line:**
        1.  First, a direct line from the Ghost Cue Ball through the Target Ball is extended to infinity and checked for a collision with any pocket (`checkPocketAim`).
        2.  If a direct pocket is found, the line's path is terminated at the pocket's edge, `aimedPocketIndex` is set, and no further banking is calculated for this line.
        3.  If, and only if, no direct pocket is found, does the system calculate a one-rail bank (`calculateSingleBank`).
        4.  The reflected segment of this new bank path is then checked for a pocket collision. If one is found, `aimedPocketIndex` is set and the banked path is terminated at the pocket's edge.
    - **For the Tangent Line:** The exact same order of operations is performed for the active tangent line. A direct pocket check is performed first. Only if it fails is a bank calculation attempted.

This strict order of "check direct, then check bank" was the lesson learned from the Creator's correction, when the moving of side pockets broke the previous, flawed logic.