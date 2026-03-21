// app/src/main/java/com/hereliesaz/cuedetat/ui/ZoomMapping.kt
package com.hereliesaz.cuedetat.ui

import com.hereliesaz.cuedetat.domain.ExperienceMode
import kotlin.math.ln
import kotlin.math.pow

internal object ZoomMapping {
    const val DEFAULT_ZOOM = 1.0f

    private const val STANDARD_MIN_ZOOM = 0.2f
    private const val STANDARD_MAX_ZOOM = 4.0f

    private const val BEGINNER_MIN_ZOOM = 0.5f
    // Increased the cap significantly so the balls draw much larger
    private const val BEGINNER_LOCKED_MAX_ZOOM = 20.0f

    private const val SLIDER_RANGE = 100f

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

    fun sliderToZoom(sliderValue: Float, minZoom: Float, maxZoom: Float): Float {
        val normalizedSlider = sliderValue + 50f
        val b = (maxZoom / minZoom).pow(1 / SLIDER_RANGE)
        return minZoom * b.pow(normalizedSlider)
    }

    fun zoomToSlider(zoomFactor: Float, minZoom: Float, maxZoom: Float): Float {
        if (zoomFactor <= minZoom) return -50f
        if (zoomFactor >= maxZoom) return 50f

        val b = (maxZoom / minZoom).pow(1 / SLIDER_RANGE)
        val normalizedSlider = (ln(zoomFactor / minZoom) / ln(b))
        return normalizedSlider - 50f
    }
}