// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/PaintCache.kt

package com.hereliesaz.cuedetat.view

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hereliesaz.cuedetat.ui.theme.BankLine1Yellow
import com.hereliesaz.cuedetat.ui.theme.BankLine2Yellow
import com.hereliesaz.cuedetat.ui.theme.BankLine3Yellow
import com.hereliesaz.cuedetat.ui.theme.BankLine4Yellow
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.config.table.Table
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs

class PaintCache {
    private val strokeWidth = 6f
    private val glowStrokeWidth = 12f
    private val glowRadius = 15f

    // --- Primary Paint Objects ---
    val tableOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val targetCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val cueCirclePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val actualCueBallPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val shotLinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val tangentLineSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokeWidth * 0.8f
    }
    val tangentLineDottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokeWidth * 0.8f; pathEffect =
        DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    val warningPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val angleGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f
    } // Doubled
    val pocketFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.Black.toArgb()
    }
    val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f; pathEffect =
        DashPathEffect(floatArrayOf(15f, 15f), 0f)
    } // Doubled
    val pathObstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val cvResultPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val gradientMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }


    // --- Bank Line Paints ---
    val bankLine1Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val bankLine2Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val bankLine3Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }
    val bankLine4Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = strokeWidth }

    // --- Glow Paint Objects ---
    val lineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = glowStrokeWidth
    }
    val ballGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = glowStrokeWidth
    }


    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(uiState: OverlayState, isDark: Boolean) {
        val LUMINANCE_ADJUST = uiState.luminanceAdjustment
        val baseScheme = uiState.appControlColorScheme ?: darkColorScheme()

        // Handle Glow Stick
        val glowValue = uiState.glowStickValue
        val glowAlpha = (abs(glowValue) * 255).toInt()
        val glowColor = if (glowValue > 0) Color.White.toArgb() else Color.Black.toArgb()

        // --- HERESY CORRECTED: A default glow is now the law ---
        val defaultBlurRadius = 8f
        val blurRadius =
            if (abs(glowValue) > 0.05f) glowRadius * abs(glowValue) else defaultBlurRadius
        val blurFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        // --- END CORRECTION ---

        lineGlowPaint.apply {
            this.color = glowColor
            this.alpha = glowAlpha
            this.maskFilter = blurFilter
        }
        ballGlowPaint.apply {
            this.color = glowColor
            this.alpha = glowAlpha
            this.maskFilter = blurFilter
        }


        // --- HERESY CORRECTED: The cache now reads all properties from the component configs. ---
        val tableConfig = Table()
        tableOutlinePaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tableOutlinePaint.strokeWidth = tableConfig.strokeWidth
        // --- END CORRECTION ---

        targetCirclePaint.color = baseScheme.primary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        cueCirclePaint.color = baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        actualCueBallPaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        fillPaint.color = baseScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        shotLinePaint.color = baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tangentLineSolidPaint.color =
            baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        tangentLineDottedPaint.color =
            baseScheme.secondary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.6f).toArgb()
        pathObstructionPaint.color =
            baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.2f).toArgb()
        cvResultPaint.color =
            Color.Blue.copy(alpha = 0.5f).adjustLuminance(LUMINANCE_ADJUST).toArgb()


        bankLine1Paint.color = BankLine1Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankLine2Paint.color = BankLine2Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankLine3Paint.color = BankLine3Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        bankLine4Paint.color = BankLine4Yellow.adjustLuminance(LUMINANCE_ADJUST).toArgb()

        textPaint.color = baseScheme.onSurface.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        warningPaint.color = WarningRed.adjustLuminance(LUMINANCE_ADJUST).toArgb()
        angleGuidePaint.color =
            baseScheme.tertiary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.4f).toArgb()
        gridLinePaint.color =
            baseScheme.primary.adjustLuminance(LUMINANCE_ADJUST).copy(alpha = 0.5f).toArgb()
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

