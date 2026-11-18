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
    ) = CueDetatState().copy(
        table = Table(TableSize.EIGHT_FT, visible),
        protractorUnit = ProtractorUnit(target, LOGICAL_BALL_RADIUS, 0f),
        onPlaneBall = cue?.let { OnPlaneBall(it, LOGICAL_BALL_RADIUS) },
        obstacleBalls = obstacles,
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
}
