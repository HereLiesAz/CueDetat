package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

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

    // --- Measured against the filter this one replaced (same constants as it had, no floor, no hold).

    private fun legacy() = ScreenCornerSmoother(
        minCutoffHz = 1.2f, beta = 0.02f, derivativeCutoffHz = 1.0f, noiseFloorPxPerSec = 0f, holdPx = 0f,
    )

    private fun gaussian(random: Random, sigma: Float): Float {
        val u1 = random.nextDouble().coerceAtLeast(1e-12)
        return (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * random.nextDouble())).toFloat() * sigma
    }

    private val fps30 = 33_333_333L

    /** Runs a quad whose top-left follows [truth] (px at time s) with [noise] px of noise per coordinate. */
    private fun track(s: ScreenCornerSmoother, seconds: Float, noise: Float, seed: Int, truth: (Float) -> Float): List<Triple<Float, Float, Float>> {
        val random = Random(seed)
        val out = ArrayList<Triple<Float, Float, Float>>()
        for (i in 1..(seconds * 30).toInt()) {
            val t = i / 30f
            val x = truth(t)
            val noisy = listOf(
                Pt(x + gaussian(random, noise), 200f + gaussian(random, noise)),
                Pt(x + 300f + gaussian(random, noise), 200f + gaussian(random, noise)),
                Pt(x + 300f + gaussian(random, noise), 400f + gaussian(random, noise)),
                Pt(x + gaussian(random, noise), 400f + gaussian(random, noise)),
            )
            out += Triple(t, x, s.filter(noisy, i * fps30)[0].x)
        }
        return out
    }

    private fun rms(v: List<Float>) = sqrt(v.fold(0f) { a, x -> a + x * x } / v.size)

    @Test
    fun `at rest it leaves a good deal less of 1 px of noise than the filter it replaced`() {
        val now = rms(track(ScreenCornerSmoother(), 30f, 1f, 1) { 100f }.filter { it.first > 5f }.map { it.third - it.second })
        val before = rms(track(legacy(), 30f, 1f, 1) { 100f }.filter { it.first > 5f }.map { it.third - it.second })
        assertTrue("now $now px, before $before px", now < 0.3f && now < 0.75f * before)
    }

    @Test
    fun `the start of a 1000 px per second pan lags less than before`() {
        // Still for a second, then up to 1000 px/s over 0.2 s.
        val pan = { t: Float ->
            when {
                t < 1f -> 100f
                t < 1.2f -> 100f + 0.5f * 5000f * (t - 1f) * (t - 1f)
                else -> 200f + 1000f * (t - 1.2f)
            }
        }
        val now = track(ScreenCornerSmoother(), 4f, 0.3f, 2, pan).filter { it.first > 1f }
        val before = track(legacy(), 4f, 0.3f, 2, pan).filter { it.first > 1f }
        val nowWorst = now.maxOf { it.second - it.third }
        val beforeWorst = before.maxOf { it.second - it.third }
        assertTrue("worst lag now $nowWorst px, before $beforeWorst px", nowWorst < 9f && nowWorst < beforeWorst)
        val steady = now.filter { it.first > 3f }.let { s -> s.map { it.second - it.third }.sum() / s.size }
        assertTrue("steady lag at 1000 px/s: $steady px", steady < 4.5f)
    }

    @Test
    fun `a slow deliberate creep is followed about as closely as before`() {
        val creep = { t: Float -> 100f + 30f * t }
        val now = track(ScreenCornerSmoother(), 10f, 1f, 3, creep).filter { it.first > 3f }
        val lag = now.map { it.second - it.third }.sum() / now.size
        assertTrue("lag at 30 px/s: $lag px (before: about 1.7)", lag in 0f..2.5f)
    }

    @Test
    fun `a change smaller than the hold does not move the published quad`() {
        val s = ScreenCornerSmoother()
        var last = emptyList<Pt>()
        for (i in 1..120) last = s.filter(corners(100f), i * frameNanos)
        // 0.05 px of input motion for a few frames: the output cannot have moved 0.15 px.
        val after = (121..125).map { s.filter(corners(100.05f), it * frameNanos) }
        assertTrue(after.all { it == last })
    }

    @Test
    fun `the four corners are filtered together, so the table keeps its shape in motion`() {
        // A rigid quad panning at 1000 px/s with noise: no edge may stretch by more than the noise.
        val random = Random(9)
        val s = ScreenCornerSmoother()
        var worst = 0f
        for (i in 1..120) {
            val x = 100f + 1000f * maxOf(0, i - 30) / 30f // still, then 1000 px/s (33 px a frame)
            val noisy = corners(x).map { Pt(it.x + gaussian(random, 0.3f), it.y + gaussian(random, 0.3f)) }
            val out = s.filter(noisy, i * fps30)
            if (i > 60) worst = maxOf(worst, abs((out[1].x - out[0].x) - 300f))
        }
        assertTrue("top edge stretched by $worst px", worst < 2.5f)
    }
}
