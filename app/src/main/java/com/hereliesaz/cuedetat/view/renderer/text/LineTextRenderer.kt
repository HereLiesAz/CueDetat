package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.ZoomMapping
import kotlin.math.cos
import kotlin.math.sin

class LineTextRenderer {

    private val minLineTextSize = 18f
    private val maxLineTextSize = 70f

    fun draw(
        canvas: Canvas,
        text: String,
        origin: PointF,
        lineAngleDegrees: Float,
        distanceFromOrigin: Float,
        angleOffsetDegrees: Float,
        rotationOffsetDegrees: Float,
        paint: Paint,
        baseFontSize: Float,
        zoomSliderPosition: Float
    ) {
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM
        val currentTextSize = (baseFontSize * zoomFactor).coerceIn(minLineTextSize, maxLineTextSize)
        paint.textSize = currentTextSize

        val textAngleRadians = Math.toRadians((lineAngleDegrees + angleOffsetDegrees).toDouble())

        val textX = origin.x + (distanceFromOrigin * cos(textAngleRadians)).toFloat()
        val textY = origin.y + (distanceFromOrigin * sin(textAngleRadians)).toFloat()

        canvas.save()
        canvas.rotate(lineAngleDegrees + rotationOffsetDegrees, textX, textY)
        canvas.drawText(text, textX, textY, paint)
        canvas.restore()
    }
}