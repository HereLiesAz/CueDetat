package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HueMathTest {

    private fun histogram(vararg hues: Int) = FloatArray(HueMath.PERIOD).also { h -> hues.forEach { h[it] += 1f } }

    @Test
    fun `the mean of hues either side of the seam is on the seam, not halfway round the wheel`() {
        // 170, 175, 5 and 10 sit at -10, -5, +5 and +10 from 0. The linear mean is 90.
        val stats = HueMath.circularStats(histogram(170, 175, 5, 10))!!
        assertTrue("mean ${stats.mean}", HueMath.distance(stats.mean, 0.0) < 0.05)
    }

    @Test
    fun `away from the seam the circular mean is the ordinary one`() {
        val stats = HueMath.circularStats(histogram(100, 110, 120))!!
        assertEquals(110.0, stats.mean, 0.1)
    }

    @Test
    fun `spread matches the ordinary standard deviation for a tight cluster`() {
        // Deviations -10, -5, 5, 10: sqrt((100 + 25 + 25 + 100) / 4) = 7.9.
        val stats = HueMath.circularStats(histogram(170, 175, 5, 10))!!
        assertEquals(7.9, stats.std, 0.3)
    }

    @Test
    fun `hues spread evenly around the wheel have no mean`() {
        val all = FloatArray(HueMath.PERIOD) { 1f }
        assertNull(HueMath.circularStats(all))
        assertNull(HueMath.circularStats(FloatArray(HueMath.PERIOD)))
        assertNull(HueMath.circularStats(FloatArray(10)))
    }

    @Test
    fun `a window inside the wheel is one range`() {
        assertEquals(listOf(80.0..100.0), HueMath.windows(90.0, 10.0))
    }

    @Test
    fun `a window over the low seam is two ranges`() {
        assertEquals(listOf(0.0..15.0, 175.0..180.0), HueMath.windows(5.0, 10.0))
    }

    @Test
    fun `a window over the high seam is two ranges`() {
        assertEquals(listOf(165.0..180.0, 0.0..5.0), HueMath.windows(175.0, 10.0))
    }

    @Test
    fun `half a turn or more is the whole wheel`() {
        assertEquals(listOf(0.0..180.0), HueMath.windows(40.0, 95.0))
    }

    @Test
    fun `distance goes the short way round`() {
        assertEquals(10.0, HueMath.distance(175.0, 5.0), 1e-9)
        assertEquals(90.0, HueMath.distance(0.0, 90.0), 1e-9)
    }
}
