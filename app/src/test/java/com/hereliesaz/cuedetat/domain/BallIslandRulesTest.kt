package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.BallIslandRules.BallKind
import com.hereliesaz.cuedetat.domain.BallIslandRules.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class BallIslandRulesTest {

    // Expected radius 10 px: a full disk is pi * 100 = 314 px, box 20 x 20.

    @Test
    fun `a round island the size of a ball is one ball`() {
        assertEquals(Verdict.Single, BallIslandRules.judge(area = 300, width = 20, height = 20, expectedRadius = 10f))
    }

    @Test
    fun `a ball half hidden by glare still counts`() {
        // 150 / 314 = 0.48 of a disk.
        assertEquals(Verdict.Single, BallIslandRules.judge(area = 150, width = 18, height = 15, expectedRadius = 10f))
    }

    @Test
    fun `a speck is not a ball`() {
        // 40 / 314 = 0.13 of a disk.
        assertEquals(Verdict.Reject, BallIslandRules.judge(area = 40, width = 7, height = 7, expectedRadius = 10f))
    }

    @Test
    fun `a long thin sliver is not a ball`() {
        // Aspect 4, fill 100 / 160 = 0.63.
        assertEquals(Verdict.Reject, BallIslandRules.judge(area = 100, width = 40, height = 4, expectedRadius = 10f))
    }

    @Test
    fun `two touching balls are a pair`() {
        // Two disks: 628 px in a 40 x 20 box; area ratio 2.0, aspect 2.0, fill 0.785.
        assertEquals(Verdict.Pair, BallIslandRules.judge(area = 628, width = 40, height = 20, expectedRadius = 10f))
    }

    @Test
    fun `a big blob like a hand is rejected`() {
        // 5000 / 314 = 16 disks.
        assertEquals(Verdict.Reject, BallIslandRules.judge(area = 5000, width = 90, height = 80, expectedRadius = 10f))
    }

    @Test
    fun `fallback radius for an 8 ft table filling the frame`() {
        // 44" x 88" surface imaged at 10 px/inch: 440 x 880 = 387,200 px. A ball (1.125") is
        // 11.25 px in radius.
        assertEquals(11.25f, BallIslandRules.fallbackRadius(387_200.0), 0.1f)
    }

    @Test
    fun `classification by pixel make-up`() {
        assertEquals(BallKind.CUE, BallIslandRules.classify(white = 0.8f, dark = 0.05f, radiusPx = 8f))
        assertEquals(BallKind.EIGHT, BallIslandRules.classify(white = 0.1f, dark = 0.7f, radiusPx = 8f))
        assertEquals(BallKind.STRIPE, BallIslandRules.classify(white = 0.35f, dark = 0.1f, radiusPx = 8f))
        assertEquals(BallKind.SOLID, BallIslandRules.classify(white = 0.05f, dark = 0.2f, radiusPx = 8f))
        // A dark solid (maroon) with little dark share stays a solid, not the 8.
        assertEquals(BallKind.SOLID, BallIslandRules.classify(white = 0.05f, dark = 0.4f, radiusPx = 8f))
        assertEquals(BallKind.UNKNOWN, BallIslandRules.classify(white = 0.8f, dark = 0f, radiusPx = 2f))
    }
}
