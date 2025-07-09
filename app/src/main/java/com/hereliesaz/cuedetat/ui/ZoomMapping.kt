// app/src/main/java/com/hereliesaz/cuedetat/ui/ZoomMapping.kt
package com.hereliesaz.cuedetat.ui

import kotlin.math.ln
import kotlin.math.pow

internal object ZoomMapping {
    // --- Master Zoom Controls ---
    const val MIN_ZOOM = 0.084f
    const val DEFAULT_ZOOM = 0.4f
    const val MAX_ZOOM = 0.6f
    // ----------------------------

    // Slider now operates on a -50 to +50 range.
    private const val SLIDER_RANGE = 100f // The total span of the slider (-50 to 50)

    // Recalculate B based on the new total range.
    private val B = (MAX_ZOOM / MIN_ZOOM).pow(1 / SLIDER_RANGE)

    /**
     * Converts a symmetrical slider position (-50f to 50f) to an exponential zoom factor.
     */
    fun sliderToZoom(sliderValue: Float): Float {
        // Normalize the slider value from -50..50 to 0..100 for the formula
        val normalizedSlider = sliderValue + 50f
        return MIN_ZOOM * B.pow(normalizedSlider)
    }


    /**
     * Converts a zoom factor back to its corresponding symmetrical slider position (-50f to 50f).
     */
    fun zoomToSlider(zoomFactor: Float): Float {
        if (zoomFactor <= MIN_ZOOM) return -50f
        if (zoomFactor >= MAX_ZOOM) return 50f
        // Formula: sliderValue = log(zoomFactor / MIN_ZOOM) / log(B)
        val normalizedSlider = (ln(zoomFactor / MIN_ZOOM) / ln(B))
        // Denormalize from 0..100 back to -50..50
        return normalizedSlider - 50f
    }
}