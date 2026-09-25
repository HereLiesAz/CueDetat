package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSamplerTest {

    @Test
    fun `due after the interval, not before`() {
        assertFalse(CaptureSampler.due(nowMs = 11_999, lastKeptMs = 10_000))
        assertTrue(CaptureSampler.due(nowMs = 12_000, lastKeptMs = 10_000))
    }

    @Test
    fun `steady when turning slowly`() {
        // 0.5 degrees in 100 ms = 5 deg/s.
        assertTrue(CaptureSampler.steady(floatArrayOf(10f, 30f, 0f), 0, floatArrayOf(10.5f, 30f, 0f), 100))
    }

    @Test
    fun `not steady when swinging`() {
        // 3 degrees of pitch in 100 ms = 30 deg/s.
        assertFalse(CaptureSampler.steady(floatArrayOf(10f, 30f, 0f), 0, floatArrayOf(10f, 33f, 0f), 100))
    }

    @Test
    fun `yaw wrap is a small turn, not a full one`() {
        // 179.8 -> -179.8 is 0.4 degrees across the seam.
        assertTrue(CaptureSampler.steady(floatArrayOf(179.8f, 0f, 0f), 0, floatArrayOf(-179.8f, 0f, 0f), 100))
    }

    @Test
    fun `no previous or stale reading is not steady`() {
        assertFalse(CaptureSampler.steady(null, 0, floatArrayOf(0f, 0f, 0f), 100))
        assertFalse(CaptureSampler.steady(floatArrayOf(0f, 0f, 0f), 0, floatArrayOf(0f, 0f, 0f), 5_000))
    }

    @Test
    fun `upright mapping sends raw corners to the right places`() {
        // Raw 400 x 300. Rotated 90 clockwise the image is 300 x 400 and the raw top-left
        // corner lands at the top-right.
        assertEquals(300f to 0f, CaptureSampler.toUpright(0f, 0f, 400, 300, 90))
        assertEquals(300f to 400f, CaptureSampler.toUpright(400f, 0f, 400, 300, 90))
        // 270: raw top-left lands bottom-left.
        assertEquals(0f to 400f, CaptureSampler.toUpright(0f, 0f, 400, 300, 270))
        // 180: raw top-left lands bottom-right.
        assertEquals(400f to 300f, CaptureSampler.toUpright(0f, 0f, 400, 300, 180))
        assertEquals(10f to 20f, CaptureSampler.toUpright(10f, 20f, 400, 300, 0))
        assertEquals(300 to 400, CaptureSampler.uprightSize(400, 300, 90))
        assertEquals(400 to 300, CaptureSampler.uprightSize(400, 300, 180))
    }
}
