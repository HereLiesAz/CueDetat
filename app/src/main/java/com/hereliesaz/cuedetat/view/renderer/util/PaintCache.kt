package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.BlurMaskFilter
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.ui.theme.AccentGold
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.ui.theme.Mariner
import com.hereliesaz.cuedetat.ui.theme.BruisedPlum
import com.hereliesaz.cuedetat.ui.theme.MonteCarlo

class PaintCache {
    val targetCirclePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    val warningPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = WarningRed.toArgb()
    }

    val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val actualCueBallPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.White.toArgb()
    }

    val shotLinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val lineGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    val ballGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 15f
    }

    val tableOutlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = AcidPatina.toArgb()
    }

    val pocketFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AndroidColor.BLACK
    }

    val gridLinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = AcidPatina.copy(alpha = 0.3f).toArgb()
    }

    val tangentLineSolidPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val tangentLineDottedPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    val pathObstructionPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = WarningRed.copy(alpha = 0.2f).toArgb()
    }

    val cvResultPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Mariner.copy(alpha = 0.5f).toArgb()
    }

    val angleGuidePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.White.copy(alpha = 0.4f).toArgb()
    }

    val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        color = Color.White.toArgb()
    }

    val bankLine1Paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
    val bankLine2Paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
    val bankLine3Paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
    val bankLine4Paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }

    val gradientMaskPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val glowPaints = mutableMapOf<Pair<Int, Float>, Paint>()

    fun setTypeface(typeface: Typeface?) {
        textPaint.typeface = typeface
    }

    fun updateColors(state: CueDetatState, systemIsDark: Boolean) {
        val baseColor = if (systemIsDark) Color.White else Color.Black
        targetCirclePaint.color = SulfurDust.toArgb()
        shotLinePaint.color = Mariner.toArgb()
        
        val glowRadius = if (state.glowStickValue > 0) state.glowStickValue * 20f else 10f
        lineGlowPaint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        ballGlowPaint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        
        textPaint.color = baseColor.toArgb()
    }
}
