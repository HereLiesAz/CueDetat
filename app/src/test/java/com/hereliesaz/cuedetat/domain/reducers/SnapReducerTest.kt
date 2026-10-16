package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

/** Drives [SnapReducer] with explicit timestamps; no wall clock involved. */
class SnapReducerTest {

    private val reducer = SnapReducer()

    @Suppress("DEPRECATION")
    private fun frame(vararg p: PointF) = VisionData(genericBalls = p.toList())

    @Test
    fun `new detection becomes an unconfirmed candidate stamped with the injected time`() {
        val out = reducer.reduce(CueDetatState(), frame(pf(100f, 100f)), nowMs = 1_000L)
        val c = out.snapCandidates!!.single()
        assertEquals(1_000L, c.firstSeenTimestamp)
        assertFalse(c.isConfirmed)
    }

    @Test
    fun `candidate seen for exactly 1500 ms is not yet confirmed`() {
        val s1 = reducer.reduce(CueDetatState(), frame(pf(100f, 100f)), nowMs = 0L)
        val s2 = reducer.reduce(s1, frame(pf(100f, 100f)), nowMs = 1_500L)
        assertFalse(s2.snapCandidates!!.single().isConfirmed)
    }

    @Test
    fun `candidate seen for more than 1500 ms is confirmed and stays confirmed`() {
        val s1 = reducer.reduce(CueDetatState(), frame(pf(100f, 100f)), nowMs = 0L)
        val s2 = reducer.reduce(s1, frame(pf(102f, 100f)), nowMs = 1_501L)
        assertTrue(s2.snapCandidates!!.single().isConfirmed)
        // Confirmation is sticky even if the clock is not advanced past the threshold again.
        val s3 = reducer.reduce(s2, frame(pf(102f, 100f)), nowMs = 1_502L)
        assertTrue(s3.snapCandidates!!.single().isConfirmed)
    }

    @Test
    fun `a detection that jumps beyond the proximity threshold restarts the clock`() {
        val s1 = reducer.reduce(CueDetatState(), frame(pf(100f, 100f)), nowMs = 0L)
        val s2 = reducer.reduce(s1, frame(pf(200f, 100f)), nowMs = 5_000L)
        val c = s2.snapCandidates!!.single()
        assertEquals(5_000L, c.firstSeenTimestamp)
        assertFalse(c.isConfirmed)
    }

    @Test
    fun `a candidate missing from the next frame is dropped`() {
        val s1 = reducer.reduce(CueDetatState(), frame(pf(100f, 100f)), nowMs = 0L)
        val s2 = reducer.reduce(s1, frame(), nowMs = 100L)
        assertTrue(s2.snapCandidates!!.isEmpty())
    }

    @Test
    fun `disabling snapping clears all candidates`() {
        val s1 = reducer.reduce(CueDetatState(), frame(pf(100f, 100f)), nowMs = 0L)
        val off = reducer.reduce(s1.copy(isSnappingEnabled = false), frame(pf(100f, 100f)), nowMs = 10L)
        assertTrue(off.snapCandidates!!.isEmpty())
    }
}
