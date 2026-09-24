package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TableSnapPolicyTest {

    private val here = Pose(0f, 0f, 10f, 1f)
    private val there = Pose(100f, -40f, 30f, 2f)

    @Test
    fun `no pull while the user is handling the table`() {
        assertNull(TableSnapPolicy.pullStep(here, there, iou = 0.95f, msSinceUserAdjust = 500L))
    }

    @Test
    fun `no pull on an unsure fit`() {
        assertNull(TableSnapPolicy.pullStep(here, there, iou = 0.7f, msSinceUserAdjust = 10_000L))
    }

    @Test
    fun `pull moves 15 percent of the way`() {
        val p = TableSnapPolicy.pullStep(here, there, iou = 0.95f, msSinceUserAdjust = 10_000L)!!
        assertEquals(15f, p.offsetX, 1e-3f)          // 0.15 * 100
        assertEquals(-6f, p.offsetY, 1e-3f)          // 0.15 * -40
        assertEquals(13f, p.rotationDeg, 1e-3f)      // 10 + 0.15 * 20
        assertEquals(1.1096f, p.zoom, 1e-3f)         // 2^0.15
    }

    @Test
    fun `rotation takes the short way across the half-turn`() {
        // 85 to -85 is 10 degrees modulo 180, not 170 back the long way.
        val p = TableSnapPolicy.pullStep(Pose(0f, 0f, 85f, 1f), Pose(0f, 0f, -85f, 1f), 0.95f, 10_000L)!!
        assertEquals(86.5f, p.rotationDeg, 1e-3f)    // 85 + 0.15 * 10
    }

    @Test
    fun `already there, no step`() {
        assertNull(TableSnapPolicy.pullStep(here, here, 0.95f, 10_000L))
    }
}
