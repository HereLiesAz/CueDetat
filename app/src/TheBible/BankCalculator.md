# 20: Bank Calculator (Future)

* This file pertains to the banking mode functionality from the roadmap.
* The geometric reflection logic needs to be implemented to calculate multi-rail bank and kick shots.
* This logic should consider pocket geometry for line termination and shot success/failure.

***
## Addendum: Implementation Details

* **Line Rendering**: The calculated bank shot path is a multi-segment line. It originates from the `ActualCueBall` (Banking Ball) and aims toward the `bankingAimTarget`.
* **Reflection Logic**: The line reflects off the logical table boundaries up to a maximum of 3 times. The reflection calculation must be pure geometric vector reflection.
* **Visual Style**:
    *   Each segment of the reflected path may be rendered in a different color to enhance clarity.
    *   When the final segment of the calculated path intersects with a pocket's geometry, that segment must change its color to white to indicate a successful shot, and the line rendering must terminate at the pocket's center.
* **Labeling**: Each segment must be labeled sequentially (e.g., "Bank 1", "Bank 2", "Bank 3") if help text is enabled.