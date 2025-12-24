package com.hereliesaz.cuedetat.ui

import com.hereliesaz.cuedetat.domain.ExperienceMode

object ZoomMapping {
    /**
     * Returns the min and max zoom levels for the given experience mode.
     */
    fun getZoomRange(mode: ExperienceMode?, isBeginnerViewLocked: Boolean = false): Pair<Float, Float> {
        // Updated to match usage signature
        if (mode == ExperienceMode.BEGINNER && isBeginnerViewLocked) {
             return 0f to 0f // Locked view, no zoom
        }
        return -50f to 0f
    }

    fun sliderToZoom(sliderPosition: Float, minZoom: Float, maxZoom: Float): Float {
        // Linear interpolation
        return minZoom + sliderPosition * (maxZoom - minZoom)
    }

    fun zoomToSlider(zoomLevel: Float, minZoom: Float, maxZoom: Float): Float {
        if (maxZoom == minZoom) return 0f
        return (zoomLevel - minZoom) / (maxZoom - minZoom)
    }
}
