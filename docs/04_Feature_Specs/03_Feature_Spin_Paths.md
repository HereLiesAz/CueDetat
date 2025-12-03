# 4.3. Feature Specification: Spin Paths

This document defines the behavior of spin path visualizations.

## Spin Path in Protractor Mode

* **Curve:** The spin path is a Bézier curve showing the cue ball's trajectory after contacting the
  object ball.
* **Single Bank Requirement:** When the table is visible, this curve **must** be calculated to show
  a single reflection off a rail.
* The system detects the first intersection point of the curve with any rail.
* The curve is truncated at this point.
* A single, straight-line reflection vector is calculated and drawn from the intersection point.

## Spin Path in Banking Mode

* **No Curved Path:** In Banking Mode, there is no curved "spin path." Spin influences the
  reflection angles of the straight-line bank shot path.
* **Spin-Induced Throw:** The `CalculateBankShot` use case must account for spin. The amount of
  sidespin applied via the `SpinControl` will modify the rebound angle off each rail.
* "Left" english (relative to the direction of travel) makes the rebound angle steeper.
* "Right" english makes the rebound angle shallower.
* **Multi-Rail Calculation:** This spin-adjusted reflection logic **must** be applied for every
  bounce in the bank shot calculation.