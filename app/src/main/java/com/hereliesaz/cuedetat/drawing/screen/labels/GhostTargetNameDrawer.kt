package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper

class GhostTargetNameDrawer(private val textLayoutHelper: TextLayoutHelper) {

    private val TEXT_STRING = "Target Ball"
    private val LABEL_OFFSET_FROM_BALL_TOP_DP = 2f
    private val LABEL_MIN_OFFSET_PIXELS = 1f

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        ghostCenter: PointF,
        ghostRadius: Float,
        verticalOffsetAdjustment: Float = 0f
    ) {
        // Only draw if initialized and helper texts are visible (removed AIMING mode check)
        if (!appState.isInitialized || !appState.areHelperTextsVisible) return
        if (ghostRadius <= 0.01f) return

        val paint = appPaints.ghostTargetNamePaint // textAlign is CENTER
        paint.textSize = getScreenSpaceTextSize(
            config.GHOST_BALL_NAME_BASE_SIZE,
            appState.zoomFactor,
            config
        )

        val labelOffsetPixels = (LABEL_OFFSET_FROM_BALL_TOP_DP / appState.zoomFactor.coerceAtLeast(0.2f))
            .coerceAtLeast(LABEL_MIN_OFFSET_PIXELS)

        val fm = paint.fontMetrics
        val textBlockHeight = fm.descent - fm.ascent

        val topOfBallY = ghostCenter.y - ghostRadius

        // Calculate Y for the center of the text block.
        // The text block's bottom should be 'labelOffsetPixels' above the top of the ball,
        // then add the verticalOffsetAdjustment.
        val textVisualBottom = topOfBallY - labelOffsetPixels + verticalOffsetAdjustment
        val textVisualTop = textVisualBottom - textBlockHeight
        val preferredY_center = textVisualTop + textBlockHeight / 2f

        val preferredX = ghostCenter.x

        textLayoutHelper.layoutAndDrawText(
            canvas, TEXT_STRING, preferredX, preferredY_center, paint, 0f, ghostCenter
        )
    }

    private fun getScreenSpaceTextSize(
        baseSize: Float, zoomFactor: Float, config: AppConfig, sizeMultiplier: Float = 1f
    ): Float {
        val effectiveZoom = zoomFactor.coerceIn(0.7f, 1.3f)
        return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.TEXT_MIN_SCALE_FACTOR, config.TEXT_MAX_SCALE_FACTOR))
            .coerceAtLeast(baseSize * sizeMultiplier * config.TEXT_MIN_SCALE_FACTOR * 0.8f)
    }
}