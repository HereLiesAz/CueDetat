package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.data.FullOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * The gate passes an orientation only when it has moved enough to show. Expected numbers are worked
 * from the 0.03 degree epsilon (about one pixel), not read from the code.
 */
class OrientationGateTest {

    private fun o(yaw: Float, pitch: Float, roll: Float) = FullOrientation(yaw, pitch, roll)

    @Test
    fun `the first orientation always passes`() {
        assertTrue(OrientationGate.worthApplying(null, o(10f, 20f, 0f)))
    }

    @Test
    fun `a change under the epsilon on every axis is dropped, over it on any axis passes`() {
        val applied = o(10f, 20f, 30f)
        assertFalse(OrientationGate.worthApplying(applied, o(10.02f, 20.02f, 30.02f)))
        assertTrue(OrientationGate.worthApplying(applied, o(10.04f, 20f, 30f)))
        assertTrue(OrientationGate.worthApplying(applied, o(10f, 19.96f, 30f)))
        assertTrue(OrientationGate.worthApplying(applied, o(10f, 20f, 30.04f)))
    }

    @Test
    fun `yaw and roll compare along the shortest arc`() {
        // 179.99 and -179.99 are 0.02 degrees apart, not 359.98.
        assertFalse(OrientationGate.worthApplying(o(179.99f, 0f, 179.99f), o(-179.99f, 0f, -179.99f)))
        assertTrue(OrientationGate.worthApplying(o(179.99f, 0f, 0f), o(-179.95f, 0f, 0f)))
    }

    @Test
    fun `sensor noise on a still phone applies nothing after the first`() {
        val random = Random(7)
        var applied: FullOrientation? = null
        var count = 0
        repeat(2000) {
            val next = o(
                45f + (random.nextFloat() - 0.5f) * 0.02f,   // +-0.01 degrees: two samples differ by at most 0.02
                -30f + (random.nextFloat() - 0.5f) * 0.02f,
                2f + (random.nextFloat() - 0.5f) * 0.02f,
            )
            if (OrientationGate.worthApplying(applied, next)) { applied = next; count++ }
        }
        assertEquals("applied updates on a still phone", 1, count)
    }

    @Test
    fun `slow drift still gets through, in steps no larger than the epsilon plus one sample`() {
        // 0.001 degrees per sample for 1000 samples is one degree of real pitch change.
        var applied: FullOrientation? = null
        var count = 0
        var worstLag = 0f
        for (i in 0..1000) {
            val truth = o(0f, i * 0.001f, 0f)
            if (OrientationGate.worthApplying(applied, truth)) { applied = truth; count++ }
            worstLag = maxOf(worstLag, abs(truth.pitch - applied!!.pitch))
        }
        // One degree in steps of just over 0.03 is 32 or 33 updates, plus the first.
        assertTrue("updates for one degree of drift: $count", count in 30..36)
        assertTrue("the applied pitch never trails the truth by more than the epsilon: $worstLag", worstLag <= 0.031f)
    }
}
