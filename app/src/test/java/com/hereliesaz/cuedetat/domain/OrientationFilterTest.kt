package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The orientation filter against synthetic motion, with measured jitter and lag in place of a feel.
 * Bounds come from a sweep of the same signals (noise sigma 0.05 degrees, the sensor's likely
 * order), with margin; each says what it protects.
 */
class OrientationFilterTest {

    private fun gaussian(random: Random, sigma: Float): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-12)
        val u2 = random.nextDouble()
        return (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)).toFloat() * sigma
    }

    private data class Sample(val t: Float, val truth: Float, val out: Float)

    /** Runs one axis (pitch, or yaw when [yaw]) through [signal] at [hz] and returns truth against output. */
    private fun run(
        signal: (Float) -> Float, hz: Int, seconds: Float, noise: Float, seed: Int = 1, yaw: Boolean = false,
    ): List<Sample> {
        val filter = OrientationFilter()
        val random = Random(seed)
        val samples = ArrayList<Sample>()
        for (i in 0 until (seconds * hz).toInt()) {
            val t = i.toFloat() / hz
            val truth = signal(t)
            val raw = truth + gaussian(random, noise)
            val nanos = (i * 1e9 / hz).toLong()
            val out = if (yaw) filter.update(raw, 0f, 0f, nanos).yaw else filter.update(0f, raw, 0f, nanos).pitch
            samples += Sample(t, truth, out)
        }
        return samples
    }

    private fun rms(values: List<Float>) = sqrt(values.fold(0f) { a, v -> a + v * v } / values.size)

    private fun meanLag(samples: List<Sample>, after: Float) =
        samples.filter { it.t > after }.let { s -> s.map { it.truth - it.out }.sum() / s.size }

    @Test
    fun `a still phone's noise is smoothed to a small fraction, at both sensor rates`() {
        for (hz in listOf(16, 50)) {
            val out = run({ 30f }, hz, 60f, noise = 0.05f).filter { it.t > 10f }
            val residual = rms(out.map { it.out - 30f })
            assertTrue("$hz Hz: residual $residual of 0.05 input", residual < 0.02f)
        }
    }

    @Test
    fun `the same motion lags the same at 16 and 50 Hz`() {
        // The old filter stepped its strength per sample, so the rate changed the filter itself.
        for (speed in listOf(5f, 20f, 60f)) {
            val slow = meanLag(run({ speed * it }, 16, 12f, 0.05f), 6f)
            val fast = meanLag(run({ speed * it }, 50, 12f, 0.05f), 6f)
            assertEquals("lag at $speed deg/s, 16 vs 50 Hz", slow, fast, 0.1f)
        }
    }

    @Test
    fun `lag stays small at every speed`() {
        // About 0.6 degrees at 5 and 20 degrees a second, about 1.2 at a quick 60.
        assertTrue(meanLag(run({ 5f * it }, 50, 12f, 0.05f), 6f) in 0.3f..0.9f)
        assertTrue(meanLag(run({ 20f * it }, 50, 12f, 0.05f), 6f) < 1.0f)
        assertTrue(meanLag(run({ 60f * it }, 50, 5f, 0.05f), 2f) < 1.6f)
    }

    @Test
    fun `a yaw-only turn no longer drags`() {
        // At 20 degrees a second, the old filter (yaw never raised its strength) sat at 0.025 a sample
        // and trailed by tens of degrees after ten seconds.
        val out = run({ (((20f * it) + 180f) % 360f) - 180f }, 16, 10f, 0.05f, yaw = true)
        val worst = out.filter { it.t > 5f }.maxOf { abs(wrapDifference(it.truth, it.out)) }
        assertTrue("worst yaw lag $worst degrees", worst < 1.2f)
    }

    @Test
    fun `a sudden step settles in about a tenth of a second`() {
        for (hz in listOf(16, 50)) {
            val out = run({ if (it < 1f) 0f else 20f }, hz, 6f, noise = 0f)
            val reached = out.first { it.t >= 1f && it.out >= 18f }.t - 1f
            assertTrue("$hz Hz: 90% in $reached s", reached < 0.12f)
        }
    }

    @Test
    fun `yaw crosses the 180 degree wrap without a jump`() {
        // 20 degrees a second at 50 Hz is 0.4 per sample; the output must not step by much more.
        val out = run({ (((170f + 20f * it) + 180f) % 360f) - 180f }, 50, 3f, 0f, yaw = true)
        val worstStep = out.zipWithNext { a, b -> abs(wrapDifference(b.out, a.out)) }.max()
        assertTrue("worst step $worstStep degrees", worstStep < 0.6f)
    }

    @Test
    fun `a slow steady pan comes out steady, never stepping backwards`() {
        // The old filter chose its strength from its own lag and could stick and slip on a slow pan.
        val out = run({ 5f * it }, 50, 10f, 0.02f)
        val steps = out.filter { it.t > 4f }.zipWithNext { a, b -> b.out - a.out }
        assertTrue("smallest step ${steps.min()}", steps.min() > -0.005f)
    }

    @Test
    fun `noise alone never looks like movement to the rate switch`() {
        for (hz in listOf(16, 50)) {
            val filter = OrientationFilter()
            val random = Random(5)
            var worst = 0f
            for (i in 0 until hz * 120) {
                filter.update(0f, 30f + gaussian(random, 0.05f), 0f, (i * 1e9 / hz).toLong())
                if (i > hz * 5) worst = maxOf(worst, filter.speedDegPerSecond)
            }
            assertTrue("$hz Hz: speed estimate from noise $worst deg/s, threshold is 3", worst < 2f)
        }
    }

    @Test
    fun `real motion is measured as motion`() {
        val filter = OrientationFilter()
        for (i in 0 until 50 * 10) filter.update(0f, 5f * i / 50f, 0f, (i * 1e9 / 50).toLong())
        assertEquals(5f, filter.speedDegPerSecond, 0.4f)
    }

    @Test
    fun `a long gap between samples restarts at the raw value`() {
        val filter = OrientationFilter()
        filter.update(0f, 10f, 0f, 0L)
        filter.update(0f, 10f, 0f, 20_000_000L)
        assertEquals(50f, filter.update(0f, 50f, 0f, 5_000_000_000L).pitch, 0f)
    }

    @Test
    fun `the same timestamp twice is not a new sample`() {
        val filter = OrientationFilter()
        val random = Random(2)
        var worst = 0f
        for (i in 0 until 50 * 60) {
            val nanos = (i * 1e9 / 50).toLong()
            val first = filter.update(0f, 30f + gaussian(random, 0.05f), 0f, nanos).pitch
            val again = filter.update(0f, 30f + gaussian(random, 0.05f), 0f, nanos).pitch
            assertEquals(first, again, 0f)
            if (i > 500) worst = maxOf(worst, abs(first - 30f))
        }
        assertTrue("residual with every sample doubled: $worst degrees", worst < 0.05f)
    }

    @Test
    fun `a reading that is not a number is ignored, not kept`() {
        val filter = OrientationFilter()
        filter.update(10f, 20f, 30f, 0L)
        val bad = filter.update(Float.NaN, Float.POSITIVE_INFINITY, Float.NaN, 20_000_000L)
        assertEquals(10f, bad.yaw, 0f)
        assertEquals(20f, bad.pitch, 0f)
        assertEquals(30f, bad.roll, 0f)
        // And the next good one carries on as if it had not happened.
        val good = filter.update(10f, 20f, 30f, 40_000_000L)
        assertEquals(20f, good.pitch, 1e-3f)
    }

    @Test
    fun `cutoffs that would freeze the filter are refused`() {
        for (build in listOf<() -> Unit>(
            { OrientationFilter(minCutoffHz = 0f) },
            { OrientationFilter(minCutoffHz = 2f, maxCutoffHz = 1f) },
            { OrientationFilter(derivativeCutoffHz = 0f) },
        )) {
            val refused = try { build(); false } catch (e: IllegalArgumentException) { true }
            assertTrue(refused)
        }
    }

    /** Signed shortest-arc difference `a - b`, in degrees. */
    private fun wrapDifference(a: Float, b: Float): Float {
        var d = a - b
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }
}
