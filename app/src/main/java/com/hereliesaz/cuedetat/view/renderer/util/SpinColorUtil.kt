// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/SpinColorUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.ColorUtils

object SpinColorUtils {

    /**
     * The single source of truth for converting a spin angle and distance into a color.
     * This logic must be used by both the UI (SpinControl) and the domain (CalculateSpinPaths).
     * @param angleDegrees The angle of the spin, where 0 is to the right (3 o'clock).
     * @param distance The normalized distance from the center (0.0) to the edge (1.0).
     * @return The calculated Color.
     */
    fun getColorFromAngleAndDistance(angleDegrees: Float, distance: Float): Color {
        // Normalize angle to be 0-360, with 0 at the right (3 o'clock).
        val normalizedAngle = (angleDegrees + 360f) % 360f

        // --- THE RIGHTEOUS FIX ---
        // The color stops are now defined by the cardinal directions commanded.
        // Red: 0/360, Yellow: 270, Green: 180, Blue: 90
        val stops = listOf(
            0f to Color.Red,
            90f to Color.Blue,
            180f to Color.Green,
            270f to Color.Yellow,
            360f to Color.Red
        )

        // Find which two color stops the angle falls between.
        val (start, end) = (0 until stops.size - 1)
            .firstOrNull { normalizedAngle >= stops[it].first && normalizedAngle <= stops[it + 1].first }
            ?.let { stops[it] to stops[it + 1] }
            ?: (stops.last() to stops.first()) // Fallback for edge cases

        val (startAngle, startColor) = start
        val (endAngle, endColor) = end

        val range = endAngle - startAngle
        val fraction = if (range == 0f) 0f else (normalizedAngle - startAngle) / range

        // Get the pure hue by interpolating.
        val baseHueColor = lerp(startColor, endColor, fraction)

        // Now, use the distance to interpolate between white (center) and the pure hue (edge).
        return lerp(Color.White, baseHueColor, distance.coerceIn(0f, 1f))
    }
}