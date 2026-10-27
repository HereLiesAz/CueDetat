package com.hereliesaz.cuedetat.view.gestures

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreeFingerDragTest {

    @Test
    fun `three fingers moving the same way drive the height`() {
        assertTrue(isCoherentVerticalDrag(listOf(6f, 5f, 8f)))
        assertTrue(isCoherentVerticalDrag(listOf(-6f, -5f, -8f)))
    }

    @Test
    fun `a resting thumb beside a two-finger gesture does not`() {
        assertFalse(isCoherentVerticalDrag(listOf(0f, 12f, 10f)))
        assertFalse(isCoherentVerticalDrag(listOf(0.4f, -9f, -7f)))
    }

    @Test
    fun `fingers moving against each other do not`() {
        assertFalse(isCoherentVerticalDrag(listOf(9f, -9f, 8f)))
    }

    @Test
    fun `fewer than three fingers never do`() {
        assertFalse(isCoherentVerticalDrag(listOf(9f, 9f)))
        assertFalse(isCoherentVerticalDrag(emptyList()))
    }
}
