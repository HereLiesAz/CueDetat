package com.hereliesaz.cuedetat.view.utility

import com.hereliesaz.cuedetat.config.AppConfig
import kotlin.math.pow // For pow function

object ZoomSliderLogic {

    // Slider Progress Convention:
    // 0   => Corresponds to minZoom
    // 100 => Corresponds to maxZoom

    // An exponent > 1.0 gives more sensitivity to the lower progress values (zoomed-out).
    // An exponent < 1.0 gives more sensitivity to the higher progress values (zoomed-in).
    private const val SLIDER_EXPONENT = 2.0 // Try values like 1.5, 2.0, 2.5

    /**
     * Converts slider progress (0-100) to a camera zoom factor.
     * @param progressInt The slider's progress (0-100).
     * @param minZoom The minimum zoom ratio supported by the camera.
     * @param maxZoom The maximum zoom ratio supported by the camera.
     * @return The calculated camera zoom factor.
     */
    fun convertSliderProgressToZoomFactor(progressInt: Int, minZoom: Float, maxZoom: Float): Float {
        val effectiveZoomRange = maxZoom - minZoom
        if (effectiveZoomRange <= 0.0001f) { // Avoid issues if range is negligible (e.g., fixed zoom camera)
            return minZoom
        }
        val progressNormalized = progressInt.toFloat() / 100.0f // Normalize progress to 0.0 - 1.0

        // Apply power function: MIN + RANGE * (progress_normalized ^ exponent)
        val scaledProgress = progressNormalized.pow(SLIDER_EXPONENT.toFloat())
        val targetZoom = minZoom + (effectiveZoomRange * scaledProgress)

        return targetZoom.coerceIn(minZoom, maxZoom)
    }

    /**
     * Converts a camera zoom factor to slider progress (0-100).
     * @param zoomFactorVal The current camera zoom factor.
     * @param minZoom The minimum zoom ratio supported by the camera.
     * @param maxZoom The maximum zoom ratio supported by the camera.
     * @return The calculated slider progress (0-100).
     */
    fun convertZoomFactorToSliderProgress(zoomFactorVal: Float, minZoom: Float, maxZoom: Float): Int {
        val effectiveZoomRange = maxZoom - minZoom
        if (effectiveZoomRange <= 0.0001f) { // Avoid issues if range is negligible
            return if (zoomFactorVal <= minZoom) 0 else 100
        }
        val currentZoomFactor = zoomFactorVal.coerceIn(minZoom, maxZoom)

        // Inverse of power function: progress_normalized = ((zoomFactor - MIN) / RANGE) ^ (1 / exponent)
        val normalizedZoom = (currentZoomFactor - minZoom) / effectiveZoomRange

        // Ensure normalizedZoom is not negative before taking root (can happen due to float precision)
        val baseForPow = normalizedZoom.coerceAtLeast(0.0f)

        val progressNormalized = baseForPow.pow(1.0f / SLIDER_EXPONENT.toFloat())
        val progress = progressNormalized * 100.0f

        return progress.toInt().coerceIn(0, 100)
    }
}