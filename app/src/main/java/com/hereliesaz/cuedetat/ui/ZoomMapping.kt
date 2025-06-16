// app/src/main/java/com/hereliesaz/cuedetat/ui/ZoomMapping.kt
package com.hereliesaz.cuedetat.ui

import kotlin.math.ln
import kotlin.math.pow

/**
 * Maps the linear 0-100 slider position to a nonlinear scale factor for zooming.
 * This provides finer control at higher zoom levels.
 */
internal object ZoomMapping {
    // Defines the effective zoom range. A scale of 1.0 means 1 logical inch = 1 pixel.
    const val MIN_SCALE = 5f   // Most zoomed out
    const val DEFAULT_SCALE = 20f
    const val MAX_SCALE = 100f  // Most zoomed in

    // This constant determines the curve of the exponential zoom.
    // It is calculated to map the slider's 0-100 range precisely to the MIN_SCALE-MAX_SCALE range.
    private val B = (MAX_SCALE / MIN_SCALE).pow(0.01f)

    /**
     * Converts a slider position (0f-100f) to an exponential scale factor.
     */
    fun sliderToScale(sliderValue: Float): Float = MIN_SCALE * B.pow(sliderValue)

    /**
     * Converts a scale factor back to its corresponding slider position (0f-100f).
     */
    fun scaleToSlider(scaleFactor: Float): Float {
        val clampedScale = scaleFactor.coerceIn(MIN_SCALE, MAX_SCALE)
        // Formula: sliderValue = log(scaleFactor / MIN_SCALE) / log(B)
        return (ln(clampedScale / MIN_SCALE) / ln(B))
    }
}
