package com.hereliesaz.cuedetat.view

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hereliesaz.cuedetat.ui.theme.*
import com.hereliesaz.cuedetat.view.state.OverlayState

class PaintCache {
    private val strokeWidth = 3f
    private val glowStrokeWidth = 12f
    private val glowRadius = 15f

    // --- Primary Paint Objects ---
    val tableOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val targetCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val cueCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val actualCueBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val shotLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth * 0.8f }
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth * 0.8f; pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f) }
    val bankLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }

    // --- Glow Paint Objects ---
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = glowStrokeWidth }

    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(uiState: OverlayState, isDark: Boolean) {
        val LUMINANCE_ADJUST = uiState.luminanceAdjustment
        val baseScheme = if (isDark) darkColorScheme() else lightColorScheme()
        val glowColor = (if(isDark) Color.White else Color.Black).copy(alpha = 0.5f).toArgb()

        val blurFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = glowColor
        glowPaint.maskFilter = blurFilter

        tableOutlinePaint.color = baseScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        targetCirclePaint.color = baseScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCirclePaint.color = baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        actualCueBallPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        fillPaint.color = baseScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        shotLinePaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tangentLineSolidPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tangentLineDottedPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.6f).toArgb()
        bankLinePaint.color = RebelYellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        textPaint.color = baseScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        warningPaint.color = baseScheme.error.adjustLuminance(LUMINANCE_ADJUST).toArgb()
    }

    private fun Color.adjustLuminance(factor: Float): Color {
        if (factor == 0f || this == Color.Transparent) return this
        val hsl = FloatArray(3)
        try {
            ColorUtils.colorToHSL(this.toArgb(), hsl)
            hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f)
            return Color(ColorUtils.HSLToColor(hsl))
        } catch (e: IllegalArgumentException) {
            return this
        }
    }
}