package com.hereliesaz.cuedetat.domain

import org.junit.Assert.*
import org.junit.Test

private fun profile(
    time: Long,
    ay: Float,
    gx: Float = 0f,
    gz: Float = 0f
): StrokeProfile = StrokeProfile(
    time = time,
    acceleration = floatArrayOf(0f, ay, 0f),
    gyroscope = floatArrayOf(gx, 0f, gz)
)

/** Builds a smooth, straight-line stroke of forward-acceleration samples. */
private fun smoothStroke(size: Int = 50, base: Float = 1f, step: Float = 0.05f): List<StrokeProfile> =
    (0 until size).map { i -> profile(time = i.toLong(), ay = base + step * i) }

/**
 * Builds a jittery/erratic stroke with large sample-to-sample swings and wrist twist.
 * The amplitude is large enough that its DTW distance from [smoothStroke] clears the
 * "erratic" threshold even at a resting (low) heart rate.
 */
private fun erraticStroke(size: Int = 50): List<StrokeProfile> =
    (0 until size).map { i ->
        val jitter = if (i % 2 == 0) 200f else -200f
        profile(time = i.toLong(), ay = jitter, gx = 5f, gz = 5f)
    }

class StrokeAnalyzerTest {

    private val analyzer = StrokeAnalyzer()

    // --- computeDTWDistance (indirectly, via analyzeStroke's fluidityScore) ---

    @Test
    fun `identical sequences produce a perfect fluidity score`() {
        val stroke = smoothStroke()
        val result = analyzer.analyzeStroke(baseline = stroke, current = stroke, heartRate = 60f)

        assertEquals(1.0f, result.fluidityScore, 0.001f)
    }

    @Test
    fun `different sequences produce a fluidity score below 1_0`() {
        val baseline = smoothStroke(base = 1f, step = 0.05f)
        val current = erraticStroke()

        val result = analyzer.analyzeStroke(baseline = baseline, current = current, heartRate = 60f)

        assertTrue(
            "Expected fluidityScore < 1.0 for a divergent stroke but was ${result.fluidityScore}",
            result.fluidityScore < 1.0f
        )
    }

    @Test
    fun `fluidityScore is not always 1_0 once a real baseline exists`() {
        // Regression test for the bug where an always-empty baseline made
        // computeDTWDistance return 0 unconditionally, forcing fluidityScore to
        // always be 1.0 regardless of how erratic the current stroke was.
        val baseline = smoothStroke(base = 1f, step = 0.05f)

        val closeMatchScore = analyzer.analyzeStroke(
            baseline = baseline,
            current = smoothStroke(base = 1f, step = 0.05f),
            heartRate = 60f
        ).fluidityScore

        val divergentScore = analyzer.analyzeStroke(
            baseline = baseline,
            current = erraticStroke(),
            heartRate = 60f
        ).fluidityScore

        assertTrue(divergentScore < closeMatchScore)
        assertTrue(divergentScore < 1.0f)
    }

    @Test
    fun `empty baseline still returns a defined result without throwing`() {
        // No baseline captured yet (e.g. before the first calibration stroke).
        val result = analyzer.analyzeStroke(baseline = emptyList(), current = smoothStroke(), heartRate = 60f)
        assertEquals(1.0f, result.fluidityScore, 0.001f)
    }

    // --- isErratic ---

    @Test
    fun `isErratic fires on large twist penalty even with a near-matching baseline`() {
        // twistPenalty is coerced into [0, 1], so at a resting heart rate (threshold 1.5)
        // twist alone can never trip isErratic; an elevated heart rate lowers the
        // threshold to 0.5, which a maxed-out twist penalty (1.0) does clear.
        val baseline = smoothStroke()
        val current = (0 until 50).map { i -> profile(time = i.toLong(), ay = 1f, gx = 5f, gz = 5f) }

        val result = analyzer.analyzeStroke(baseline = baseline, current = current, heartRate = 140f)

        assertTrue(result.isErratic)
    }

    @Test
    fun `isErratic fires when DTW distance from baseline is large`() {
        val baseline = smoothStroke(base = 1f, step = 0.05f)
        val current = erraticStroke()

        val result = analyzer.analyzeStroke(baseline = baseline, current = current, heartRate = 60f)

        assertTrue(result.isErratic)
    }

    @Test
    fun `isErratic does not fire for a smooth stroke matching the baseline`() {
        val stroke = smoothStroke()
        val result = analyzer.analyzeStroke(baseline = stroke, current = stroke, heartRate = 60f)

        assertFalse(result.isErratic)
    }

    @Test
    fun `high heart rate lowers the erratic threshold`() {
        val baseline = smoothStroke(base = 1f, step = 0.05f)
        // A moderate twist that would not trip the low-HR threshold (1.5)
        // but should trip the high-HR threshold (0.5).
        val current = (0 until 50).map { i -> profile(time = i.toLong(), ay = 1f, gx = 0.6f, gz = 0.6f) }

        val calmResult = analyzer.analyzeStroke(baseline = baseline, current = current, heartRate = 80f)
        val excitedResult = analyzer.analyzeStroke(baseline = baseline, current = current, heartRate = 140f)

        assertFalse(calmResult.isErratic)
        assertTrue(excitedResult.isErratic)
    }
}

class StrokeSessionTrackerTest {

    @Test
    fun `no baseline exists before the first window completes`() {
        val tracker = StrokeSessionTracker(windowSize = 5)
        assertFalse(tracker.hasBaseline)

        repeat(4) { i -> assertNull(tracker.addSample(profile(i.toLong(), 1f), heartRate = 60f)) }
        assertFalse(tracker.hasBaseline)
    }

    @Test
    fun `first completed window becomes the baseline`() {
        val tracker = StrokeSessionTracker(windowSize = 5)

        var lastResult: AnalyticsResult? = null
        repeat(5) { i -> lastResult = tracker.addSample(profile(i.toLong(), 1f + i * 0.1f), heartRate = 60f) }

        assertTrue(tracker.hasBaseline)
        assertEquals(5, tracker.baselineProfiles.size)
        assertNotNull(lastResult)
        assertEquals(1.0f, lastResult!!.fluidityScore, 0.001f)
    }

    @Test
    fun `subsequent windows are compared against the fixed baseline`() {
        val tracker = StrokeSessionTracker(windowSize = 5)

        // Baseline stroke: smooth.
        repeat(5) { i -> tracker.addSample(profile(i.toLong(), 1f + i * 0.1f), heartRate = 60f) }
        val baselineSnapshot = tracker.baselineProfiles

        // Second stroke: erratic, should score below 1.0 and not overwrite the baseline.
        var secondResult: AnalyticsResult? = null
        repeat(5) { i ->
            val jitter = if (i % 2 == 0) 20f else -20f
            secondResult = tracker.addSample(profile(i.toLong(), jitter, gx = 5f, gz = 5f), heartRate = 60f)
        }

        assertNotNull(secondResult)
        assertTrue(secondResult!!.fluidityScore < 1.0f)
        assertEquals(baselineSnapshot, tracker.baselineProfiles)
    }

    @Test
    fun `reset clears baseline so the next window recalibrates`() {
        val tracker = StrokeSessionTracker(windowSize = 5)
        repeat(5) { i -> tracker.addSample(profile(i.toLong(), 1f), heartRate = 60f) }
        assertTrue(tracker.hasBaseline)

        tracker.reset()
        assertFalse(tracker.hasBaseline)

        var result: AnalyticsResult? = null
        repeat(5) { i -> result = tracker.addSample(profile(i.toLong(), 9f), heartRate = 60f) }

        assertTrue(tracker.hasBaseline)
        assertEquals(1.0f, result!!.fluidityScore, 0.001f)
    }
}
