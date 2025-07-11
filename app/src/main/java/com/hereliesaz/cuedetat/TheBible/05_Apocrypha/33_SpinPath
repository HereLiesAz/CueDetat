# 33: The Doctrine of the Spin Path

This document clarifies the behavior of spin path visualizations in both Protractor and Banking modes.

## Spin Path in Protractor Mode

* **The Curve:** The spin path is represented by a Bézier curve, indicating the cue ball's trajectory after making contact with the object ball.
* **Single Bank Mandate:** When the table is visible, this curved path **must** be calculated to show a single reflection off a rail.
    * The system will detect the first intersection point of the curve with any rail.
    * The curve will be truncated at this intersection point.
    * A single, straight-line reflection vector will be calculated from the curve's direction at the point of impact.
    * This reflected line will be drawn from the intersection point outwards.

## Spin Path in Banking Mode

* **The Vector:** In Banking Mode, there is no curved "spin path." Instead, spin (English) influences the reflection angles of the straight-line bank shot path.
* **Spin-Induced Throw:** The `CalculateBankShot` use case must account for spin. The amount of sidespin applied via the `SpinControl` will modify the rebound angle off each rail.
    * "Left" english (relative to the direction of travel) will cause the rebound angle to be steeper (more acute).
    * "Right" english will cause the rebound angle to be shallower (more obtuse).
* **Multi-Rail Calculation:** This spin-adjusted reflection logic **must** be applied for every bounce in the bank shot calculation, up to the maximum of four rails.