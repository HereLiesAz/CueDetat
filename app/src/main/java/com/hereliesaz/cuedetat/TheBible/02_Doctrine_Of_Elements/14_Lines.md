# The Lines

*   **Aiming Line:** Drawn from the Ghost Cue Ball's center through the Target Ball's center, extending to infinity.
*   **Shot Line:** Drawn from the Actual Cue Ball's center (or a default anchor if hidden) through the Ghost Cue Ball's center, extending to infinity. This line **must always be visible** in Protractor Mode, regardless of the Actual Cue Ball's visibility.
*   **Visibility:** Line visibility is tied to the current mode. Text labels for lines are controlled by the "WTF is all this?" toggle.

***
## Addendum: Detailed Line Specifications

*   **Aiming Line** (Protractor Mode)
  *   **Path**: Drawn from the `GhostCueBall` center through the `TargetBall` center, extending to the edge of the view.
  *   **Style**: A solid, prominent color (e.g., `AccentGold`) to distinguish it as the primary aiming path.
  *   **Label**: "Aiming Line", placed along the line, outside the `TargetBall`.

*   **Shot Line** (Protractor Mode)
  *   **Path**: Drawn from the `shotLineAnchor` point (which corresponds to the `ActualCueBall`'s center if visible, or a default anchor below the screen if hidden) through the `GhostCueBall` center, extending to the edge of the view.
  *   **Style**: A solid line. Its color must change to a warning red when an impossible shot is detected.
  *   **Label**: "Shot Line", placed along the line.

*   **Tangent Lines** (Protractor Mode)
  *   **Path**: Two rays originating from the `GhostCueBall` center, drawn perpendicular to the Aiming Line and extending to the edge of the view.
  *   **Style**: The line indicating the cue ball's deflection path is solid; the other is dotted. The solid side is determined by the quadrant the `ShotGuideLine` occupies relative to the `AimingLine`.
  *   **Label**: "Tangent Line", placed along both lines.

*   **Fixed Angle Guides** (Protractor Mode)
  *   **Path**: Lines radiating from the `TargetBall`'s center, drawn in screen-space so they are not affected by perspective.
  *   **Style**: Subtle, thin lines.
  *   **Angles**: Must be drawn at 14°, 30°, 43°, and 48°.
  *   **Label**: The degree value (e.g., "30°"), placed at the end of each respective line.

*   **Banking Shot Line** (Banking Mode)
  *   **Path**: A multi-segment line confined to the logical table boundaries, originating from the `ActualCueBall` (Banking Ball), and aimed at the `bankingAimTarget`. It reflects off the rails up to 4 times.
  *   **Style**: Solid lines. The segments must be colored in a progressive, decaying sequence of yellows, from brightest at the first segment to most muted at the last.
  *   **Termination**: If the calculated path collides with a pocket, line rendering must cease at the pocket's edge. The final segment leading to the pocket must be colored white.
  *   **Label**: Each segment is labeled sequentially, e.g., "Bank 1", "Bank 2".

## Addendum: Spin/English Visualization (Future)

This feature pertains to the visualization of cue ball paths resulting from applied spin (english).

*   **UI Control**: A color wheel UI element will allow the user to select a strike point on the cue ball. White center for no spin, other areas for top, bottom, left, right, etc.
*   **Path Visualization**: When a spin is selected, the application will draw a set of potential cue ball paths post-impact.
*   **Color Coding**: Each potential path line must be color-coded to correspond directly with the color of its respective strike spot on the color wheel key, creating a clear visual key between input and resulting path.