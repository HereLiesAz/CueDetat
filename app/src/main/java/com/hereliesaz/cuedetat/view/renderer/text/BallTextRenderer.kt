package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.ui.ZoomMapping

class BallTextRenderer {

    private val baseGhostBallTextSize = 42f
    private val minGhostBallTextSize = 20f
    private val maxGhostBallTextSize = 80f

    fun draw(
        canvas: Canvas,
        paint: Paint,
        zoomSliderPosition: Float,
        x: Float,
        y: Float,
        radius: Float,
        text: String
    ) {
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM
        val currentTextSize = (baseGhostBallTextSize * zoomFactor).coerceIn(
            minGhostBallTextSize,
            maxGhostBallTextSize
        )
        paint.textSize = currentTextSize
        val textMetrics = paint.fontMetrics
        val textPadding = 5f * zoomFactor.coerceAtLeast(0.5f)
        val visualTop = y - radius
        val baseline = visualTop - textPadding - textMetrics.descent
        canvas.drawText(text, x, baseline, paint)
    }
}