// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/BallTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

class BallTextRenderer {

    private val baseFontSize = 30f // Reduced to better fit longer labels
    private val minFontSize = 16f
    private val maxFontSize = 60f

    fun draw(
        canvas: Canvas,
        paint: Paint,
        zoomSliderPosition: Float,
        ball: LogicalCircular,
        text: String,
        state: OverlayState
    ) {
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM
        val currentTextSize = (baseFontSize * zoomFactor).coerceIn(minFontSize, maxFontSize)
        paint.textSize = currentTextSize

        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state, state.pitchMatrix)
        val screenPos = DrawingUtils.mapPoint(ball.center, state.pitchMatrix)

        val textMetrics = paint.fontMetrics
        val textPadding = 5f * zoomFactor.coerceAtLeast(0.5f)
        val visualTop = screenPos.y - radiusInfo.lift - radiusInfo.radius
        val baseline = visualTop - textPadding - textMetrics.descent
        canvas.drawText(text, screenPos.x, baseline, paint)
    }
}