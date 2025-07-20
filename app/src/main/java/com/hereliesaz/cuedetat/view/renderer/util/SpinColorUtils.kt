// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/SpinColorUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object SpinColorUtils {

    /**
     * The single source of truth for converting a spin angle and distance into a color.
     * This logic must be used by both the UI (SpinControl) and the domain (CalculateSpinPaths).
     * @param angleDegrees The angle of the spin, where 0 is to the right (3 o'clock).
     * @param distance The normalized distance from the center (0.0) to the edge (1.0).
     * @return The calculated Color.
     */
    fun getColorFromAngleAndDistance(angleDegrees: Float, distance: Float): Color {
        // Corrects the mathematical angle to align with the visual color wheel.
        val adjustedAngle = (angleDegrees + 360f) % 360f

        val stops = listOf(
            0f to Color.Red,
            90f to Color.Blue,
            180f to Color.Green,
            270f to Color.Yellow,
            360f to Color.Red
        )

        val (start, end) = (0 until stops.size - 1)
            .firstOrNull { adjustedAngle >= stops[it].first && adjustedAngle <= stops[it + 1].first }
            ?.let { stops[it] to stops[it + 1] }
            ?: (stops.last() to stops.first())

        val (startAngle, startColor) = start
        val (endAngle, endColor) = end

        val range = endAngle - startAngle
        val fraction = if (range == 0f) 0f else (adjustedAngle - startAngle) / range

        val baseHueColor = lerp(startColor, endColor, fraction)

        return lerp(Color.White, baseHueColor, distance.coerceIn(0f, 1f))
    }
}