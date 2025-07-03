package com.hereliesaz.cuedetatlite.ui

object ZoomMapping {
    const val MIN_ZOOM = 1.475f
    const val MAX_ZOOM = 2.6f
    const val SLIDER_MIN = 0f
    const val SLIDER_MAX = 100f
    const val SLIDER_EFFECTIVE_MIN = 65f
    const val DEFAULT_ZOOM = 1.475f // Baseline for text scaling

    fun sliderToZoom(sliderValue: Float): Float {
        val range = MAX_ZOOM - MIN_ZOOM
        val effectiveSliderValue = sliderValue.coerceIn(SLIDER_EFFECTIVE_MIN, SLIDER_MAX)
        val normalized = (effectiveSliderValue - SLIDER_EFFECTIVE_MIN) / (SLIDER_MAX - SLIDER_EFFECTIVE_MIN)
        return (range * normalized) + MIN_ZOOM
    }

    fun zoomToSlider(zoomValue: Float): Float {
        val range = MAX_ZOOM - MIN_ZOOM
        if (range == 0f) return SLIDER_EFFECTIVE_MIN
        val normalized = (zoomValue.coerceIn(MIN_ZOOM, MAX_ZOOM) - MIN_ZOOM) / range
        return normalized * (SLIDER_MAX - SLIDER_EFFECTIVE_MIN) + SLIDER_EFFECTIVE_MIN
    }
}
