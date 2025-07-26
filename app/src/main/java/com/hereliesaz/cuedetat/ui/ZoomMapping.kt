// app/src/main/java/com/hereliesaz/cuedetat/ui/ZoomMapping.kt
package com.hereliesaz.cuedetat.ui

import com.hereliesaz.cuedetat.view.state.ExperienceMode
import kotlin.math.ln
import kotlin.math.pow

internal object ZoomMapping {
    // --- Master Zoom Controls ---
    const val DEFAULT_ZOOM = 1.0f

    // Standard Mode Range
    private const val STANDARD_MIN_ZOOM = 0.5f
    private const val STANDARD_MAX_ZOOM = 4.0f

    // Beginner Mode Range
    private const val BEGINNER_MIN_ZOOM = 0.5f
    private const val BEGINNER_LOCKED_MAX_ZOOM = 9.6f
    // ----------------------------

    // Slider now operates on a -50 to +50 range.
    private const val SLIDER_RANGE = 100f // The total span of the slider (-50 to 50)

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
     */
    fun zoomToSlider(zoomFactor: Float, minZoom: Float, maxZoom: Float): Float {
        if (zoomFactor <= minZoom) return -50f
        if (zoomFactor >= maxZoom) return 50f

        val b = (maxZoom / minZoom).pow(1 / SLIDER_RANGE)
        // Formula: sliderValue = log(zoomFactor / MIN_ZOOM) / log(B)
        val normalizedSlider = (ln(zoomFactor / minZoom) / ln(b))
        // Denormalize from 0..100 back to -50..50
        return normalizedSlider - 50f
    }
}