package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TopDownBallRules.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class TopDownBallRulesTest {

    // Ball radius 8 px in the top-down view.

    @Test
    fun `seen from straight above, no stretch and contact under the centre`() {
        val e = TopDownBallRules.elevationFromStretch(1f)
        assertEquals(Math.PI.toFloat() / 2f, e, 1e-4f)
        // tan(45 deg) = 1: from above, the near end is one radius from the contact point.
        assertEquals(8f, TopDownBallRules.contactOffset(8f, e), 1e-3f)
    }

    @Test
    fun `at 30 degrees elevation`() {
        // k = 1 / sin 30 = 2.
        val e = TopDownBallRules.elevationFromStretch(2f)
        assertEquals(Math.toRadians(30.0).toFloat(), e, 1e-4f)
        // tan(15 deg) = 0.26795: near end is 2.14 px in front of contact.
        assertEquals(8f * 0.26795f, TopDownBallRules.contactOffset(8f, e), 1e-3f)
    }

    @Test
    fun `a stretched ball is still one ball`() {
        // k = 2: 16 px wide, up to 2 * 8 * 2 * 1.3 = 41.6 px long. 16 x 32, ellipse fill ~0.785.
        assertEquals(Verdict.SINGLE, TopDownBallRules.judge(16f, 32f, 400, 8f, 2f))
    }

    @Test
    fun `one ball behind another is a pair along the view`() {
        // 16 wide, 60 long: over one ball's 41.6, under two.
        assertEquals(Verdict.PAIR_ALONG, TopDownBallRules.judge(16f, 60f, 750, 8f, 2f))
    }

    @Test
    fun `two balls side by side are a pair across`() {
        assertEquals(Verdict.PAIR_ACROSS, TopDownBallRules.judge(32f, 32f, 800, 8f, 2f))
    }

    @Test
    fun `a chalk-sized speck and a cue-stick streak are not balls`() {
        assertEquals(Verdict.REJECT, TopDownBallRules.judge(6f, 6f, 30, 8f, 2f))
        assertEquals(Verdict.REJECT, TopDownBallRules.judge(8f, 200f, 1200, 8f, 2f))
    }
}
