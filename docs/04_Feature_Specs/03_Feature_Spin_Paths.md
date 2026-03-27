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

## Spin Control Input Coordinates

* **`SpinApplied` event**: Dispatches the raw canvas pixel coordinates as `PointF(offset.x, offset.y)`. It does **not** pre-normalize to a −1..1 range before sending. Normalization is handled downstream.
* **`drawLogicalIndicator`** in `SpinControl.kt`: The indicator position uses `Offset(offset.x, offset.y)` directly from the stored raw pixel coords. It does **not** multiply by the radius again (that was a double-application bug that has been fixed).
* `MasseControl.kt` already used raw coordinates correctly and was not changed.

## Massé Mode Direction State

* **`masseShotAngleDeg: Float`**: A new field in `CueDetatState` (default `0f`) that stores the massé shot angle in degrees.
* On **`ToggleMasseMode` enable**: `lingeringSpinOffset` and `selectedSpinOffset` are cleared; `masseShotAngleDeg` is initialized from the current shot geometry.
* On **`ToggleMasseMode` disable**: `masseShotAngleDeg` is reset to `0f`.
* **`ROTATING_PROTRACTOR` gesture**: When `isMasseModeActive`, the rotation gesture rotates `masseShotAngleDeg` around the cue ball (not the target ball).
* **Massé physics** use `Math.toRadians(state.masseShotAngleDeg)` directly for the shot angle instead of deriving it from the ghost ball position.
* **`drawProtractorGuides`** returns early if `isMasseModeActive` (guides are suppressed during massé).
* **`drawRailLabels`** returns early if `isMasseModeActive` (rail labels are suppressed during massé).

## Spin Path in Banking Mode

* **No Curved Path:** In Banking Mode, there is no curved "spin path." Spin influences the
  reflection angles of the straight-line bank shot path.
* **Spin-Induced Throw:** The `CalculateBankShot` use case must account for spin. The amount of
  sidespin applied via the `SpinControl` will modify the rebound angle off each rail.
* "Left" english (relative to the direction of travel) makes the rebound angle steeper.
* "Right" english makes the rebound angle shallower.
* **Multi-Rail Calculation:** This spin-adjusted reflection logic **must** be applied for every
  bounce in the bank shot calculation.