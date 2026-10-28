package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.state.SnapCandidate
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

    @Test
    fun `two close balls keep their own tracks`() {
        // Tracks A at x=0 (first seen at 0) and B at x=20 (first seen at 1000); detections arrive
        // as x=10 then x=-5. Nearest-in-turn gives the x=10 detection to A (a tie, first wins) and
        // leaves B for x=-5, which is 5 from A. Matching both together (total 15 against 35) gives
        // x=10 to B and x=-5 to A.
        val before = CueDetatState(snapCandidates = listOf(
            SnapCandidate(pf(0f, 0f), firstSeenTimestamp = 0L),
            SnapCandidate(pf(20f, 0f), firstSeenTimestamp = 1_000L),
        ))
        val out = reducer.reduce(before, frame(pf(10f, 0f), pf(-5f, 0f)), nowMs = 2_000L)
        val byX = out.snapCandidates!!.associateBy { it.detectedPoint.x }
        assertEquals(1_000L, byX.getValue(10f).firstSeenTimestamp)
        assertEquals(0L, byX.getValue(-5f).firstSeenTimestamp)
    }

    @Test
    fun `the cue and target anchors never follow the same candidate`() {
        // Cue anchor at 0, target anchor at 6; candidates at 5 and 40. Each anchor's nearest is
        // the one at 5, so followed independently both land on it.
        val before = CueDetatState(
            cueBallCvAnchor = pf(0f, 0f),
            targetCvAnchor = pf(6f, 0f),
        )
        val out = reducer.reduce(before, frame(pf(5f, 0f), pf(40f, 0f)), nowMs = 0L)
        val cue = out.cueBallCvAnchor!!
        val target = out.targetCvAnchor!!
        assertTrue("both anchors on (${cue.x}, ${cue.y})", cue.x != target.x || cue.y != target.y)
        // Costs [[5, 40], [1, 34]]: cue -> 5 and target -> 40 total 39, the other pairing 41.
        assertEquals(5f, cue.x, 0f)
        assertEquals(40f, target.x, 0f)
    }
}
