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
 *
 * Scope of the replacement: only ball halos moved off BlurMaskFilter. Line and
 * rail glows still use BlurMaskFilter via [createGlowPaint] (cached, so the
 * native filter is not reallocated per frame).
 *
 * Allocation: the gradient is built once per (colour, alpha, stops) in a unit
 * circle at the origin and cached in [PaintCache.haloShaders]; per draw only its
 * local matrix is updated (translate to the ball, scale to the outer radius).
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

    // Quantize stops to 1/256 so the cache key space stays small.
    val qInner = (innerStop * 256f).toInt()
    val qRing = (ringStop * 256f).toInt().coerceAtLeast(qInner + 1)
    val key = (peak.toLong() shl 32) or ((qInner.toLong() and 0xFFFF) shl 16) or (qRing.toLong() and 0xFFFF)
    val shader = paints.haloShaders.getOrPut(key) {
        RadialGradient(
            0f, 0f, 1f,
            intArrayOf(transparent, peak, transparent),
            floatArrayOf(qInner / 256f, qRing / 256f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    paints.haloMatrix.reset()
    paints.haloMatrix.setScale(outer, outer)
    paints.haloMatrix.postTranslate(cx, cy)
    shader.setLocalMatrix(paints.haloMatrix)
    val paint = paints.glowHaloPaint.apply {
        this.shader = shader
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(cx, cy, outer, paint)
    paint.shader = null
}

/**
 * Returns a cached [Paint] for a blurred line/rail glow, honouring the global "Glow Stick"
 * override (white for positive, black for negative values).
 *
 * Paints are cached in [PaintCache.glowPaints] (LRU) under a reusable [GlowKey] probe, so the
 * steady-state lookup allocates nothing: no String key, no new BlurMaskFilter. The glow-stick
 * value is quantized to 1/100 so a moving slider cannot create unbounded entries.
 * Callers must treat the returned Paint as read-only (copy via `Paint.set` before mutating).
 *
 * @param baseGlowColor The default color of the glow when the Glow Stick is not active.
 * @param baseGlowWidth The stroke width for the glow.
 * @param state The current CueDetatState, used to check for the Glow Stick value.
 * @param paints The [com.hereliesaz.cuedetat.view.PaintCache] holding the cache.
 */
fun createGlowPaint(
    baseGlowColor: Color,
    baseGlowWidth: Float,
    state: CueDetatState,
    paints: com.hereliesaz.cuedetat.view.PaintCache,
    blurType: android.graphics.BlurMaskFilter.Blur = android.graphics.BlurMaskFilter.Blur.NORMAL
): Paint {
    val rawGlow = state.glowStickValue
    val stickActive = abs(rawGlow) > 0.05f
    val glowQ = if (stickActive) kotlin.math.round(rawGlow * 100f).toInt() else 0
    val colorInt = baseGlowColor.toArgb()

    val probe = paints.glowKeyProbe.apply {
        color = colorInt; widthBits = baseGlowWidth.toRawBits(); blur = blurType.ordinal; glow = glowQ
    }
    paints.glowPaints[probe]?.let { return it }

    val glowValue = glowQ / 100f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = baseGlowWidth
    }
    if (stickActive) {
        // Override active: Glow color becomes white (positive) or black (negative).
        paint.color = if (glowValue > 0) AndroidColor.WHITE else AndroidColor.BLACK
        paint.alpha = (abs(glowValue) * 255).toInt().coerceIn(0, 255)
        // Blur radius increases with intensity.
        paint.maskFilter = BlurMaskFilter(15f * abs(glowValue), blurType)
    } else {
        paint.color = colorInt
        // Default glow is 70% opacity of the base color.
        paint.alpha = (baseGlowColor.alpha * 255 * 0.7f).toInt()
        paint.maskFilter = BlurMaskFilter(8f, blurType)
    }
    paints.glowPaints[probe.copy()] = paint
    return paint
}

/** Mutable cache key for [createGlowPaint]; stored entries are immutable copies. */
data class GlowKey(var color: Int = 0, var widthBits: Int = 0, var blur: Int = 0, var glow: Int = 0)
