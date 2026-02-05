// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/PaintUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlin.math.abs

/**
 * Creates and configures a [Paint] object for glow effects.
 *
 * This utility centralizes the logic for creating glows, including the
 * global "Glow Stick" override effect (which forces glows to white/black based on user input).
 * It ensures a new, correctly configured Paint object is created and cached for each draw context,
 * preventing shared state issues.
 *
 * @param baseGlowColor The default color of the glow when the Glow Stick is not active.
 * @param baseGlowWidth The stroke width for the glow.
 * @param state The current CueDetatState, used to check for the Glow Stick value.
 * @param paints The [com.hereliesaz.cuedetat.view.PaintCache] to reuse paint objects from.
 * @return A configured Paint object ready for drawing a glow.
 */
fun createGlowPaint(
    baseGlowColor: Color,
    baseGlowWidth: Float,
    state: CueDetatState,
    paints: com.hereliesaz.cuedetat.view.PaintCache
): Paint {
    // Determine a cache key based on the glow stick state.
    // If glow stick is active, key depends on its value.
    // If inactive, key depends on the base color and width.
    val glowValue = state.glowStickValue
    val key = if (abs(glowValue) > 0.05f) {
        "glow_${glowValue}"
    } else {
        "glow_${baseGlowColor}_${baseGlowWidth}"
    }

    // Retrieve from cache or create new.
    return paints.glowPaints.getOrPut(key) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = baseGlowWidth
        }

        // Check if Glow Stick override is active.
        if (abs(glowValue) > 0.05f) {
            // Override active: Glow color becomes white (positive) or black (negative).
            val glowAlpha = (abs(glowValue) * 255).toInt()
            val color = if (glowValue > 0) Color.White.toArgb() else Color.Black.toArgb()
            // Blur radius increases with intensity.
            val blurRadius = 15f * abs(glowValue)

            paint.color = color
            paint.alpha = glowAlpha
            paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            // Default behavior: Use the provided base color.
            paint.color = baseGlowColor.toArgb()
            // Default glow is 70% opacity of the base color.
            paint.alpha = (baseGlowColor.alpha * 255 * 0.7f).toInt()
            // Fixed blur radius for standard UI elements.
            paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        paint
    }
}
