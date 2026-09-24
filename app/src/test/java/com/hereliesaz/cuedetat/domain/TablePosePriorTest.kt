package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TablePosePriorTest {

    private fun sample(yaw: Float, rot: Float, t: Long = 0L, zoom: Float = 1f) =
        TablePoseSample(yaw, 30f, 0f, rot, zoom, 0f, 0f, t)

    @Test
    fun `nothing remembered, no prior`() {
        assertNull(TablePosePrior.choose(null, null, null, 0f, 30f, 0L))
    }

    @Test
    fun `a fresh session pose wins and follows the phone's turn`() {
        // Confirmed at yaw 0, rotation 20. Phone turned to yaw 15: table turns to 20 - 15 = 5.
        val p = TablePosePrior.choose(sample(0f, 20f, t = 1_000L), null, listOf(sample(0f, 60f)), 15f, 30f, 1_000L)!!
        assertEquals(TablePosePrior.Source.SESSION, p.source)
        assertEquals(5f, p.prediction.rotationDeg, 1e-3f)
        assertEquals(1f, p.prediction.confidence, 1e-3f)
    }

    @Test
    fun `a stale session gives way to the location`() {
        val stale = sample(0f, 20f, t = 0L)
        val here = List(5) { sample(0f, 40f) }
        val p = TablePosePrior.choose(stale, here, null, 0f, 30f, TablePosePrior.SESSION_FADE_MS + 1)!!
        assertEquals(TablePosePrior.Source.LOCATION, p.source)
        assertEquals(40f, p.prediction.rotationDeg, 1e-3f)
    }

    @Test
    fun `the last table elsewhere is trusted at half`() {
        val p = TablePosePrior.choose(null, null, List(5) { sample(0f, 40f) }, 0f, 30f, 0L)!!
        assertEquals(TablePosePrior.Source.LAST_TABLE, p.source)
        assertEquals(0.5f, p.prediction.confidence, 1e-3f)
    }

    @Test
    fun `distance between two points about 111 m apart`() {
        // 0.001 degree of latitude is 111.2 m.
        assertEquals(111.2, TablePosePrior.distanceMeters(0.0, 0.0, 0.001, 0.0), 0.5)
    }
}
