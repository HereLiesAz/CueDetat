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
        if (!appState.isInitialized || !appState.areHelperTextsVisible || ghostRadius <= 0.01f) return

        val paint = appPaints.ghostCueNamePaint // textAlign is CENTER
        paint.textSize = getScreenSpaceTextSize(
            config.GHOST_BALL_NAME_BASE_SIZE,
            appState.zoomFactor,
            config
        )

        val labelOffsetPixels = (LABEL_OFFSET_FROM_BALL_TOP_DP / appState.zoomFactor.coerceAtLeast(0.2f))
            .coerceAtLeast(LABEL_MIN_OFFSET_PIXELS)

        val fm = paint.fontMetrics
        val topOfBallY = ghostCenter.y - ghostRadius

        // Calculate 'preferredY' so it's the baseline of the text,
        // placing the bottom of the text (fm.descent below baseline)
        // 'labelOffsetPixels' above the top of the ball.
        // So, baselineY + fm.descent = topOfBallY - labelOffsetPixels
        // baselineY = topOfBallY - labelOffsetPixels - fm.descent
        var preferredY_baseline = topOfBallY - labelOffsetPixels - fm.descent

        // Apply the vertical offset adjustment calculated by ScreenRenderer
        // A negative adjustment from ScreenRenderer pushes it further up.
        preferredY_baseline += verticalOffsetAdjustment

        val preferredX = ghostCenter.x // For Align.CENTER, X is the center

        // TextLayoutHelper.layoutAndDrawText will use this preferredY.
        // If paint.textAlign is CENTER, TextLayoutHelper's drawTextAtPosition
        // for non-rotated text with a single line, might use Y as the vertical center.
        // Let's adjust preferredY to be the center of the text block for TextLayoutHelper.
        val textBlockHeight = fm.descent - fm.ascent
        preferredY_baseline - fm.ascent - (textBlockHeight / 2f) + fm.ascent // This simplifies to y_baseline - text_height/2 - ascent
        // simplified: y_baseline + (fm.ascent - fm.descent)/2
        // Y for text center = baselineY + ascent + (height/2)
        // Y for text center = baselineY - (height/2) + descent (if y is baseline)
        // If preferredY_baseline is the baseline:
        // The center of the text is roughly baselineY - (textBlockHeight / 2) + (textBlockHeight - descent)
        // No, if paint.textAlign is CENTER, TextLayoutHelper's drawTextAtPosition (for single line, no rotation)
        // treats Y as the vertical center.
        // So we need to calculate the desired vertical center of the text block.
        // The text block's bottom is at topOfBallY - labelOffsetPixels + verticalOffsetAdjustment (if using actual bottom)
        // The text block's top is bottom - textBlockHeight
        // The text block's center is top + textBlockHeight / 2
        val textVisualBottom = topOfBallY - labelOffsetPixels + verticalOffsetAdjustment // Y where the descent of text ends
        val textVisualTop = textVisualBottom - textBlockHeight
        val preferredY_center = textVisualTop + textBlockHeight / 2f


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