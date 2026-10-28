package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import org.junit.Assert.assertEquals
import org.junit.Test

class BallPickArmingTest {

    private fun arm(s: CueDetatState, phase: com.hereliesaz.cuedetat.domain.BallSelectionPhase) =
        reduceCvAction(s, com.hereliesaz.cuedetat.domain.MainScreenEvent.BeginBallSelection(phase))

    @Test
    fun `the cue and target buttons arm their pick and disarm on a second press`() {
        val cue = com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_CUE
        val target = com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_TARGET
        val armed = arm(CueDetatState(), cue)
        assertEquals(cue, armed.ballSelectionPhase)
        assertEquals(target, arm(armed, target).ballSelectionPhase)
        assertEquals(com.hereliesaz.cuedetat.domain.BallSelectionPhase.NONE, arm(armed, cue).ballSelectionPhase)
    }

    @Test
    fun `a tap with no table matrix places nothing`() {
        val armed = CueDetatState(ballSelectionPhase = com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_CUE)
        val out = reduceCvAction(
            armed,
            com.hereliesaz.cuedetat.domain.MainScreenEvent.ArSurfaceTapped(android.graphics.PointF()),
        )
        assertEquals(armed, out)
    }
}
