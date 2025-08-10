// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/PaintUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlin.math.abs

/**
 * Creates and configures a Paint object for glow effects.
 *
 * This utility centralizes the logic for creating glows, including the
 * global "Glow Stick" override effect. It ensures a new, correctly
 * configured Paint object is used for each draw call, preventing shared
 * state issues.
 *
 * @param baseGlowColor The default color of the glow when the Glow Stick
 *    is not active.
 * @param baseGlowWidth The stroke width for the glow.
 * @param state The current CueDetatState, used to check for the Glow Stick
 *    value.
 * @return A configured Paint object ready for drawing a glow.
 */
fun createGlowPaint(
    baseGlowColor: Color,
    baseGlowWidth: Float,
    state: CueDetatState,
    paints: com.hereliesaz.cuedetat.view.PaintCache
): Paint {
    val glowValue = state.glowStickValue
    val key = if (abs(glowValue) > 0.05f) {
        "glow_${glowValue}"
    } else {
        "glow_${baseGlowColor}_${baseGlowWidth}"
    }

    return paints.glowPaints.getOrPut(key) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = baseGlowWidth
        }

        if (abs(glowValue) > 0.05f) {
            // Glow Stick override is active
            val glowAlpha = (abs(glowValue) * 255).toInt()
            val color = if (glowValue > 0) Color.White.toArgb() else Color.Black.toArgb()
            val blurRadius = 15f * abs(glowValue)
            paint.color = color
            paint.alpha = glowAlpha
            paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            // Default glow effect
            paint.color = baseGlowColor.toArgb()
            paint.alpha = (baseGlowColor.alpha * 255 * 0.7f).toInt() // Default glow is 70% alpha
            paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        paint
    }
}