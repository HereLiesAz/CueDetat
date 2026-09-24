package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraViewMappingTest {

    private fun assertMaps(m: FloatArray, x: Float, y: Float, ex: Float, ey: Float) {
        val (vx, vy) = CameraViewMapping.map(m, x, y)
        assertEquals(ex, vx, 1e-3f)
        assertEquals(ey, vy, 1e-3f)
    }

    @Test
    fun `no rotation and matching size is identity`() {
        val m = CameraViewMapping.fillCenter(640, 480, 0, 640, 480)
        assertMaps(m, 0f, 0f, 0f, 0f)
        assertMaps(m, 640f, 480f, 640f, 480f)
    }

    @Test
    fun `portrait phone - 90 degree sensor, centre-cropped`() {
        // Raw 640x480 rotated upright is 480x640. View 1080x2400: scale = max(1080/480, 2400/640)
        // = max(2.25, 3.75) = 3.75, so the upright image is 1800x2400 and 360 px is cropped from
        // each side (tx = (1080 - 1800) / 2 = -360).
        val m = CameraViewMapping.fillCenter(640, 480, 90, 1080, 2400)
        // Image centre lands on view centre.
        assertMaps(m, 320f, 240f, 540f, 1200f)
        // Raw bottom-left (0, 480) is upright top-left: (0, 0) * 3.75 - (360, 0).
        assertMaps(m, 0f, 480f, -360f, 0f)
        // Raw top-left (0, 0) is upright top-right: (480, 0) -> 480 * 3.75 - 360 = 1440.
        assertMaps(m, 0f, 0f, 1440f, 0f)
    }

    @Test
    fun `270 degrees mirrors 90`() {
        val m = CameraViewMapping.fillCenter(640, 480, 270, 1080, 2400)
        assertMaps(m, 320f, 240f, 540f, 1200f)
        // Raw top-left (0, 0) is upright bottom-left: (0, 640) -> (-360, 2400).
        assertMaps(m, 0f, 0f, -360f, 2400f)
    }

    @Test
    fun `180 degrees flips both axes`() {
        val m = CameraViewMapping.fillCenter(640, 480, 180, 640, 480)
        assertMaps(m, 0f, 0f, 640f, 480f)
        assertMaps(m, 640f, 480f, 0f, 0f)
    }
}
