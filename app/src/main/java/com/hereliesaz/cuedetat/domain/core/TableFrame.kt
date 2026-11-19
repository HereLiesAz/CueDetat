package com.hereliesaz.cuedetat.domain.core

import android.graphics.PointF
import com.hereliesaz.cuedetat.core.geometry.ScreenPlane
import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.domain.TableFrameHomography

/**
 * How the app's legacy logical plane maps onto the metric table plane, in one place.
 *
 * **The scale** is [TableFrameHomography]'s (the AR code's), widened to `Double`, so there is one
 * source and not two. It is defined by the plane's own convention, `(LOGICAL_BALL_RADIUS * 2)` over a
 * 2.25 in ball; the widening carries the `Float` constant's rounding (about 4e-8 relative, a tenth of a
 * micron across a nine-foot table).
 *
 * **The axes**: the app's table is portrait with its long axis on logical **y** and `y` pointing down
 * the screen. The core's is long-axis-`x`, right-handed. The crossing is [ScreenPlane]'s, which
 * documents why the swap it amounts to preserves handedness, and which is tested in the core, where it
 * runs without an Android SDK. This object only puts `PointF` on it.
 *
 * Until now the bridge crossed with `(x, -y)` and no swap, which left the long axis on core `y`. That
 * was harmless for the aim solver, which is rotation-invariant, and wrong for anything that uses the
 * core `Table` (pockets, cushions), which is the whole point of the advisor.
 */
object TableFrame {

    /** Logical units per inch. */
    val LOGICAL_UNITS_PER_INCH: Double = TableFrameHomography.LOGICAL_UNITS_PER_INCH.toDouble()

    /** Logical units per metre. */
    val LOGICAL_UNITS_PER_METER: Double = TableFrameHomography.LOGICAL_UNITS_PER_METER.toDouble()

    private val plane = ScreenPlane(LOGICAL_UNITS_PER_METER)

    /** A logical point or vector as a metric table-plane one. */
    fun toCore(point: PointF): Vec2 = plane.toTable(point.x.toDouble(), point.y.toDouble())

    /** A metric table-plane point or vector back in logical coordinates. */
    fun toLogical(point: Vec2): PointF = PointF().apply {
        x = plane.screenX(point).toFloat()
        y = plane.screenY(point).toFloat()
    }

    /** A logical length as a real one. */
    fun toLength(logical: Float): Length = plane.length(logical.toDouble())

    /** A real length in logical units. */
    fun toLogical(length: Length): Float = plane.units(length).toFloat()

    /**
     * Index into the core [Table.pockets] of the pocket nearest a logical point. The app keeps its
     * corner pockets at the rail corners and its side pockets just past the rails, and its pocket
     * order differs from the core's, so a pocket known only by position (`Table.pockets`,
     * `TableScanModel.pockets[].logicalPosition`) is mapped by where it is.
     */
    fun corePocketIndex(table: Table, logical: PointF): Int = table.nearestPocketIndex(toCore(logical))
}
