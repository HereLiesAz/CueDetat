package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.random.Random
import org.junit.Test

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

/**
 * Placement is deterministic here: the default table is invisible, so the random strategy is not
 * reached and [Random] only needs to be injectable. A throwing [Random] proves it is never drawn.
 */
class ObstacleReducerTest {

    private val utils = ReducerUtils()
    private val noRandom = object : Random() {
        override fun nextBits(bitCount: Int): Int = error("random must not be used")
    }

    private fun add(state: CueDetatState) =
        reduceObstacleAction(state, MainScreenEvent.AddObstacleBall, utils, noRandom)

    @Test
    fun `with no table and no detections the first obstacle goes to the top-left grid slot`() {
        val r = CueDetatState().protractorUnit.radius
        val out = add(CueDetatState())
        assertEquals(1, out.obstacleBalls.size)
        assertEquals(-3f * r, out.obstacleBalls[0].center.x, 1e-4f)
        assertEquals(-6f * r, out.obstacleBalls[0].center.y, 1e-4f)
    }

    @Test
    fun `grid slots cycle through four quadrants`() {
        var s = CueDetatState()
        repeat(4) { s = add(s) }
        val r = s.protractorUnit.radius
        val xs = s.obstacleBalls.map { it.center.x }
        val ys = s.obstacleBalls.map { it.center.y }
        assertEquals(listOf(-3f * r, 3f * r, -3f * r, 3f * r), xs)
        assertEquals(listOf(-6f * r, -6f * r, 6f * r, 6f * r), ys)
    }

    @Test
    fun `a free detected ball is preferred over the grid`() {
        val detected = pf(500f, 500f)
        @Suppress("DEPRECATION")
        val state = CueDetatState(visionData = VisionData(genericBalls = listOf(detected)))
        val out = add(state)
        assertTrue(out.obstacleBalls.single().center.x == 500f && out.obstacleBalls.single().center.y == 500f)
    }

    @Test
    fun `a detected ball under an existing virtual ball is skipped`() {
        // Protractor unit sits at the origin: a detection on top of it is already claimed.
        @Suppress("DEPRECATION")
        val state = CueDetatState(visionData = VisionData(genericBalls = listOf(pf(0f, 0f))))
        val r = state.protractorUnit.radius
        val out = add(state)
        assertEquals(-3f * r, out.obstacleBalls.single().center.x, 1e-4f)
    }
}
