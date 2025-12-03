# 2.5. Line Components

This document defines the lines used for aiming and visualization.

* **Shot Guide Line:** Drawn from the `shotLineAnchor` (either the `ActualCueBall` or a calculated
  screen-bottom point) through the `GhostCueBall`'s center. It is always visible in Protractor Mode.
* **Color Rule:** The line's color must change to `WarningRed` when any warning condition (
  `isGeometricallyImpossible`, `isTiltBeyondLimit`, `isObstructed`) is true.

* **Tangent Lines:** Originate from the `GhostCueBall`.
* **Straight Shot Rule:** When the shot is straight, both tangent lines must be rendered in their
  inactive, dotted state.
* **Pocketing Rule:** When the active tangent line's path leads to a pocket, its color must change
  to `WarningRed`.

* **Fading Logic:** All lines that extend into open space (`AimingLine`, `ShotGuideLine`,
  `TangentLine`, `ProtractorGuides`, and the final segment of a non-pocketed `BankShotLine`) must
  have a finite length of **two table lengths**. They must begin to fade at **1.2 table lengths**
  and be fully transparent at their end. This calculation must use the table's logical dimensions,
  regardless of whether the table is currently visible.