package com.hereliesaz.cuedetatlite.ui

object ZoomMapping {
    // Reverted to a much lower range to increase the zoom-out capability.
    const val MIN_ZOOM = 0.33f
    const val MAX_ZOOM = 0.8f
    const val SLIDER_MIN = 0f
    const val SLIDER_MAX = 100f
    // A sensible default within the new, lower range.
    const val DEFAULT_ZOOM = 0.4f

    fun sliderToZoom(sliderValue: Float): Float {
        val range = MAX_ZOOM - MIN_ZOOM
        // Map the full 0-100 slider range to the new zoom range.
        val normalized = sliderValue.coerceIn(SLIDER_MIN, SLIDER_MAX) / (SLIDER_MAX - SLIDER_MIN)
        return (range * normalized) + MIN_ZOOM
    }

    fun zoomToSlider(zoomValue: Float): Float {
        val range = MAX_ZOOM - MIN_ZOOM
        if (range == 0f) return SLIDER_MIN
        val normalized = (zoomValue.coerceIn(MIN_ZOOM, MAX_ZOOM) - MIN_ZOOM) / range
        return normalized * (SLIDER_MAX - SLIDER_MIN) + SLIDER_MIN
    }
}
