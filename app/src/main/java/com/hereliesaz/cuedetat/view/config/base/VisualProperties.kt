// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/config/base/VisualProperties.kt
package com.hereliesaz.cuedetat.view.config.base

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import kotlin.math.abs

enum class CenterShape {
    NONE, DOT, CROSSHAIR
}

interface VisualProperties {
    val label: String
    val opacity: Float
    val glowWidth: Float
    val glowColor: Color
    val strokeWidth: Float
    val strokeColor: Color
    val additionalOffset: Float

    fun getGlowPaint(glowValue: Float): Paint? {
        if (glowWidth <= 0f) return null
        val finalGlowValue = glowValue.coerceIn(-1f, 1f)
        if (finalGlowValue == 0f) return null

        val blurRadius = glowWidth * abs(finalGlowValue)
        val blurFilter = if (blurRadius > 0.1f) BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL) else null
        val glowPaintColor = if (finalGlowValue > 0) Color.White else Color.Black

        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = if (this@VisualProperties is LinesConfig) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
            strokeWidth = this@VisualProperties.glowWidth
            color = glowPaintColor.toArgb()
            alpha = (this@VisualProperties.glowColor.alpha * abs(finalGlowValue) * 255).toInt()
            maskFilter = blurFilter
        }
    }

    private fun Color.adjustLuminance(factor: Float): Color {
        if (factor == 0f || this == Color.Transparent) return this
        val hsl = FloatArray(3)
        return try {
            ColorUtils.colorToHSL(this.toArgb(), hsl)
            hsl[2] = (hsl[2] + factor).coerceIn(0f, 1f)
            Color(ColorUtils.HSLToColor(hsl))
        } catch (e: IllegalArgumentException) {
            this
        }
    }

    fun getBasePaint(luminanceAdjustment: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = this@VisualProperties.strokeWidth
        color = this@VisualProperties.strokeColor.adjustLuminance(luminanceAdjustment).toArgb()
        alpha = (this@VisualProperties.opacity * 255).toInt()
    }
}

interface BallsConfig : VisualProperties {
    val centerShape: CenterShape
    val centerSize: Float
    val centerColor: Color
    val fillColor: Color
    val additionalOffset3d: Float

    fun getFillPaint(luminanceAdjustment: Float): Paint = getBasePaint(luminanceAdjustment).apply {
        style = Paint.Style.FILL
        color = this@BallsConfig.fillColor.toArgb()
    }

    fun getCenterPaint(luminanceAdjustment: Float): Paint = getBasePaint(luminanceAdjustment).apply {
        style = Paint.Style.FILL
        color = this@BallsConfig.centerColor.toArgb()
    }

    fun getStrokePaint(luminanceAdjustment: Float, isWarning: Boolean): Paint = getBasePaint(luminanceAdjustment).apply {
        style = Paint.Style.STROKE
        if (isWarning) {
            color = WarningRed.toArgb()
        }
    }
}

interface LinesConfig : VisualProperties {
    fun getLinePaint(luminanceAdjustment: Float, isWarning: Boolean): Paint = getBasePaint(luminanceAdjustment).apply {
        style = Paint.Style.STROKE
        if (isWarning) {
            color = WarningRed.toArgb()
        }
    }

    fun getDottedPaint(luminanceAdjustment: Float): Paint = getLinePaint(luminanceAdjustment, isWarning = false).apply {
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
        alpha = (this@LinesConfig.opacity * 0.6f * 255).toInt()
    }
}


interface TableComponentConfig : VisualProperties {
    val fillColor: Color

    fun getFillPaint(luminanceAdjustment: Float): Paint = getBasePaint(luminanceAdjustment).apply {
        style = Paint.Style.FILL
        color = this@TableComponentConfig.fillColor.toArgb()
        alpha = (this@TableComponentConfig.opacity * 255).toInt()
    }

    fun getStrokePaint(luminanceAdjustment: Float): Paint = getBasePaint(luminanceAdjustment).apply {
        style = Paint.Style.STROKE
    }
}