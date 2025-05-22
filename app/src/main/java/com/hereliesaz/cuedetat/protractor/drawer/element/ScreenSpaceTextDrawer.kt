package com.hereliesaz.cuedetat.protractor.drawer.element // Or com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextPocketAim
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextWhite
import com.hereliesaz.cuedetat.ui.theme.AppHelpTextYellow
import kotlin.math.max


internal fun getDynamicTextSizeSSD( // For non-warning texts
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
    private val attemptTextDrawInternal: (canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float) -> Boolean,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {
    private var lastWarningIndex = -1

    private val WARNING_TEXT_AREA_START_X_PADDING = 32f
    private val WARNING_TEXT_AREA_RIGHT_MARGIN = 190f

    private val WARNING_TEXT_DIRECT_LINE_SPACING_MULTIPLIER = 1.15f
    private val MIN_WARNING_FONT_SIZE = 40f

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
        val zoomFactorForScalableText = state.zoomFactor

        // --- Warning Text - Direct Drawing with Strict Margins & Font Scaling ---
        if (isShotCurrentlyInvalid && warningToDisplay != null) {
            val screenWidth = viewWidthProvider()
            val screenHeight = viewHeightProvider()

            val warningPaint = TextPaint(paints.insultingWarningTextPaint)
            var currentFontSize = config.WARNING_TEXT_BASE_SIZE // Default large size

            // Check for specific warnings that need a smaller initial font size
            if (warningToDisplay == ProtractorConfig.WARNING_MRS_CALLED ||
                warningToDisplay == ProtractorConfig.WARNING_YODA_SAYS) {
                currentFontSize = config.SPECIAL_WARNING_TEXT_SMALLER_SIZE
            }
            // Max font size for scaling logic to prevent special warnings from scaling *up* beyond their intended smaller size.
            val maxAllowedFontSizeForThisWarning = currentFontSize * 1.1f


            warningPaint.textSize = currentFontSize
            warningPaint.textAlign = Paint.Align.RIGHT

            val lineBreakMaxWidth = screenWidth - WARNING_TEXT_AREA_START_X_PADDING - WARNING_TEXT_AREA_RIGHT_MARGIN

            if (lineBreakMaxWidth > 0) {
                val longestWord = getLongestWord(warningToDisplay)
                if (longestWord.isNotEmpty()) {
                    val longestWordWidth = warningPaint.measureText(longestWord) // Measured with currentFontSize
                    if (longestWordWidth > lineBreakMaxWidth) {
                        // Scale font size down if the longest word is too wide for the available line width
                        val scaleFactor = lineBreakMaxWidth / longestWordWidth
                        currentFontSize *= (scaleFactor * 0.98f) // Apply scale factor with a tiny buffer
                        // Coerce to prevent font from becoming too small or excessively large (relative to its starting point)
                        currentFontSize = currentFontSize.coerceIn(MIN_WARNING_FONT_SIZE, maxAllowedFontSizeForThisWarning)
                        warningPaint.textSize = currentFontSize // Update paint with adjusted size
                    }
                }

                val lines = breakTextIntoLines(warningToDisplay, warningPaint, lineBreakMaxWidth)

                if (lines.isNotEmpty()) {
                    val fm = warningPaint.fontMetrics
                    val effectiveBaselineDistance = warningPaint.fontSpacing * WARNING_TEXT_DIRECT_LINE_SPACING_MULTIPLIER
                    val totalVisualHeight = -fm.ascent + (lines.size - 1) * effectiveBaselineDistance + fm.descent
                    val blockTopY = (screenHeight / 2f) - (totalVisualHeight / 2f)
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

        // --- Other Help Texts - Continue using existing mechanism ---
        if (state.areTextLabelsVisible) {
            val targetLabelPaint = paints.targetBallLabelPaint
            targetLabelPaint.textSize = getDynamicTextSizeSSD(config.BASE_GHOST_BALL_TEXT_SIZE, zoomFactorForScalableText, config)
            val cueLabelPaint = paints.cueBallLabelPaint
            cueLabelPaint.textSize = getDynamicTextSizeSSD(config.BASE_GHOST_BALL_TEXT_SIZE, zoomFactorForScalableText, config)

            val tmLabel = targetLabelPaint.fontMetrics
            val labelPadding = 20f / zoomFactorForScalableText.coerceAtLeast(0.5f)

            val targetLabelY = targetGhostCenterY - targetGhostRadius - labelPadding - tmLabel.descent
            attemptTextDrawInternal(canvas, "Target Ball", targetGhostCenterX, targetLabelY, targetLabelPaint, 0f)

            val cueLabelY = cueGhostCenterY - cueGhostRadius - labelPadding - tmLabel.descent
            attemptTextDrawInternal(canvas, "Cue Ball", cueGhostCenterX, cueLabelY, cueLabelPaint, 0f)

            val instructionTextPaint = paints.ghostBallInstructionTextPaint
            val baseInstructionSize = config.BASE_GHOST_BALL_TEXT_SIZE * config.CENTER_CUE_INSTRUCTION_SIZE_FACTOR
            instructionTextPaint.textSize = getDynamicTextSizeSSD(baseInstructionSize, zoomFactorForScalableText, config)
            instructionTextPaint.textAlign = Paint.Align.CENTER
            val instructionYOffset = (max(targetGhostRadius, cueGhostRadius) * 0.25f) + (15f / zoomFactorForScalableText.coerceAtLeast(0.5f))

            instructionTextPaint.color = AppHelpTextYellow.toArgb()
            var yPosInstruction = targetGhostCenterY + instructionYOffset
            attemptTextDrawInternal(canvas, "Zoom to match object ball", targetGhostCenterX, yPosInstruction, instructionTextPaint, 0f)
            yPosInstruction += instructionTextPaint.textSize * 1.1f
            attemptTextDrawInternal(canvas, "& align outline", targetGhostCenterX, yPosInstruction, instructionTextPaint, 0f)

            val centerCueUserInstructionPaint = paints.ghostBallInstructionTextPaint
            centerCueUserInstructionPaint.color = AppHelpTextPocketAim.toArgb()
            centerCueUserInstructionPaint.textSize = getDynamicTextSizeSSD(
                config.BASE_GHOST_BALL_TEXT_SIZE,
                1.0f,
                config,
                isStaticSize = true,
                sizeMultiplier = config.CENTER_CUE_INSTRUCTION_SIZE_FACTOR
            )
            centerCueUserInstructionPaint.textAlign = Paint.Align.CENTER

            val screenCenterX = viewWidthProvider() / 2f
            val screenBottomInstructionYBaseline = viewHeightProvider() - 80f
            val centerCueText = "Center actual cue ball here,\nunder your phone."
            attemptTextDrawInternal(canvas, centerCueText, screenCenterX, screenBottomInstructionYBaseline, centerCueUserInstructionPaint, 0f)

            val hintPaint = paints.helperTextInteractionHintPaint
            hintPaint.textSize = getDynamicTextSizeSSD(
                config.HELPER_TEXT_BASE_SIZE,
                1.0f,
                config,
                isStaticSize = true,
                sizeMultiplier = 0.70f
            )
            val hintTextYBase = viewHeightProvider() - 20f
            val hintLineSpacing = hintPaint.textSize * 1.3f
            attemptTextDrawInternal(canvas, "Pan: Rotate", 20f, hintTextYBase - hintLineSpacing, hintPaint, 0f)
            attemptTextDrawInternal(canvas, "Pinch: Zoom", 20f, hintTextYBase, hintPaint, 0f)
        }
    }
}