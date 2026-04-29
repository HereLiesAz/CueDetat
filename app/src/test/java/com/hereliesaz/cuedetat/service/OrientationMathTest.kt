package com.hereliesaz.cuedetat.service

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class OrientationMathTest {

    @Test
    fun identityRotationProducesIdentityDelta() {
        val identity = floatArrayOf(0f, 0f, 0f, 1f)
        val delta = OrientationTrackingService.quaternionDelta(identity, identity)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), delta, 0.001f)
    }

    @Test
    fun halfTurnAroundYAxisProducesCorrectDelta() {
        val identity = floatArrayOf(0f, 0f, 0f, 1f)
        val halfTurnY = floatArrayOf(0f, 1f, 0f, 0f)
        val delta = OrientationTrackingService.quaternionDelta(identity, halfTurnY)
        assertArrayEquals(halfTurnY, delta, 0.001f)
    }
}
