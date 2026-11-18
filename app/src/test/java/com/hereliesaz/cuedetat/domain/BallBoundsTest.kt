package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Table
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

/**
 * Balls stay wholly on the table. Expected numbers are worked by hand, not read from the code under
 * test: an 8 ft table is 44 x 88 in, a 2.25 in ball is 50 logical units across (radius 25), so one
 * inch is 50 / 2.25 = 22.222 units. Half the short side is 44 * 22.222 / 2 = 488.889; the ball's
 * centre may come no closer than its radius, so |x| <= 463.889. Half the long side is 977.778, so
 * |y| <= 952.778.
 */
class BallBoundsTest {

    private val maxX = 463.889f
    private val maxY = 952.778f
    private val eps = 0.01f

    private fun onTable(
        target: PointF = pf(0f, 0f),
        cue: PointF? = null,
        obstacles: List<OnPlaneBall> = emptyList(),
        visible: Boolean = true,
        rotation: Float = 0f,
        banking: Boolean = false,
        masse: Boolean = false,
    ) = CueDetatState().copy(
        table = Table(TableSize.EIGHT_FT, visible),
        protractorUnit = ProtractorUnit(target, LOGICAL_BALL_RADIUS, rotation),
        onPlaneBall = cue?.let { OnPlaneBall(it, LOGICAL_BALL_RADIUS) },
        obstacleBalls = obstacles,
        isBankingMode = banking,
        isMasseModeActive = masse,
    )

    @Test
    fun `a target ball dragged past a rail stops at it`() {
        val out = BallBounds.confine(onTable(target = pf(900f, 100f)))
        assertEquals(maxX, out.protractorUnit.center.x, eps)
        assertEquals(100f, out.protractorUnit.center.y, eps)
    }

    @Test
    fun `a ball in the corner is held on both axes`() {
        val out = BallBounds.confine(onTable(cue = pf(-9999f, 9999f)))
        assertEquals(-maxX, out.onPlaneBall!!.center.x, eps)
        assertEquals(maxY, out.onPlaneBall!!.center.y, eps)
    }

    @Test
    fun `balls already on the table are not touched`() {
        val state = onTable(target = pf(10f, 20f), cue = pf(-30f, 400f), obstacles = listOf(OnPlaneBall(pf(100f, -200f), 25f)))
        assertSame(state, BallBounds.confine(state))
    }

    @Test
    fun `only the obstacle that is off the table moves`() {
        val kept = OnPlaneBall(pf(100f, 100f), 25f)
        val off = OnPlaneBall(pf(0f, -5000f), 25f)
        val out = BallBounds.confine(onTable(obstacles = listOf(kept, off)))
        assertSame(kept, out.obstacleBalls[0])
        assertEquals(0f, out.obstacleBalls[1].center.x, eps)
        assertEquals(-maxY, out.obstacleBalls[1].center.y, eps)
    }

    @Test
    fun `a smaller ball may come closer to the rail by the difference in radius`() {
        // Half the short side is 488.889; a radius-10 ball's centre may reach 478.889.
        val out = BallBounds.confine(onTable(obstacles = listOf(OnPlaneBall(pf(9999f, 0f), 10f))))
        assertEquals(478.889f, out.obstacleBalls[0].center.x, eps)
    }

    @Test
    fun `with no visible table there are no bounds`() {
        val state = onTable(target = pf(9999f, 9999f), visible = false)
        assertSame(state, BallBounds.confine(state))
    }

    @Test
    fun `stateReducer applies it to whatever event comes through`() {
        // ToggleHelp touches no ball; the ball is outside only because the state was built so.
        val state = onTable(target = pf(900f, 0f))
        val out = stateReducer(state, MainScreenEvent.ToggleHelp, ReducerUtils(), GestureReducer())
        assertEquals(maxX, out.protractorUnit.center.x, eps)
    }

    @Test
    fun `containsBall counts the ball's radius, not just its centre`() {
        val table = Table(TableSize.EIGHT_FT, true)
        assertTrue(table.containsBall(pf(463f, 0f), 25f))
        // Centre is on the table (488.889 is the rail) but the ball hangs over it.
        assertFalse(table.containsBall(pf(480f, 0f), 25f))
        assertTrue(table.isPointInside(pf(480f, 0f)))
    }

    // --- The ghost cue ball: target centre + 50 at (-sin a, cos a), so a = 0 puts it 50 below (+y).

    @Test
    fun `an aim that would put the ghost over a rail turns to the nearest one that does not`() {
        // Target at the right-hand limit. a = -60 puts the ghost to the right of it (over the rail);
        // the nearest aim that does not is a = 0, ghost straight along the rail at (463.889, 50),
        // a 60 degree turn; the other way round is 120.
        val out = BallBounds.confine(onTable(target = pf(maxX, 0f), rotation = -60f))
        assertEquals(0f, out.protractorUnit.rotationDegrees, 0.05f)
        assertEquals(maxX, out.protractorUnit.center.x, eps)
    }

    @Test
    fun `in a corner the ghost is turned a quarter to lie along the rail`() {
        // Target in the corner (463.889, 952.778), aim 0 puts the ghost over the bottom rail. Allowed
        // aims are 90..180 (ghost left or up); 90 is the nearer: ghost at (413.889, 952.778).
        val out = BallBounds.confine(onTable(target = pf(maxX, maxY), rotation = 0f))
        assertEquals(90f, out.protractorUnit.rotationDegrees, 0.05f)
    }

    @Test
    fun `the aim keeps its winding when it is turned`() {
        // 300 is -60 a full turn on; it must come back as 360, not 0.
        val out = BallBounds.confine(onTable(target = pf(maxX, 0f), rotation = 300f))
        assertEquals(360f, out.protractorUnit.rotationDegrees, 0.05f)
    }

    @Test
    fun `a target dragged off the table is judged from where it ends up`() {
        // Target clamps to (463.889, 0); then the aim as in the first ghost test.
        val out = BallBounds.confine(onTable(target = pf(900f, 0f), rotation = -60f))
        assertEquals(maxX, out.protractorUnit.center.x, eps)
        assertEquals(0f, out.protractorUnit.rotationDegrees, 0.05f)
    }

    @Test
    fun `whatever the aim, the ghost is on the table`() {
        val targets = listOf(pf(maxX, 0f), pf(-maxX, maxY), pf(maxX, -maxY), pf(0f, maxY), pf(200f, -maxY))
        for (target in targets) {
            var a = -360f
            while (a <= 360f) {
                val out = BallBounds.confine(onTable(target = target, rotation = a))
                val rad = Math.toRadians(out.protractorUnit.rotationDegrees.toDouble())
                val ghostX = out.protractorUnit.center.x - 50f * Math.sin(rad).toFloat()
                val ghostY = out.protractorUnit.center.y + 50f * Math.cos(rad).toFloat()
                assertTrue("ghost x $ghostX at aim $a, target (${target.x}, ${target.y})", Math.abs(ghostX) <= maxX + eps)
                assertTrue("ghost y $ghostY at aim $a, target (${target.x}, ${target.y})", Math.abs(ghostY) <= maxY + eps)
                a += 15f
            }
        }
    }

    @Test
    fun `an aim that already fits is not touched`() {
        val state = onTable(target = pf(0f, 0f), rotation = 123f)
        assertSame(state, BallBounds.confine(state))
    }

    @Test
    fun `no ghost in banking mode, and massé owns its own`() {
        val banking = onTable(target = pf(maxX, 0f), rotation = -60f, banking = true)
        assertEquals(-60f, BallBounds.confine(banking).protractorUnit.rotationDegrees, 0f)
        val masse = onTable(target = pf(maxX, 0f), rotation = -60f, masse = true)
        assertEquals(-60f, BallBounds.confine(masse).protractorUnit.rotationDegrees, 0f)
    }
}
