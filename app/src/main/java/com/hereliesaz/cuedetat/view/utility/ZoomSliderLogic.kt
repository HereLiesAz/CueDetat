package com.hereliesaz.cuedetat.view.utility

import com.hereliesaz.cuedetat.config.AppConfig
import kotlin.math.pow // For pow function

object ZoomSliderLogic {

    // Slider Progress Convention:
    // 0   => Corresponds to effective MIN_ZOOM_FACTOR for slider (Most Zoomed OUT)
    // 100 => Corresponds to effective MAX_ZOOM_FACTOR for slider (Most Zoomed IN, capped at old midpoint)

    // --- Original Configuration for determining the cap (effectiveMaxZoomForSlider) ---
    // These are used ONLY to calculate the effectiveMaxZoomForSlider based on the old midpoint logic.
    private const val ORIGINAL_FAVORED_ZOOM_FRACTION = 0.3f
    private const val ORIGINAL_SLIDER_PROGRESS_FOR_FAVORED = 95.0f
    // --- End Original Configuration ---

    // --- New Configuration for Smooth Power-Law Mapping ---
    // An exponent > 1.0 gives more sensitivity to the lower progress values (zoomed-out).
    // An exponent < 1.0 gives more sensitivity to the higher progress values (zoomed-in).
    // Let's start with 2.0 for more control at the zoomed-out end.
    private const val SLIDER_EXPONENT = 2.0 // Try values like 1.5, 2.0, 2.5
    // --- End New Configuration ---


    // Calculate the zoom factor that the original midpoint of the slider (progress 50) maps to.
    // This will become our new effective maximum zoom-in for the slider.
    private val effectiveMaxZoomForSlider: Float by lazy {
        // Helper to use the *original* two-part linear logic just for this one-time calculation
        fun calculateOriginalMidpointZoom(): Float {
            val progress = 50f // Midpoint
            val minZoom = AppConfig.MIN_ZOOM_FACTOR
            val maxZoom = AppConfig.MAX_ZOOM_FACTOR
            val totalOriginalZoomRange = maxZoom - minZoom
            val originalFavoredUpperLimit = minZoom + (totalOriginalZoomRange * ORIGINAL_FAVORED_ZOOM_FRACTION)
            val targetZoom: Float

            if (progress <= ORIGINAL_SLIDER_PROGRESS_FOR_FAVORED) {
                val t = if (ORIGINAL_SLIDER_PROGRESS_FOR_FAVORED == 0f) (if (progress == 0f) 0f else 1f)
                else progress / ORIGINAL_SLIDER_PROGRESS_FOR_FAVORED
                val zoomRangeInFavoredSegment = originalFavoredUpperLimit - minZoom
                targetZoom = minZoom + (t * zoomRangeInFavoredSegment)
            } else {
                val remainingSliderPercent = 100f - ORIGINAL_SLIDER_PROGRESS_FOR_FAVORED
                if (remainingSliderPercent > 0f) {
                    val tRemaining = (progress - ORIGINAL_SLIDER_PROGRESS_FOR_FAVORED) / remainingSliderPercent
                    val zoomRangeInRemainingSegment = maxZoom - originalFavoredUpperLimit
                    targetZoom = originalFavoredUpperLimit + (tRemaining * zoomRangeInRemainingSegment)
                } else {
                    targetZoom = originalFavoredUpperLimit
                }
            }
            return targetZoom.coerceIn(minZoom, maxZoom)
        }
        calculateOriginalMidpointZoom()
    }

    private val currentMinZoom = AppConfig.MIN_ZOOM_FACTOR
    private val currentMaxZoom = effectiveMaxZoomForSlider // Use the capped value
    private val currentSliderEffectiveZoomRange = currentMaxZoom - currentMinZoom


    fun convertSliderProgressToZoomFactor(progressInt: Int): Float {
        if (currentSliderEffectiveZoomRange <= 0.0001f) { // Avoid issues if range is negligible
            return currentMinZoom
        }
        val progressNormalized = progressInt.toFloat() / 100.0f // Normalize progress to 0.0 - 1.0

        // Apply power function: MIN + RANGE * (progress_normalized ^ exponent)
        val scaledProgress = progressNormalized.pow(SLIDER_EXPONENT.toFloat())
        val targetZoom = currentMinZoom + (currentSliderEffectiveZoomRange * scaledProgress)

        return targetZoom.coerceIn(currentMinZoom, currentMaxZoom)
    }

    fun convertZoomFactorToSliderProgress(zoomFactorVal: Float): Int {
        if (currentSliderEffectiveZoomRange <= 0.0001f) { // Avoid issues if range is negligible
            return if (zoomFactorVal <= currentMinZoom) 0 else 100
        }
        val currentZoomFactor = zoomFactorVal.coerceIn(currentMinZoom, currentMaxZoom)

        // Inverse of power function: progress_normalized = ((zoomFactor - MIN) / RANGE) ^ (1 / exponent)
        val normalizedZoom = (currentZoomFactor - currentMinZoom) / currentSliderEffectiveZoomRange

        // Ensure normalizedZoom is not negative before taking root (can happen due to float precision)
        val baseForPow = normalizedZoom.coerceAtLeast(0.0f)

        val progressNormalized = baseForPow.pow(1.0f / SLIDER_EXPONENT.toFloat())
        val progress = progressNormalized * 100.0f

        return progress.toInt().coerceIn(0, 100)
    }
}