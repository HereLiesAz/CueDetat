// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/SpinColorUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Utility object for calculating colors representing Spin (English).
 */
object SpinColorUtils {

    /**
     * The single source of truth for converting a spin angle and distance into a color.
     *
     * This logic maps the 2D spin vector (angle and magnitude) to a color on a gradient wheel.
     * This ensures the Spin Control UI and the projected Spin Path always match in color.
     *
     * @param angleDegrees The angle of the spin in degrees, where 0 is to the right (3 o'clock).
     * @param distance The normalized distance from the center (0.0 = center, 1.0 = edge).
     * @return The calculated [Color].
     */
    fun getColorFromAngleAndDistance(angleDegrees: Float, distance: Float): Color {
        // Normalize the angle to be within [0, 360).
        val adjustedAngle = (angleDegrees + 360f) % 360f

        // Define color stops around the wheel.
        // 0/360: Red, 90: Blue, 180: Green, 270: Yellow.
        val stops = listOf(
            0f to Color.Red,
            90f to Color.Blue,
            180f to Color.Green,
            270f to Color.Yellow,
            360f to Color.Red // Wrap around to Red.
        )

        // Find the two stops surrounding the current angle.
        val (start, end) = (0 until stops.size - 1)
            .firstOrNull { adjustedAngle >= stops[it].first && adjustedAngle <= stops[it + 1].first }
            ?.let { stops[it] to stops[it + 1] }
            ?: (stops.last() to stops.first()) // Fallback (shouldn't happen with correct logic).

        val (startAngle, startColor) = start
        val (endAngle, endColor) = end

        // Calculate interpolation fraction between the two angular stops.
        val range = endAngle - startAngle
        val fraction = if (range == 0f) 0f else (adjustedAngle - startAngle) / range

        // Interpolate the base hue color.
        val baseHueColor = lerp(startColor, endColor, fraction)

        // Interpolate between White (center) and the Hue (edge) based on distance/intensity.
        // Center (no spin) is always white. Edge (max spin) is fully saturated.
        return lerp(Color.White, baseHueColor, distance.coerceIn(0f, 1f))
    }
}
