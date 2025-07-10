package com.hereliesaz.cuedetat.view

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hereliesaz.cuedetat.ui.theme.*
import com.hereliesaz.cuedetat.view.state.OverlayState

class PaintCache {
    private val strokeWidth = 6f // Doubled as commanded
    private val glowStrokeWidth = 12f // Returned to original holy value
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
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val angleGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f } // Doubled
    val pocketFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.Black.toArgb() }
    val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f) } // Doubled


    // --- Bank Line Paints ---
    val bankLine1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val bankLine2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val bankLine3Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val bankLine4Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }

    // --- Glow Paint Objects ---
    val lineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL_AND_STROKE; strokeWidth = glowStrokeWidth }
    val ballGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = glowStrokeWidth }


    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(uiState: OverlayState, isDark: Boolean) {
        val LUMINANCE_ADJUST = uiState.luminanceAdjustment
        val baseScheme = uiState.appControlColorScheme
        val glowColor = (if(isDark) Color.White else Color.Black).copy(alpha = 0.5f).toArgb()

        val blurFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        lineGlowPaint.color = glowColor
        lineGlowPaint.maskFilter = blurFilter
        ballGlowPaint.color = glowColor
        ballGlowPaint.maskFilter = blurFilter

        tableOutlinePaint.color = AcidPatina.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        targetCirclePaint.color = baseScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCirclePaint.color = baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        actualCueBallPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        fillPaint.color = baseScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        shotLinePaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tangentLineSolidPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tangentLineDottedPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.6f).toArgb()

        bankLine1Paint.color = BankLine1Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankLine2Paint.color = BankLine2Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankLine3Paint.color = BankLine3Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankLine4Paint.color = BankLine4Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()

        textPaint.color = baseScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        warningPaint.color = baseScheme.error.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        angleGuidePaint.color = baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.4f).toArgb()
        gridLinePaint.color = baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.3f).toArgb()
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