package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.data.DetectedBall
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun pf(x: Float, y: Float) = PointF().apply { this.x = x; this.y = y }

class BallPlacementTest {

    private val r = LOGICAL_BALL_RADIUS

    @Test
    fun `a tap near a detected ball takes that ball's position`() {
        val out = BallPlacement.resolve(
            pf(100f, 100f), BallSelectionPhase.AWAITING_CUE, TargetType.SOLIDS, null,
            listOf(DetectedBall(pf(100f + r, 100f))),
        )
        assertTrue(out.snapped)
        assertEquals(100f + r, out.position.x, 1e-4f)
    }

    @Test
    fun `a tap on bare felt goes exactly where it landed`() {
        val out = BallPlacement.resolve(
            pf(100f, 100f), BallSelectionPhase.AWAITING_CUE, TargetType.SOLIDS, null,
            listOf(DetectedBall(pf(100f + r * 10f, 100f))),
        )
        assertFalse(out.snapped)
        assertEquals(100f, out.position.x, 1e-4f)
        assertEquals(100f, out.position.y, 1e-4f)
    }

    @Test
    fun `the target pick skips the other suit`() {
        val out = BallPlacement.resolve(
            pf(100f, 100f), BallSelectionPhase.AWAITING_TARGET, TargetType.STRIPES, null,
            listOf(DetectedBall(pf(105f, 100f), BallType.SOLID)),
        )
        assertFalse(out.snapped)
    }

    @Test
    fun `the cue pick takes any detection`() {
        val out = BallPlacement.resolve(
            pf(100f, 100f), BallSelectionPhase.AWAITING_CUE, TargetType.STRIPES, null,
            listOf(DetectedBall(pf(105f, 100f), BallType.SOLID)),
        )
        assertTrue(out.snapped)
    }

    @Test
    fun `an unconfirmed candidate is not trusted`() {
        val out = BallPlacement.resolve(
            pf(100f, 100f), BallSelectionPhase.AWAITING_CUE, TargetType.SOLIDS,
            listOf(SnapCandidate(pf(105f, 100f), firstSeenTimestamp = 0L, isConfirmed = false)), null,
        )
        assertFalse(out.snapped)
    }

    @Test
    fun `the nearer of two detections wins`() {
        val out = BallPlacement.resolve(
            pf(100f, 100f), BallSelectionPhase.AWAITING_CUE, TargetType.SOLIDS, null,
            listOf(DetectedBall(pf(100f + r * 2f, 100f)), DetectedBall(pf(100f - r, 100f))),
        )
        assertEquals(100f - r, out.position.x, 1e-4f)
    }
}
