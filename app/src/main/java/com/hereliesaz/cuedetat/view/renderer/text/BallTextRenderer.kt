// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/text/BallTextRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.text

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
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
        state: CueDetatState,
        drawBelow: Boolean = false
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

        paint.textSize = (baseFontSize * zoomFactor).coerceIn(minFontSize, maxFontSize)
        paint.textAlign = Paint.Align.CENTER

        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked
        if (isBeginnerLocked) {
            paint.color = android.graphics.Color.parseColor("#00E5FF")
            paint.setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
        } else {
            paint.color = config.color.copy(alpha = config.opacity).toArgb()
            paint.clearShadowLayer()
        }

        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state, matrix)
        val screenPos = DrawingUtils.mapPoint(ball.center, matrix)

        val textMetrics = paint.fontMetrics
        val textPadding = 10f * zoomFactor.coerceAtLeast(0.5f)
        val lineHeight = textMetrics.descent - textMetrics.ascent

        var currentBaseline = if (drawBelow) {
            val visualBottom = screenPos.y - radiusInfo.lift + radiusInfo.radius
            visualBottom + textPadding - textMetrics.ascent + config.yOffset
        } else {
            val visualTop = screenPos.y - radiusInfo.lift - radiusInfo.radius
            visualTop - textPadding - textMetrics.descent + config.yOffset
        }

        val lines = text.split("\n")

        // Clamp X firmly into the screen so no text is ever rendered off the edges
        val safeX = (screenPos.x + config.xOffset).coerceIn(
            150f * state.screenDensity,
            state.viewWidth - (150f * state.screenDensity)
        )

        for (line in lines) {
            canvas.drawText(line, safeX, currentBaseline, paint)
            currentBaseline += lineHeight
        }
    }
}