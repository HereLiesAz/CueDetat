package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeltColorMatchTest {

    private val green = floatArrayOf(120f, 0.6f, 0.5f)

    @Test
    fun `hue distance wraps around red`() {
        assertEquals(20f, FeltColorMatch.hueDistance(350f, 10f), 1e-4f)
        assertEquals(180f, FeltColorMatch.hueDistance(0f, 180f), 1e-4f)
    }

    @Test
    fun `shadowed felt still matches`() {
        assertTrue(FeltColorMatch.matches(floatArrayOf(125f, 0.55f, 0.2f), green))
    }

    @Test
    fun `wood rail does not match`() {
        assertFalse(FeltColorMatch.matches(floatArrayOf(30f, 0.6f, 0.4f), green))
    }

    @Test
    fun `grey floor does not match despite green-ish hue`() {
        assertFalse(FeltColorMatch.matches(floatArrayOf(118f, 0.1f, 0.5f), green))
    }

    @Test
    fun `black pocket does not match`() {
        assertFalse(FeltColorMatch.matches(floatArrayOf(120f, 0.6f, 0.05f), green))
    }

    @Test
    fun `red felt matches across the hue wrap`() {
        assertTrue(FeltColorMatch.matches(floatArrayOf(355f, 0.7f, 0.5f), floatArrayOf(5f, 0.7f, 0.5f)))
    }
}
