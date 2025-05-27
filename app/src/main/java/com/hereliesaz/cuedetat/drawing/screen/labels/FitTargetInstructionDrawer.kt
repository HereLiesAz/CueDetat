package com.hereliesaz.cuedetat.drawing.screen.labels

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.text.TextPaint
import com.hereliesaz.cuedetat.config.AppConfig
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState
import com.hereliesaz.cuedetat.drawing.utility.TextLayoutHelper
import kotlin.math.max

class FitTargetInstructionDrawer(
    private val textLayoutHelper: TextLayoutHelper,
    private val viewWidthProvider: () -> Int
) {
    private val TEXT_LINE_1 = "Fit this to your"
    private val TEXT_LINE_2 = "target ball, IRL."
    private val COMBINED_TEXT by lazy { "$TEXT_LINE_1\n$TEXT_LINE_2" }

    // Constants for this specific drawer's layout needs
    private val FIT_TARGET_TEXT_RIGHT_CLEARANCE_MARGIN_DP = 190f // To clear slider
    private val MIN_FIT_TARGET_FONT_SIZE_SP = 20f // Min font size
    private val FIT_TARGET_TEXT_X_PADDING_FROM_CIRCLE_DP = 2f // Padding from circle


    fun draw(
        canvas: Canvas,
        appState: AppState,
        appPaints: AppPaints,
        config: AppConfig,
        targetGhostCenter: PointF,
        targetGhostRadius: Float
    ) {
        if (!appState.isInitialized || !appState.areHelperTextsVisible || targetGhostRadius <= 0.01f) return

        val paint = TextPaint(appPaints.fitTargetInstructionPaint)
        val baseSize = config.GHOST_BALL_NAME_BASE_SIZE * config.FIT_TARGET_INSTRUCTION_BASE_SIZE_FACTOR
        var currentFontSize = getScreenSpaceTextSize(baseSize, appState.zoomFactor, config)
        paint.textSize = currentFontSize
        // paint.textAlign is LEFT from AppPaints

        val screenWidth = viewWidthProvider()
        val xPos = targetGhostCenter.x + targetGhostRadius +
                (FIT_TARGET_TEXT_X_PADDING_FROM_CIRCLE_DP / appState.zoomFactor.coerceAtLeast(0.5f))
        val maxTextWidth = screenWidth - xPos - FIT_TARGET_TEXT_RIGHT_CLEARANCE_MARGIN_DP

        if (maxTextWidth <= 0) return

        val widthLine1 = paint.measureText(TEXT_LINE_1)
        val widthLine2 = paint.measureText(TEXT_LINE_2)
        val longerPredefinedLineWidth = max(widthLine1, widthLine2)

        if (longerPredefinedLineWidth > maxTextWidth) {
            val scaleFactor = maxTextWidth / longerPredefinedLineWidth
            currentFontSize *= (scaleFactor * 0.98f)
            currentFontSize = currentFontSize.coerceIn(MIN_FIT_TARGET_FONT_SIZE_SP, baseSize * 1.1f)
            paint.textSize = currentFontSize
        }

        val fm = paint.fontMetrics
        val lineHeight = paint.fontSpacing
        val numLines = 2
        val blockHeight = (fm.descent - fm.ascent) * numLines + lineHeight * (numLines - 1)
        val preferredY = targetGhostCenter.y - (blockHeight / 2f) - fm.ascent

        textLayoutHelper.layoutAndDrawText(
            canvas, COMBINED_TEXT, xPos, preferredY, paint, 0f, targetGhostCenter
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