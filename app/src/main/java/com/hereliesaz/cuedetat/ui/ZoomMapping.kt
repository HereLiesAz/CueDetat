// app/src/main/java/com/hereliesaz/cuedetat/ui/ZoomMapping.kt
package com.hereliesaz.cuedetat.ui

import com.hereliesaz.cuedetat.domain.ExperienceMode
import kotlin.math.ln
import kotlin.math.pow

/**
 * Utility for mapping linear slider values to exponential zoom levels.
 *
 * This provides a more natural zooming experience, where each step on the slider
 * feels like a consistent percentage change in magnification, rather than a linear change.
 */
internal object ZoomMapping {
    // --- Master Zoom Controls ---
    const val DEFAULT_ZOOM = 1.0f

    // Standard Mode Range (0.5x to 4.0x)
    private const val STANDARD_MIN_ZOOM = 0.5f
    private const val STANDARD_MAX_ZOOM = 4.0f

    // Beginner Mode Range (0.5x to 9.6x)
    private const val BEGINNER_MIN_ZOOM = 0.5f
    private const val BEGINNER_LOCKED_MAX_ZOOM = 9.6f
    // ----------------------------

    // The slider UI operates on a normalized -50 to +50 range for integer stepping.
    private const val SLIDER_RANGE = 100f

    /**
     * Determines the allowed zoom range based on the current mode.
     */
    fun getZoomRange(
        mode: ExperienceMode?,
        isBeginnerViewLocked: Boolean = false,
    ): Pair<Float, Float> {
        return if (mode == ExperienceMode.BEGINNER && isBeginnerViewLocked) {
            BEGINNER_MIN_ZOOM to BEGINNER_LOCKED_MAX_ZOOM
        } else {
            STANDARD_MIN_ZOOM to STANDARD_MAX_ZOOM
        }
    }

    /**
     * Converts a symmetrical slider position (-50f to 50f) to an exponential
     * zoom factor.
     *
     * Formula: Zoom = MinZoom * B^(slider + 50)
     * where B = (MaxZoom / MinZoom)^(1/100)
     */
    fun sliderToZoom(sliderValue: Float, minZoom: Float, maxZoom: Float): Float {
        // Normalize the slider value from -50..50 to 0..100 for the formula
        val normalizedSlider = sliderValue + 50f
        val b = (maxZoom / minZoom).pow(1 / SLIDER_RANGE)
        return minZoom * b.pow(normalizedSlider)
    }


    /**
     * Converts a zoom factor back to its corresponding symmetrical slider
     * position (-50f to 50f).
     *
     * Formula: slider = log(Zoom / MinZoom) / log(B) - 50
     */
    fun zoomToSlider(zoomFactor: Float, minZoom: Float, maxZoom: Float): Float {
        if (zoomFactor <= minZoom) return -50f
        if (zoomFactor >= maxZoom) return 50f

        val b = (maxZoom / minZoom).pow(1 / SLIDER_RANGE)
        // Inverse calculation.
        val normalizedSlider = (ln(zoomFactor / minZoom) / ln(b))
        // Denormalize from 0..100 back to -50..50
        return normalizedSlider - 50f
    }
}
