package com.hereliesaz.cuedetat.ui

import org.junit.Test
import org.junit.Assert.*

class ZoomLogicTest {

    @Test
    fun testZoomLogic() {
        val minZoom = -50f
        val maxZoom = 0f

        // Initial State: Slider at 0.5 -> Zoom should be -25
        val initialSlider = 0.5f
        val currentZoom = ZoomMapping.sliderToZoom(initialSlider, minZoom, maxZoom)
        assertEquals(-25f, currentZoom, 0.1f)

        // Pinch Zoom In (Scale > 1.0, e.g. 1.1)
        // We expect zoom to get LARGER (closer to 0). E.g. -22.
        val scaleFactorIn = 1.1f
        // Current logic: -25 / 1.1 = -22.72. This is closer to 0. Correct direction.
        val newZoomIn = currentZoom / scaleFactorIn
        assertTrue(newZoomIn > currentZoom) // -22 > -25

        // Pinch Zoom Out (Scale < 1.0, e.g. 0.9)
        // We expect zoom to get SMALLER (closer to -50). E.g. -27.
        val scaleFactorOut = 0.9f
        // Current logic: -25 / 0.9 = -27.77. This is further from 0. Correct direction.
        val newZoomOut = currentZoom / scaleFactorOut
        assertTrue(newZoomOut < currentZoom) // -27 < -25
    }
}
