--- FILE: app/src/main/java/com/hereliesaz/cuedetat/TheBible/02_Doctrine_Of_Elements/14_Lines.md ---
# The Lines

*   **Aiming Line:** Drawn from the Ghost Cue Ball's center through the Target Ball's center. It terminates at the first rail it hits (if the table is visible) or a pocket edge.
*   **Shot Line:** Drawn from the Actual Cue Ball's center (or a default anchor if hidden) through the Ghost Cue Ball's center, extending to infinity. This line **must always be visible** in Protractor Mode, regardless of the Actual Cue Ball's visibility.
*   **Visibility:** Line visibility is tied to the current mode. Text labels for lines are controlled by the "WTF is all this?" toggle.

***
## Addendum: Detailed Line Specifications

*   **Aiming Line** (Protractor Mode)
*   **Path**: Drawn from the `GhostCueBall` center towards the `TargetBall` center.
*   **Termination**: It terminates at the first point of contact, be it a pocket edge or a rail.
*   **Color**: If the line terminates at a pocket, it must turn `RebelYellow`.
*   **Banking Preview**:
    *   When the table is visible and the direct aiming line does **not** terminate in a pocket, a single bank reflection shall be calculated and drawn.
    *   The first segment (to the rail) uses the standard `AimingLine` style.
    *   The second, reflected segment uses the styles from `BankLine3.kt`. If this reflected path aims at a pocket, it must turn `RebelYellow` and terminate at the pocket's edge.
    *   A diamond number label must be rendered at the point of impact on the rail. This label is not considered "help text" and must always be visible.
*   **Label**: "Aiming Line".

*   **Shot Guide Line** (Protractor Mode)
*   **Path**: Drawn from the `shotLineAnchor` point through the `GhostCueBall` center.
*   **Banking Preview**: When the table is visible, the impact point where this line *would* strike a rail is calculated. A diamond number label must be drawn at this point.
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
*   **Termination**: If the path collides with a pocket, line rendering must cease at the pocket's edge. The final segment leading to the pocket must be colored `RebelYellow`.
*   **Label**: A diamond number label must be rendered at each rail impact point.