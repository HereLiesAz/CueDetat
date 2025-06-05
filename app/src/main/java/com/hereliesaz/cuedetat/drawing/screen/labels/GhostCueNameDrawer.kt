package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper

class GhostCueNameDrawer(private val textLayoutHelper: TextLayoutHelper) {

    private val TEXT_STRING = "Ghost Ball"
    // This offset is from the TOP EDGE of the ball to the very BOTTOM of the text characters (descent).
    private val LABEL_OFFSET_FROM_BALL_TOP_DP = 0.5f // Extremely small offset
    private val LABEL_MIN_OFFSET_PIXELS = 0f       // Allow touching

    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        ghostCenter: PointF,
        ghostRadius: Float,
        verticalOffsetAdjustment: Float = 0f // This will be ADDED to the calculated Y. Negative pushes up.
    ) {
        // Only draw if in AIMING mode and helper texts are visible
        if (!appState.isInitialized || !appState.areHelperTextsVisible || appState.currentMode != AppState.SelectionMode.AIMING) return
        if (ghostRadius <= 0.01f) return

        val paint = appPaints.ghostCueNamePaint // textAlign is CENTER
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

        // The text block's bottom should be 'labelOffsetPixels' above the top of the ball.
        // Then apply the verticalOffsetAdjustment.
        // The text block's bottom is at topOfBallY - labelOffsetPixels + verticalOffsetAdjustment
        val textVisualBottom = topOfBallY - labelOffsetPixels + verticalOffsetAdjustment
        val textVisualTop = textVisualBottom - textBlockHeight
        val preferredY_center = textVisualTop + textBlockHeight / 2f // Y for the center of the text block

        val preferredX = ghostCenter.x // For Align.CENTER, X is the center

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