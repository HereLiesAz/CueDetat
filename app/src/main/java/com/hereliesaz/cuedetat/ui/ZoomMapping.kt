// app/src/main/java/com/hereliesaz/cuedetat/ui/ZoomMapping.kt
package com.hereliesaz.cuedetat.ui

import kotlin.math.ln
import kotlin.math.pow

internal object ZoomMapping {
    // --- Master Zoom Controls ---
    const val MIN_ZOOM = 0.084f // Changed from 0.05f to limit max zoom-out
    const val DEFAULT_ZOOM = 0.4f
    const val MAX_ZOOM = 0.6f
    // ----------------------------

    // This constant determines the curve of the exponential zoom.
    // It is calculated to map the slider's 0-100 range precisely to the MIN_ZOOM-MAX_ZOOM range.
    // Formula: B = (MAX_ZOOM / MIN_ZOOM) ^ (1 / 100)
    private val B = (MAX_ZOOM / MIN_ZOOM).pow(0.01f)

    /**
     * Converts a slider position (0f-100f) to an exponential zoom factor.
     */
    fun sliderToZoom(sliderValue: Float): Float = MIN_ZOOM * B.pow(sliderValue)

    /**
     * Converts a zoom factor back to its corresponding slider position (0f-100f).
     */
    fun zoomToSlider(zoomFactor: Float): Float {
        if (zoomFactor <= MIN_ZOOM) return 0f
        if (zoomFactor >= MAX_ZOOM) return 100f
        // Formula: sliderValue = log(zoomFactor / MIN_ZOOM) / log(B)
        return (ln(zoomFactor / MIN_ZOOM) / ln(B))
    }
}