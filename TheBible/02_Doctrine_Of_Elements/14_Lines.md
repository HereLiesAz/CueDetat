# The Lines

* **Shot Guide Line:** Drawn from the `shotLineAnchor` through the `GhostCueBall`'s center. This line **must always be visible** in Protractor Mode.
  * **Anchor Doctrine:** When the `ActualCueBall` is visible, it serves as the anchor. When hidden, the line **must** be anchored to the logical coordinate corresponding to the bottom-center of the screen.
  * **Color Doctrine:** The line's color must be `VioletResidue` (matching the `ActualCueBall`). It must change to `WarningRed` when any warning condition (`isGeometricallyImpossible`, `isTiltBeyondLimit`, `isObstructed`) is true.

* **Tangent Lines:** Originate from the `GhostCueBall`.
  * **Straight Shot Doctrine:** When the `ShotGuideLine` and `AimingLine` are collinear (a straight shot), both tangent lines **must** be rendered in their inactive, dotted state.
  * **Pocketing Doctrine:** When the active tangent line's path leads to a pocket, its color **must** change to `WarningRed`.

* **Fading to Nothingness:** All lines that do not terminate at a pocket or another ball (`AimingLine`, `ShotGuideLine`, `TangentLine`, `ProtractorGuides`, and the final segment of a non-pocketed `BankShotLine`) **must** have a finite length. Their length is defined as **two table lengths** from their origin. They must begin to fade at **1.2 table lengths** and be fully transparent at their end. This calculation must use the table's logical dimensions, regardless of whether the table is currently visible.