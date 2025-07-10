# The Lines

*   **Aiming Line:** Drawn from the Ghost Cue Ball's center through the Target Ball's center, extending to infinity.
*   **Shot Line:** Drawn from the Actual Cue Ball's center (or a default anchor if hidden) through the Ghost Cue Ball's center, extending to infinity. This line **must always be visible** in Protractor Mode, regardless of the Actual Cue Ball's visibility.
*   **Visibility:** Line visibility is tied to the current mode. Text labels for lines are controlled by the "WTF is all this?" toggle.

***
## Addendum: Detailed Line Specifications

*   **Aiming Line** (Protractor Mode)
*   **Path**: Drawn from the `GhostCueBall` center through the `TargetBall` center.
*   **Banking Preview**: When the table is visible, this line will calculate a single reflection off the first rail it intersects. The reflected path will be drawn. If this banked path aims at a pocket, the reflected segment must turn white.
*   **Style**: A solid, prominent color.
*   **Label**: "Aiming Line".

*   **Shot Line** (Protractor Mode)
*   **Path**: Drawn from the `shotLineAnchor` point through the `GhostCueBall` center.
*   **Banking Preview**: When the table is visible, the impact point where this line *would* strike a rail is calculated. A diamond number label must be drawn at this point, but the line itself does not reflect.
*   **Style**: A solid line. Its color must change to a warning red when an impossible shot is detected. Its color is **not** affected by aiming at a pocket.
*   **Label**: "Shot Guide Line".

*   **Tangent Lines** (Protractor Mode)
*   **Path**: Two rays originating from the `GhostCueBall` center, drawn perpendicular to the Aiming Line.
*   **Style**: The line indicating the cue ball's deflection path is solid; the other is dotted.
*   **Label**: "Tangent Line".

*   **Fixed Angle Guides** (Protractor Mode)
*   **Path**: Lines radiating from the `GhostCueBall`'s center, drawn in **logical space** so they are affected by perspective.
*   **Angles**: Must be drawn at 14°, 30°, 43°, and 48°.
*   **Label**: The degree value (e.g., "30°"), placed at the end of each respective line.

*   **Banking Shot Line** (Banking Mode)
*   **Path**: A multi-segment line originating from the `ActualCueBall` (Banking Ball), and aimed at the `bankingAimTarget`, reflecting off the rails up to 4 times.
*   **Style**: Solid lines, colored in a progressive, decaying sequence of yellows.
*   **Termination**: If the path collides with a pocket, line rendering must cease at the pocket's edge. The final segment leading to the pocket must be colored white.
*   **Label**: Each segment is labeled sequentially, e.g., "Bank 1", "Bank 2".

## Addendum: Spin/English Visualization (Future)

*   **UI Control**: A color wheel UI element will allow the user to select a strike point on the cue ball.
*   **Path Visualization**: When a spin is selected, the application will draw a set of potential cue ball paths post-impact.
*   **Color Coding**: Each potential path line must be color-coded to correspond directly with the color of its respective strike spot on the color wheel key.