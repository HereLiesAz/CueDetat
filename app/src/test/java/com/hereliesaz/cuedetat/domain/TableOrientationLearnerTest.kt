package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableOrientationLearnerTest {

    private fun sample(yaw: Float, rot: Float, pitch: Float = 30f, zoom: Float = 1f) =
        TablePoseSample(yaw, pitch, 0f, rot, zoom, 0f, 0f, 0L)

    @Test
    fun `no samples, no prediction`() {
        assertNull(TableOrientationLearner.predict(emptyList(), 0f, 30f))
    }

    @Test
    fun `one sample predicts the table turning against the phone`() {
        // Locked at yaw 10, rotation 5: heading constant c = 5 - (-1)(10) = 15.
        // Phone now at yaw 30: rotation = -30 + 15 = -15.
        val p = TableOrientationLearner.predict(listOf(sample(10f, 5f)), yawDeg = 30f, pitchDeg = 30f)!!
        assertEquals(-15f, p.rotationDeg, 1e-3f)
    }

    @Test
    fun `half-turn ambiguity is folded, not averaged into nonsense`() {
        // Rotations 89 and -89 are 2 degrees apart modulo 180, not 178. Same yaw.
        val p = TableOrientationLearner.predict(listOf(sample(0f, 89f), sample(0f, -89f)), 0f, 30f)!!
        assertEquals(90f, kotlin.math.abs(p.rotationDeg), 1e-3f)
        assertTrue(p.confidence > 0f)
    }

    @Test
    fun `confidence grows with agreeing samples`() {
        val one = TableOrientationLearner.predict(listOf(sample(0f, 10f)), 0f, 30f)!!
        val five = TableOrientationLearner.predict(List(5) { sample(it * 10f, 10f - it * 10f) }, 0f, 30f)!!
        assertTrue(five.confidence > one.confidence)
        // Five samples, all with c = rot + yaw = 10: perfect agreement, full count -> 1.
        assertEquals(1f, five.confidence, 1e-3f)
        assertEquals(10f, five.rotationDeg, 1e-3f)
    }

    @Test
    fun `zoom is regressed on pitch and clamped to what was seen`() {
        // zoom = 0.5 + 0.02 * pitch at pitch 20, 30, 40 -> 0.9, 1.1, 1.3.
        val s = listOf(sample(0f, 0f, 20f, 0.9f), sample(0f, 0f, 30f, 1.1f), sample(0f, 0f, 40f, 1.3f))
        assertEquals(1.0f, TableOrientationLearner.predict(s, 0f, 25f)!!.zoom, 1e-3f)
        // Pitch 60 would extrapolate to 1.7; clamped to the largest seen, 1.3.
        assertEquals(1.3f, TableOrientationLearner.predict(s, 0f, 60f)!!.zoom, 1e-3f)
    }

    @Test
    fun `normalize and diff modulo 180`() {
        assertEquals(-80f, TableOrientationLearner.normalize180(100f), 1e-3f)
        assertEquals(90f, TableOrientationLearner.normalize180(-90f), 1e-3f)
        assertEquals(2f, TableOrientationLearner.diff180(89f, -89f), 1e-3f)
    }
}
