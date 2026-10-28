package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ScreenCornerSmootherTest {

    private val frameNanos = 16_666_667L

    private fun corners(x: Float, y: Float = 200f) = listOf(Pt(x, y), Pt(x + 300f, y), Pt(x + 300f, y + 200f), Pt(x, y + 200f))

    @Test
    fun `a still table stays still`() {
        val s = ScreenCornerSmoother()
        var out = corners(100f)
        for (i in 1..60) out = s.filter(corners(100f), i * frameNanos)
        assertEquals(100f, out[0].x, 1e-3f)
        assertEquals(200f, out[0].y, 1e-3f)
    }

    @Test
    fun `pixel jitter at rest is cut well below its input amplitude`() {
        val s = ScreenCornerSmoother()
        var worst = 0f
        for (i in 1..180) {
            val noisy = 100f + if (i % 2 == 0) 2f else -2f
            val out = s.filter(corners(noisy), i * frameNanos)
            if (i > 60) worst = maxOf(worst, abs(out[0].x - 100f))
        }
        assertTrue("jitter left over: $worst px of 2", worst < 0.5f)
    }

    @Test
    fun `steady camera motion is followed with little lag`() {
        val s = ScreenCornerSmoother()
        val perFrame = 10f // 600 px/s
        var out = corners(0f)
        var truth = 0f
        for (i in 1..90) {
            truth = i * perFrame
            out = s.filter(corners(truth), i * frameNanos)
        }
        // A fixed low-pass tuned to kill the jitter above would trail by ~80 px here.
        assertTrue("trailing by ${truth - out[0].x} px", truth - out[0].x in 0f..20f)
    }

    @Test
    fun `a jump is a placement not motion and passes straight through`() {
        val s = ScreenCornerSmoother()
        for (i in 1..30) s.filter(corners(100f), i * frameNanos)
        val out = s.filter(corners(700f), 31 * frameNanos)
        assertEquals(700f, out[0].x, 1e-3f)
    }

    @Test
    fun `reset forgets the history`() {
        val s = ScreenCornerSmoother()
        for (i in 1..30) s.filter(corners(100f), i * frameNanos)
        s.reset()
        val out = s.filter(corners(160f), 31 * frameNanos)
        assertEquals(160f, out[0].x, 1e-3f)
    }
}
