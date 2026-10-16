package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
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
    fun `grid slots cycle through four quadrants`() {
        val r = 10f
        val slots = (0..3).map { gridSlotOffset(it, r) }
        assertEquals(listOf(-30f, 30f, -30f, 30f), slots.map { it.first })
        assertEquals(listOf(-60f, -60f, 60f, 60f), slots.map { it.second })
        // The fifth obstacle wraps back to the first slot.
        assertEquals(gridSlotOffset(0, r), gridSlotOffset(4, r))
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
        // The detection coincides with the protractor unit's centre, so it is already claimed and
        // must not be reused; placement falls through to the grid. PointF(x, y) is stubbed to zeros
        // in JVM unit tests, so assert on identity rather than coordinates.
        val unit = CueDetatState().protractorUnit
        val claimed = pf(unit.center.x, unit.center.y)
        @Suppress("DEPRECATION")
        val state = CueDetatState(visionData = VisionData(genericBalls = listOf(claimed)))
        val out = add(state)
        assertEquals(1, out.obstacleBalls.size)
        assertNotSame(claimed, out.obstacleBalls.single().center)
    }
}
