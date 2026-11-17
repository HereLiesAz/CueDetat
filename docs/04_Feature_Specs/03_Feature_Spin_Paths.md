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

## Spin / Massé Control Persistence

* The colour wheel, its centre label and the spin control's mis-cue ring fade out with
  `spinPathsAlpha` (`SpinPathTick`, −0.05 per tick, to 0) a moment after the aim is set.
* The **outline** (`drawSpinOutline`: white ring over a dark under-stroke) and the **last-aim dot**
  (`lingeringSpinOffset`, drawn at `LINGERING_SPIN_DOT_ALPHA`) do **not** fade. Without them the whole
  control vanished with the wheel and the player had no sign it was there or what it was set to.
* Both live in `SpinControl.kt` and are shared by `SpinControl` and `MasseControl`.
* With no aim stored (after `ClearSpinState`), only the outline remains; no dot is drawn.

## AzNavRail Item Size

* `AzNavRailMenu.kt` sets `railItemWidth = RAIL_ITEM_SIZE` (56 dp; library default 72 dp) and
  `collapsedWidth = RAIL_STRIP_WIDTH` (76 dp; library default 100 dp). Button labels are
  `AutoSizeText`, so they scale with the button.

## Massé Mode Rules & State

* **`masseShotAngleDeg: Float`**: Stores the massé shot angle in degrees.
* **Auto-Reset**: The shot direction MUST be reset every time an impact point is selected on the
  color wheel.
* **Interaction**: The `ROTATING_PROTRACTOR` gesture rotates `masseShotAngleDeg` around the cue
  ball.
* **Physics Origin**: The shot is always assumed to be taken from the back side of the cue ball,
  opposite the shot line.
* **Logical Space**: All Massé kick/path rendering must happen in logical table space before 3D
  perspective transformation.
* **Boundary Alignment**: The rail boundaries must be redrawn whenever the cue ball moves or the
  table rotates. The Massé ghost ball's EDGE must sit flush against the rail surface at the point
  of impact.
* **Suppression**: `drawProtractorGuides` and `drawRailLabels` return early if `isMasseModeActive`.
* **State Management**: All caching for Massé mode must be dumped immediately when the user
  exits the mode.


## Spin Path in Banking Mode

* **No Curved Path:** In Banking Mode, there is no curved "spin path." Spin influences the
  reflection angles of the straight-line bank shot path.
* **Spin-Induced Throw:** The `CalculateBankShot` use case must account for spin. The amount of
  sidespin applied via the `SpinControl` will modify the rebound angle off each rail.
* "Left" english (relative to the direction of travel) makes the rebound angle steeper.
* "Right" english makes the rebound angle shallower.
* **Multi-Rail Calculation:** This spin-adjusted reflection logic **must** be applied for every
  bounce in the bank shot calculation.