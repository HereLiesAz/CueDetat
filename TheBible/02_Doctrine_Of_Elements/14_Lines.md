# The Lines

* **Aiming Line:** Drawn from the Ghost Cue Ball's center through the Target Ball's center. It terminates at the first rail it hits (if the table is visible) or a pocket edge.
* **Shot Guide Line:** Drawn from the Actual Cue Ball's center (or a default anchor if hidden) through the Ghost Cue Ball's center, extending to infinity. This line **must always be visible** in Protractor Mode, regardless of the Actual Cue Ball's visibility. When the `ActualCueBall` is not visible, the line's origin must be anchored to the bottom-center of the screen.
* **Visibility:** Line visibility is tied to the current mode. Text labels for lines are controlled by the "WTF is all this?" toggle.

***
## Addendum: Detailed Line Specifications

* **Aiming Line** (Protractor Mode)
* **Path**: Drawn from the `GhostCueBall` center towards the `TargetBall` center.
* **Termination**: It terminates at the first point of contact, be it a pocket edge or a rail.
* **Color**: If the line terminates at a pocket, it must turn `RebelYellow`.
* **Banking Preview**:
  * When the table is visible and the direct aiming line does **not** terminate in a pocket, a single bank reflection shall be calculated and drawn.
  * The banked segment must inherit the color of the primary aiming line, but with a reduced alpha to distinguish it. If the path leads to a pocket, the banked segment turns `RebelYellow`.
  * A diamond number label must be rendered at the point of impact on the rail. This label is not considered "help text" and must always be visible when its line is visible. It is rendered on the same 3D plane as the rails themselves.

* **Shot Guide Line** (Protractor Mode)
* **Path**: Drawn from the `shotLineAnchor` point through the `GhostCueBall` center.
* **Banking Preview**: When the table is visible, the impact point where this line *would* strike a rail is calculated. A diamond number label must be drawn at this point.
* **Style**: A solid line. Its color must change to a warning red when an impossible shot is detected. Its color is **not** affected by aiming at a pocket.
* **Label**: "Shot Guide Line".

* **Tangent Lines** (Protractor Mode)
* **Path**: Two rays originating from the `GhostCueBall` center, drawn perpendicular to the Aiming Line.
* **Style**: The line indicating the cue ball's deflection path is solid; the other is dotted. Its banking preview behaves identically to the Aiming Line's.
* **Label**: "Tangent Line".

* **Fixed Angle Guides** (Protractor Mode)
* **Path**: Lines radiating from the `GhostCueBall`'s center, drawn in **logical space** so they are affected by perspective.
* **Angles**: Must be drawn at 14°, 30°, 43°, and 48°.
* **Label**: The degree value (e.g., "30°"), placed at the end of each respective line.

* **Banking Shot Line** (Banking Mode)
* **Path**: A multi-segment line originating from the `ActualCueBall` (Banking Ball), and aimed at the `bankingAimTarget`, reflecting off the rails up to 4 times.
* **Style**: Solid lines, colored in a progressive, decaying sequence of yellows.
* **Termination**: If the final segment of the path collides with a pocket's geometry, line rendering must cease at the pocket's edge. The final segment leading to the pocket must be colored `RebelYellow`.
* **Label**: A diamond number label must be rendered at each rail impact point.

* **Spin Paths** (Protractor Mode)
* **Purpose**: To visualize the cue ball's post-contact trajectory when English is applied.
* **Default State (The Ribbon of Fate)**: By default, without user interaction, a full spectrum of color-coded paths **must** be displayed. These paths branch from the tangent line and show a wide range of possible outcomes based on spin. The color of each path corresponds to its position on the `SpinControl` wheel.
* **Interactive State**: When the user touches and drags on the `SpinControl` wheel, the ribbon disappears and is replaced by a single, precise path that corresponds to the exact point of their finger.
* **Linger & Fade**: When the user releases their finger, this single interactive path and its corresponding selection indicator on the wheel **must linger** for 5 seconds, then fade to invisibility over the next 5 seconds. Upon completion, the default "Ribbon of Fate" must reappear.