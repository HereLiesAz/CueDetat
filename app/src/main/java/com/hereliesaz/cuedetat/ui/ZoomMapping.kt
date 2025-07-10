package com.hereliesaz.cuedetat.ui

import kotlin.math.log
import kotlin.math.pow

/**
 * Maps the linear slider position to a non-linear zoom factor.
 * This provides a more intuitive and controlled zoom experience.
 */
object ZoomMapping {

    const val MIN_ZOOM = 0.25f
    const val MAX_ZOOM = 4.0f
    const val DEFAULT_ZOOM = 1.0f

    private const val SLIDER_MIN = -50f
    private const val SLIDER_MAX = 50f

    // An exponential base calculated to map the slider range to the zoom range.
    // base = exp(log(MAX_ZOOM) / SLIDER_MAX)
    private const val ZOOM_BASE = 1.028135f // exp(ln(4.0) / 50)

    /**
     * Converts a slider position (from -50.0 to 50.0) to a zoom factor.
     * @param position The raw value from the slider.
     * @return The calculated zoom factor, coerced into the valid range.
     */
    fun sliderToZoom(position: Float): Float {
        return ZOOM_BASE.pow(position).coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /**
     * Converts a zoom factor back to its corresponding slider position.
     * @param zoom The zoom factor.
     * @return The calculated slider position.
     */
    fun zoomToSlider(zoom: Float): Float {
        return log(zoom.coerceIn(MIN_ZOOM, MAX_ZOOM), ZOOM_BASE)
    }
}