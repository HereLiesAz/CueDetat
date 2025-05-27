package com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextPocketAim
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextWhite
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextYellow
import kotlin.math.max


internal fun getDynamicTextSizeSSD(
    baseSize: Float,
    zoomFactor: Float,
    config: ProtractorConfig,
    isStaticSize: Boolean = false,
    sizeMultiplier: Float = 1f
): Float {
    if (isStaticSize) {
        return baseSize * sizeMultiplier
    }
    val effectiveZoom = zoomFactor.coerceIn(0.7f, 1.3f)
    val minSizeFactor = config.HELPER_TEXT_MIN_SIZE_FACTOR * 0.8f
    return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.HELPER_TEXT_MIN_SIZE_FACTOR, config.HELPER_TEXT_MAX_SIZE_FACTOR))
        .coerceAtLeast(baseSize * sizeMultiplier * minSizeFactor)
}

class ScreenSpaceTextDrawer(
    private val attemptTextDrawInternal: (canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float, targetCenter: PointF) -> Boolean,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private val WARNING_TEXT_AREA_START_X_PADDING = 32f
    private val WARNING_TEXT_AREA_RIGHT_MARGIN = 190f
    private val WARNING_TEXT_DIRECT_LINE_SPACING_MULTIPLIER = 1.15f
    private val MIN_WARNING_FONT_SIZE = 40f
    private val REPURPOSED_WARNING_VERTICAL_CENTER_TARGET_PERCENT = 0.45f

    private val FIT_TARGET_TEXT_RIGHT_CLEARANCE_MARGIN = 190f
    private val MIN_FIT_TARGET_FONT_SIZE = 20f
    private val FIT_TARGET_TEXT_X_PADDING_FROM_CIRCLE = 2f // Further reduced padding


    private fun getLongestWord(text: String): String {
        return text.split(Regex("\\s+")).maxByOrNull { it.length } ?: ""
    }

    private fun breakTextIntoLines(text: String, paint: TextPaint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        if (text.isEmpty() || maxWidth <= 0) return lines
        var currentLine = StringBuilder()
        val words = text.split(Regex("\\s+"))

        for (word in words) {
            if (word.isEmpty()) continue
            val wordWidthWithSpace = paint.measureText(if (currentLine.isEmpty()) word else " $word")
            val currentLineWidth = paint.measureText(currentLine.toString())

            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLineWidth + wordWidthWithSpace <= maxWidth) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine.clear()
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }


    fun draw(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        config: ProtractorConfig,
        targetGhostCenterX: Float, targetGhostCenterY: Float, targetGhostRadius: Float,
        cueGhostCenterX: Float, cueGhostCenterY: Float, cueGhostRadius: Float,
        isShotCurrentlyInvalid: Boolean,
        warningToDisplay: String?
    ) {
        val screenWidth = viewWidthProvider()
        val screenHeight = viewHeightProvider() // Get screen height once
        val zoomFactorForScalableText = state.zoomFactor
        val globalProtractorCenter = state.targetCircleCenter

        // --- Repurposed Warning Text ---
        if (warningToDisplay != null) {
            val warningPaint = TextPaint(paints.insultingWarningTextPaint)
            var currentFontSize = config.WARNING_TEXT_BASE_SIZE
            if (warningToDisplay == ProtractorConfig.WARNING_MRS_CALLED || warningToDisplay == ProtractorConfig.WARNING_YODA_SAYS) {
                currentFontSize = config.SPECIAL_WARNING_TEXT_SMALLER_SIZE
            }
            val maxAllowedFontSizeForThisWarning = currentFontSize * 1.1f
            warningPaint.textSize = currentFontSize
            warningPaint.textAlign = Paint.Align.RIGHT
            val lineBreakMaxWidth = screenWidth - WARNING_TEXT_AREA_START_X_PADDING - WARNING_TEXT_AREA_RIGHT_MARGIN
            if (lineBreakMaxWidth > 0) {
                val longestWord = getLongestWord(warningToDisplay)
                if (longestWord.isNotEmpty()) {
                    val longestWordWidth = warningPaint.measureText(longestWord)
                    if (longestWordWidth > lineBreakMaxWidth) {
                        val scaleFactor = lineBreakMaxWidth / longestWordWidth
                        currentFontSize *= (scaleFactor * 0.98f)
                        currentFontSize = currentFontSize.coerceIn(MIN_WARNING_FONT_SIZE, maxAllowedFontSizeForThisWarning)
                        warningPaint.textSize = currentFontSize
                    }
                }
                val lines = breakTextIntoLines(warningToDisplay, warningPaint, lineBreakMaxWidth)
                if (lines.isNotEmpty()) {
                    val fm = warningPaint.fontMetrics
                    val effectiveBaselineDistance = warningPaint.fontSpacing * WARNING_TEXT_DIRECT_LINE_SPACING_MULTIPLIER
                    val totalVisualHeight = -fm.ascent + (lines.size - 1) * effectiveBaselineDistance + fm.descent
                    val blockTopY = (screenHeight * REPURPOSED_WARNING_VERTICAL_CENTER_TARGET_PERCENT) - (totalVisualHeight / 2f)
                    var currentYBaseline = blockTopY - fm.ascent
                    val xPos = screenWidth - WARNING_TEXT_AREA_RIGHT_MARGIN
                    canvas.save()
                    for (line in lines) {
                        canvas.drawText(line, xPos, currentYBaseline, warningPaint)
                        currentYBaseline += effectiveBaselineDistance
                    }
                    canvas.restore()
                }
            }
        }

        // --- Other Screen Space Help Texts ---
        if (state.areTextLabelsVisible) {
            val targetLabelPaint = paints.targetBallLabelPaint
            targetLabelPaint.textSize = getDynamicTextSizeSSD(config.BASE_GHOST_BALL_TEXT_SIZE, zoomFactorForScalableText, config)
            val cueLabelPaint = paints.cueBallLabelPaint
            cueLabelPaint.textSize = getDynamicTextSizeSSD(config.BASE_GHOST_BALL_TEXT_SIZE, zoomFactorForScalableText, config)
            val tmLabel = targetLabelPaint.fontMetrics
            val labelPadding = 20f / zoomFactorForScalableText.coerceAtLeast(0.5f)

            val targetGhostCenterPoint = PointF(targetGhostCenterX, targetGhostCenterY)
            val targetLabelY = targetGhostCenterY - targetGhostRadius - labelPadding - tmLabel.descent
            attemptTextDrawInternal(canvas, "Target Ball", targetGhostCenterX, targetLabelY, targetLabelPaint, 0f, targetGhostCenterPoint)

            val cueGhostCenterPoint = PointF(cueGhostCenterX, cueGhostCenterY)
            val cueLabelY = cueGhostCenterY - cueGhostRadius - labelPadding - tmLabel.descent
            attemptTextDrawInternal(canvas, "Cue Ball", cueGhostCenterX, cueLabelY, cueLabelPaint, 0f, cueGhostCenterPoint)


            // Instructional Text for Target Ghost Ball (9b)
            val instructionTextPaint = TextPaint(paints.ghostBallInstructionTextPaint)
            val baseInstructionSize = config.BASE_GHOST_BALL_TEXT_SIZE * config.CENTER_CUE_INSTRUCTION_SIZE_FACTOR
            var currentInstructionFontSize = getDynamicTextSizeSSD(baseInstructionSize, zoomFactorForScalableText, config)
            instructionTextPaint.textSize = currentInstructionFontSize
            instructionTextPaint.textAlign = Paint.Align.LEFT
            instructionTextPaint.color = AppHelpTextYellow.toArgb()

            val instructionLine1 = "Fit this to your"
            val instructionLine2 = "target ball, IRL."
            val instructionTextCombined = "$instructionLine1\n$instructionLine2"

            val instructionTextX = targetGhostCenterX + targetGhostRadius + (FIT_TARGET_TEXT_X_PADDING_FROM_CIRCLE / zoomFactorForScalableText.coerceAtLeast(0.5f))
            val maxInstructionWidth = screenWidth - instructionTextX - FIT_TARGET_TEXT_RIGHT_CLEARANCE_MARGIN

            if (maxInstructionWidth > 0) {
                val textWidth1 = instructionTextPaint.measureText(instructionLine1)
                val textWidth2 = instructionTextPaint.measureText(instructionLine2)
                val longerPredefinedLineWidth = max(textWidth1, textWidth2)

                if (longerPredefinedLineWidth > maxInstructionWidth) {
                    val scaleFactor = maxInstructionWidth / longerPredefinedLineWidth
                    currentInstructionFontSize *= (scaleFactor * 0.98f)
                    currentInstructionFontSize = currentInstructionFontSize.coerceIn(MIN_FIT_TARGET_FONT_SIZE, baseInstructionSize * 1.1f)
                    instructionTextPaint.textSize = currentInstructionFontSize
                }

                val fmInstruction = instructionTextPaint.fontMetrics
                val instructionLineHeight = instructionTextPaint.fontSpacing
                val numLines = 2
                val blockHeight = (fmInstruction.descent - fmInstruction.ascent) * numLines + instructionLineHeight * (numLines - 1)
                val yPosInstructionBlock_baseline = targetGhostCenterY - (blockHeight / 2f) - fmInstruction.ascent

                attemptTextDrawInternal(canvas, instructionTextCombined, instructionTextX, yPosInstructionBlock_baseline, instructionTextPaint, 0f, targetGhostCenterPoint)
            }

            // "Center actual cue ball here..." (11b) - Moved UP
            val centerCueUserInstructionPaint = paints.ghostBallInstructionTextPaint
            centerCueUserInstructionPaint.color = AppHelpTextPocketAim.toArgb()
            centerCueUserInstructionPaint.textSize = getDynamicTextSizeSSD(
                config.BASE_GHOST_BALL_TEXT_SIZE, 1.0f, config, true, config.CENTER_CUE_INSTRUCTION_SIZE_FACTOR
            )
            centerCueUserInstructionPaint.textAlign = Paint.Align.CENTER
            val screenCenterX = viewWidthProvider() / 2f
            // New Y position: e.g., 85% of screen height from top for the baseline
            val bottomInstructionYBaseline = screenHeight * 0.85f
            val centerCueText = "Center actual cue ball here,\nunder your phone."
            attemptTextDrawInternal(canvas, centerCueText, screenCenterX, bottomInstructionYBaseline, centerCueUserInstructionPaint, 0f, globalProtractorCenter)

            val hintPaint = paints.helperTextInteractionHintPaint
            hintPaint.textSize = getDynamicTextSizeSSD(
                config.HELPER_TEXT_BASE_SIZE, 1.0f, config, true, 0.70f
            )
            hintPaint.textAlign = Paint.Align.LEFT
            val hintTextYBase = viewHeightProvider() - 20f // Stays at bottom
            val hintLineSpacing = hintPaint.textSize * 1.3f
            attemptTextDrawInternal(canvas, "Pan: Rotate", 20f, hintTextYBase - hintLineSpacing, hintPaint, 0f, globalProtractorCenter)
            attemptTextDrawInternal(canvas, "Pinch: Zoom", 20f, hintTextYBase, hintPaint, 0f, globalProtractorCenter)
        }
    }
}