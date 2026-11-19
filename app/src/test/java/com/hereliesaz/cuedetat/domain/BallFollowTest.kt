package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

/** Expected numbers are worked from the constants: deadband 1.0, full follow at 6.0, share 0.25 to 1. */
class BallFollowTest {

    @Test
    fun `a move inside the deadband is held`() {
        val out = BallFollow.step(pf(100f, 100f), pf(100.6f, 99.5f))
        assertEquals(100f, out.x, 0f)
        assertEquals(100f, out.y, 0f)
    }

    @Test
    fun `a real move is followed outright`() {
        val out = BallFollow.step(pf(100f, 100f), pf(140f, 90f))
        assertEquals(140f, out.x, 1e-4f)
        assertEquals(90f, out.y, 1e-4f)
    }

    @Test
    fun `just past the deadband takes a quarter, and the share grows with the distance`() {
        // 1.4 units: share = 0.25 + 0.75 * 0.4 / 5 = 0.31, so it moves 0.434.
        assertEquals(0.434f, BallFollow.step(pf(0f, 0f), pf(1.4f, 0f)).x, 1e-3f)
        // 3.5 units: share = 0.25 + 0.75 * 2.5 / 5 = 0.625, so it moves 2.1875.
        assertEquals(2.1875f, BallFollow.step(pf(0f, 0f), pf(3.5f, 0f)).x, 1e-3f)
        // 6 units and beyond: all of it.
        assertEquals(6f, BallFollow.step(pf(0f, 0f), pf(6f, 0f)).x, 1e-4f)
    }

    @Test
    fun `the direction of the move is kept`() {
        val out = BallFollow.step(pf(0f, 0f), pf(3f, 4f)) // distance 5
        assertEquals(3f / 4f, out.x / out.y, 1e-4f)
    }

    @Test
    fun `detection noise on a still ball moves it not at all`() {
        val random = Random(11)
        var ball = pf(100f, 200f)
        var travelled = 0f
        repeat(1000) {
            // +-0.6 units of noise on each axis: at most 0.85 from the centre, always inside the deadband.
            val detected = pf(100f + (random.nextFloat() - 0.5f) * 1.2f, 200f + (random.nextFloat() - 0.5f) * 1.2f)
            val next = BallFollow.step(ball, detected)
            travelled += hypot(next.x - ball.x, next.y - ball.y)
            ball = next
        }
        assertEquals(0f, travelled, 0f)
    }

    @Test
    fun `a ball that is being pushed along is followed with a small, bounded lag`() {
        // 0.5 units a frame, with the same noise on top. The lag settles near 1.5 units.
        val random = Random(3)
        var ball = pf(0f, 0f)
        var worst = 0f
        for (i in 1..400) {
            val truth = i * 0.5f
            val detected = pf(truth + (random.nextFloat() - 0.5f) * 1.0f, 0f)
            ball = BallFollow.step(ball, detected)
            if (i > 50) worst = maxOf(worst, truth - ball.x)
        }
        assertTrue("lag behind a ball moving at 0.5 units a frame: $worst", worst <= 2.5f)
    }
}
