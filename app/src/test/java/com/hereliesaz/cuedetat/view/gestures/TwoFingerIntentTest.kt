package com.hereliesaz.cuedetat.view.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A pinch's stray twist and drift must not rotate or pan the table; a deliberate one must. */
class TwoFingerIntentTest {

    @Test
    fun `small twist and drift stay below the slop`() {
        val intent = TwoFingerIntent()
        repeat(10) {
            val r = intent.accept(0.1f, 0.5f, 0.2f) // 1 deg, ~5 px in total
            assertFalse(r.rotate)
            assertFalse(r.pan)
        }
    }

    @Test
    fun `jitter that cancels never opens the rotation channel`() {
        val intent = TwoFingerIntent()
        repeat(100) { i ->
            val r = intent.accept(if (i % 2 == 0) 2f else -2f, 0f, 0f)
            assertFalse(r.rotate)
        }
    }

    @Test
    fun `a deliberate rotation opens its channel and keeps it open`() {
        val intent = TwoFingerIntent()
        assertFalse(intent.accept(2f, 0f, 0f).rotate)
        assertTrue(intent.accept(2f, 0f, 0f).rotate) // 4 deg accumulated
        assertTrue(intent.accept(-0.5f, 0f, 0f).rotate) // stays open
    }

    @Test
    fun `rotation and pan open independently`() {
        val intent = TwoFingerIntent()
        val r = intent.accept(0f, 20f, 0f)
        assertEquals(false, r.rotate)
        assertEquals(true, r.pan)
    }
}
