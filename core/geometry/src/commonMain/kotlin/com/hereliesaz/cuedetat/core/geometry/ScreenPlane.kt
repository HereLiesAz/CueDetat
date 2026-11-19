package com.hereliesaz.cuedetat.core.geometry

import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.meters

/**
 * A table drawn **portrait** on a screen, in arbitrary square units, and how its points cross into
 * the metric table plane ([Vec2]): `x` runs along the short axis toward the viewer's right, `y`
 * runs along the **long** axis toward the viewer's bottom (so `y` is *down*).
 *
 * The table plane is long-axis-`x`, short-axis-`y`, right-handed and metric. The crossing is
 *
 * ```
 * table.x = screen.y / unitsPerMeter      // long axis  <- screen y
 * table.y = screen.x / unitsPerMeter      // short axis <- screen x
 * ```
 *
 * which looks like a bare swap but is a proper rotation once the screen's own handedness is
 * counted: a screen with `y` down is a reflection of the usual right-handed view from above, and a
 * swap is a reflection too, so the two cancel. What that buys, and the tests pin: a counter-clockwise
 * turn *as the viewer sees it* is a counter-clockwise turn in the table plane, so the sign of a
 * cut, throw or english means the same thing on both sides. (Screen right -> table `+y`, screen up
 * -> table `-x`, and right -> up is a counter-clockwise quarter turn in both.)
 *
 * The map is linear, so vectors (directions, offsets) cross with the same formulas as points. It is
 * its own inverse up to the scale.
 *
 * Pure and dependency-free so it can be tested anywhere; the app's `PointF` adapter is
 * `domain/core/TableFrame`. Callers choose [unitsPerMeter]; the app's is the legacy logical plane's
 * (about 874.9).
 */
class ScreenPlane(val unitsPerMeter: Double) {

    init {
        require(unitsPerMeter > 0.0 && unitsPerMeter.isFinite()) { "unitsPerMeter must be positive and finite: $unitsPerMeter" }
    }

    /** A screen-plane point or vector as a table-plane one. */
    fun toTable(x: Double, y: Double): Vec2 = Vec2(y / unitsPerMeter, x / unitsPerMeter)

    /** The screen-plane `x` of a table-plane point or vector. */
    fun screenX(point: Vec2): Double = point.y * unitsPerMeter

    /** The screen-plane `y` of a table-plane point or vector. */
    fun screenY(point: Vec2): Double = point.x * unitsPerMeter

    /** A length in screen units as a real length. */
    fun length(units: Double): Length = (units / unitsPerMeter).meters

    /** A real length in screen units. */
    fun units(length: Length): Double = length.meters * unitsPerMeter
}
