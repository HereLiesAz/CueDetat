package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

class MassePhysicsSimulatorTest {

    private val table = Table(size = TableSize.EIGHT_FT, isVisible = true)
    // Aim along +Y so abs(it.x) measures lateral curve perpendicular to the
    // shot line. masseShotAngleDeg is computed in production as atan2(dy, dx)
    // (angle from +X axis CCW), so π/2 = "shot pointing up the screen".
    private val shotAngle = (PI / 2).toFloat()

    @Test
    fun `center strike produces straight path`() {
        val result = MassePhysicsSimulator.simulate(
            contactOffset = pf(0f, 0f),
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = table
        )
        assertTrue("Must have path points", result.points.size >= 2)
        val maxDeviation = result.points.maxOf { pt ->
            abs(pt.x)
        }
        assertTrue("Center strike must produce near-straight path, maxDeviation=$maxDeviation",
            maxDeviation < 10f)
    }

    @Test
    fun `right contact offset curves ball to the right`() {
        val result = MassePhysicsSimulator.simulate(
            contactOffset = pf(1f, 0f),
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = table
        )
        val maxWorldX = result.points.drop(3).maxOf { abs(it.x) }
        assertTrue("Right strike must produce meaningful lateral curve, got maxX=$maxWorldX",
            maxWorldX > 5f)
    }

    @Test
    fun `left contact offset curves ball to the left`() {
        val result = MassePhysicsSimulator.simulate(
            contactOffset = pf(-1f, 0f),
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = table
        )
        val maxWorldX = result.points.drop(3).maxOf { abs(it.x) }
        assertTrue("Left strike must produce meaningful lateral curve, got maxX=$maxWorldX",
            maxWorldX > 5f)
    }

    @Test
    fun `right and left curves go opposite directions`() {
        val right = MassePhysicsSimulator.simulate(
            contactOffset = pf(1f, 0f), elevationDeg = 45f, shotAngle = shotAngle, table = table
        )
        val left = MassePhysicsSimulator.simulate(
            contactOffset = pf(-1f, 0f), elevationDeg = 45f, shotAngle = shotAngle, table = table
        )
        val rightMidX = right.points.getOrNull(right.points.size / 2)?.x ?: 0f
        val leftMidX = left.points.getOrNull(left.points.size / 2)?.x ?: 0f
        assertTrue("Right and left strikes must curve in opposite directions: right=$rightMidX, left=$leftMidX",
            rightMidX * leftMidX < 0f)
    }

    @Test
    fun `low elevation produces less curve than high elevation`() {
        // Use invisible table so rail-saturation can't make both values equal
        val noRailTable = Table(size = TableSize.EIGHT_FT, isVisible = false)
        fun curveAmount(elev: Float): Float {
            val result = MassePhysicsSimulator.simulate(
                contactOffset = pf(1f, 0f), elevationDeg = elev, shotAngle = shotAngle, table = noRailTable
            )
            return result.points.drop(3).maxOf { abs(it.x) }
        }
        val lowCurve = curveAmount(10f)
        val highCurve = curveAmount(70f)
        assertTrue("Higher elevation must produce more curve: low=$lowCurve, high=$highCurve",
            highCurve > lowCurve)
    }

    @Test
    fun `pocketIndex is null when table is invisible`() {
        val invisibleTable = Table(size = TableSize.EIGHT_FT, isVisible = false)
        val result = MassePhysicsSimulator.simulate(
            contactOffset = pf(0f, 0f),
            elevationDeg = 45f,
            shotAngle = shotAngle,
            table = invisibleTable
        )
        assertNull("With invisible table (no pockets), pocketIndex must be null", result.pocketIndex)
    }
}
