package com.hereliesaz.cuedetat.view

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.ui.theme.HippieGreen
import com.hereliesaz.cuedetat.ui.theme.BankLine1Yellow
import com.hereliesaz.cuedetat.ui.theme.BruisedPlum

class PaintCache {
    val targetCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = SulfurDust.toArgb()
        strokeWidth = 5f
    }

    val warningPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = WarningRed.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    val fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.toArgb()
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    val shotLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Mariner.toArgb()
        strokeWidth = 5f
    }

    val pathObstructionPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = WarningRed.toArgb()
        strokeWidth = 5f
    }

    val tangentLineSolidPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.White.toArgb()
        strokeWidth = 3f
    }

    val tangentLineDottedPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.White.toArgb()
        strokeWidth = 3f
        // pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) // Add if needed
    }

    val bankLine1Paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = BankLine1Yellow.toArgb()
        strokeWidth = 4f
    }

    val angleGuidePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.White.toArgb()
        strokeWidth = 2f
    }

    val gradientMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    val tableOutlinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = HippieGreen.toArgb()
        strokeWidth = 6f
    }

    val gridLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
         style = Paint.Style.STROKE
         color = HippieGreen.copy(alpha = 0.5f).toArgb()
         strokeWidth = 2f
    }

    val pocketFillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.Black.toArgb()
    }

    val cvResultPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.Magenta.toArgb()
        strokeWidth = 3f
    }

    fun getGlowPaint(color: Color, width: Float): Paint {
        // In a real cache, we'd cache these based on key (color, width).
        // For now, return a new one to pass compile, or implement simple cache if strictly required.
        // "For glow effects, use the getGlowPaint method... Do not create new Paint objects"
        // Let's implement a simple cache.
        val key = color.toArgb() to width
        return glowPaints.getOrPut(key) {
             Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.color = color.toArgb()
                strokeWidth = width
                maskFilter = android.graphics.BlurMaskFilter(width / 2, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
        }
    }

    // Made public/internal to fix access error from PaintUtils
    val glowPaints = mutableMapOf<Pair<Int, Float>, Paint>()

    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(isDarkMode: Boolean) {
        // Implementation for theme switching if needed
    }
}
