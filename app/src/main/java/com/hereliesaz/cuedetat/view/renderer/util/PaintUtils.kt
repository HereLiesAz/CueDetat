// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/PaintUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.PaintCache
import kotlin.math.abs

/**
 * Draws a soft glow halo around a ball ring of [ringRadius] at ([cx], [cy]),
 * using a hardware-accelerated [RadialGradient] instead of a
 * [BlurMaskFilter].
 *
 * Why: the overlay is drawn into Compose's hardware-accelerated canvas. A
 * `BlurMaskFilter` is not supported in hardware, so each blurred circle forces
 * a software-rendered intermediate whose cost scales with the drawn circle's
 * AREA. As the user zooms in, on-screen balls grow and the per-frame blur cost
 * grows quadratically — the source of the "laggier the closer the balls" jank
 * in dynamic basic mode. A RadialGradient stays on the GPU and is effectively
 * free at any zoom.
 *
 * The halo peaks at the ring and fades to transparent both inward and outward,
 * with the spread scaled to the ring so the glow looks consistent at every
 * zoom level (a fixed-pixel blur, by contrast, vanished when zoomed in).
 */
fun drawGlowCircle(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    ringRadius: Float,
    baseGlowColor: Color,
    state: CueDetatState,
    paints: PaintCache,
) {
    if (ringRadius <= 0f) return

    val glowValue = state.glowStickValue
    val colorInt: Int
    val peakAlpha: Int
    if (abs(glowValue) > 0.05f) {
        // Glow Stick override: positive = white, negative = black.
        peakAlpha = (abs(glowValue) * 255).toInt().coerceIn(0, 255)
        colorInt = if (glowValue > 0) AndroidColor.WHITE else AndroidColor.BLACK
    } else {
        colorInt = baseGlowColor.toArgb()
        peakAlpha = (baseGlowColor.alpha * 255 * 0.7f).toInt().coerceIn(0, 255)
    }
    if (peakAlpha == 0) return

    // Spread scales with the ring so the glow is proportional at any zoom.
    val spread = (ringRadius * 0.35f).coerceAtLeast(6f)
    val outer = ringRadius + spread

    val peak = (colorInt and 0x00FFFFFF) or (peakAlpha shl 24)
    val transparent = colorInt and 0x00FFFFFF // alpha 0

    val innerStop = ((ringRadius - spread) / outer).coerceIn(0f, 0.98f)
    val ringStop = (ringRadius / outer).coerceIn(innerStop + 0.001f, 0.999f)

    val shader = RadialGradient(
        cx, cy, outer,
        intArrayOf(transparent, peak, transparent),
        floatArrayOf(innerStop, ringStop, 1f),
        Shader.TileMode.CLAMP,
    )
    val paint = paints.glowHaloPaint.apply {
        this.shader = shader
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, outer, paint)
    paint.shader = null
}

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
    paints: com.hereliesaz.cuedetat.view.PaintCache,
    blurType: android.graphics.BlurMaskFilter.Blur = android.graphics.BlurMaskFilter.Blur.NORMAL
): Paint {
    // Determine a cache key based on the glow stick state.
    // If glow stick is active, key depends on its value.
    // If inactive, key depends on the base color and width.
    val glowValue = state.glowStickValue
    val key = if (abs(glowValue) > 0.05f) {
        "glow_${glowValue}_${blurType.name}"
    } else {
        "glow_${baseGlowColor}_${baseGlowWidth}_${blurType.name}"
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
            paint.maskFilter = BlurMaskFilter(blurRadius, blurType)
        } else {
            // Default behavior: Use the provided base color.
            paint.color = baseGlowColor.toArgb()
            // Default glow is 70% opacity of the base color.
            paint.alpha = (baseGlowColor.alpha * 255 * 0.7f).toInt()
            // Fixed blur radius for standard UI elements.
            paint.maskFilter = BlurMaskFilter(8f, blurType)
        }
        paint
    }
}
