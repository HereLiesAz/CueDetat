// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/BallTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.config.ui.LabelProperties
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils

class BallTextRenderer {

    private val baseFontSize = 30f
    private val minFontSize = 16f
    private val maxFontSize = 60f

    fun draw(
        canvas: Canvas,
        paint: Paint,
        ball: LogicalCircular,
        text: String,
        config: LabelProperties,
        state: CueDetatState
    ) {
        if (!state.areHelpersVisible && !config.isPersistentlyVisible) return
        val matrix = state.pitchMatrix ?: return

        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(
            state.zoomSliderPosition,
            minZoom,
            maxZoom
        ) / ZoomMapping.DEFAULT_ZOOM
        val currentTextSize = (baseFontSize * zoomFactor).coerceIn(minFontSize, maxFontSize)
        paint.textSize = currentTextSize
        paint.color = config.color.copy(alpha = config.opacity).toArgb()

        val radiusInfo =
            DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state, matrix)
        val screenPos = DrawingUtils.mapPoint(ball.center, matrix)

        val textMetrics = paint.fontMetrics
        val textPadding = 5f * zoomFactor.coerceAtLeast(0.5f)
        val visualTop = screenPos.y - radiusInfo.lift - radiusInfo.radius
        val baseline = visualTop - textPadding - textMetrics.descent + config.yOffset

        canvas.drawText(text, screenPos.x + config.xOffset, baseline, paint)
    }
}