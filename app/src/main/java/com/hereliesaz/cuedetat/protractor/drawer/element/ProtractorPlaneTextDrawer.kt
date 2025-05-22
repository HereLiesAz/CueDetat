package com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import android.graphics.Paint
// import android.graphics.Path
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max


internal fun getDynamicTextSizePPD(baseSize: Float, zoomFactor: Float, config: ProtractorConfig, isHelperLineLabel: Boolean = false, sizeMultiplier: Float = 1f): Float {
    val effectiveZoom = if (isHelperLineLabel) zoomFactor.coerceIn(0.7f, 1.3f) else zoomFactor // More dampening for line labels
    return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.HELPER_TEXT_MIN_SIZE_FACTOR, config.HELPER_TEXT_MAX_SIZE_FACTOR))
        .coerceAtLeast(baseSize * sizeMultiplier * config.HELPER_TEXT_MIN_SIZE_FACTOR * 0.7f) // Min size can be smaller for some labels
}

class ProtractorPlaneTextDrawer(
    private val attemptTextDrawInternal: (canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float) -> Boolean,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {

    fun draw(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        config: ProtractorConfig,
        aimLineStartX: Float, aimLineStartY: Float,
        aimLineCueX: Float, aimLineCueY: Float,
        aimLineNormDirX: Float, aimLineNormDirY: Float,
        deflectionVectorX: Float, deflectionVectorY: Float // This is one of the unit deflection vectors
    ) {
        if (!state.areTextLabelsVisible) return

        val zoomFactor = state.zoomFactor.coerceAtLeast(0.4f)
        val baseHelperTextSize = config.HELPER_TEXT_BASE_SIZE
        val textOffsetFromLine = 50f / zoomFactor // Increased offset a bit

        // "Hold phone over the cue ball..." - Already handled (AppHelpTextPhoneOverCue)
        // No changes to this specific label's text, color, or general position logic from previous state.
        // Size will be affected by general HELPER_TEXT_BASE_SIZE increase.
        val phoneOverCuePaint = paints.helperTextPhoneOverCuePaint
        phoneOverCuePaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true)
        phoneOverCuePaint.textAlign = Paint.Align.CENTER
        if (aimLineStartX != 0f || aimLineStartY != 0f) {
            val textBlockX = aimLineStartX
            val textBlockY = aimLineStartY + (90f / zoomFactor)
            val cueBallHelpText = "Hold phone over the cue ball,\ndirectly below this."
            attemptTextDrawInternal(canvas, cueBallHelpText, textBlockX, textBlockY, phoneOverCuePaint, 0f)
        }


        // "Projected Shot Line" - AppHelpTextPurple
        val projectedShotLinePaint = paints.helperTextProjectedShotLinePaint
        projectedShotLinePaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true)
        if (aimLineNormDirX != 0f || aimLineNormDirY != 0f) {
            val angleRad = atan2(aimLineNormDirY, aimLineNormDirX)
            val perpOffsetX = sin(angleRad) * textOffsetFromLine
            val perpOffsetY = -cos(angleRad) * textOffsetFromLine
            projectedShotLinePaint.textAlign = Paint.Align.CENTER
            val displayX = aimLineCueX + aimLineNormDirX * state.currentLogicalRadius * 2.2f // Slightly further
            val displayY = (aimLineCueY + aimLineNormDirY * state.currentLogicalRadius * 2.2f)
            attemptTextDrawInternal(canvas, "Projected Shot Line", displayX + perpOffsetX, displayY + perpOffsetY, projectedShotLinePaint, Math.toDegrees(angleRad.toDouble()).toFloat())
        }

        // Deflection Line Labels - "Cue Ball Path" and "Tangent Line"
        if (deflectionVectorX != 0f || deflectionVectorY != 0f) {
            val cueBallPathPaint = paints.helperTextCueBallPathPaint // Uses AppPurple
            cueBallPathPaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, config.CUE_BALL_PATH_TEXT_SIZE_FACTOR)
            cueBallPathPaint.textAlign = Paint.Align.CENTER

            val tangentLinePaint = paints.helperTextTangentLinePaint // Uses AppHelpTextTangentLine (purple_200)
            tangentLinePaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, config.TANGENT_LINE_TEXT_SIZE_FACTOR)
            tangentLinePaint.textAlign = Paint.Align.CENTER

            val screenMaxDimLogical = max(viewWidthProvider(), viewHeightProvider()) / (2f * zoomFactor)
            // Move Tangent Line (now on dotted side) further out
            val tangentLabelDist = state.currentLogicalRadius + screenMaxDimLogical * 0.45f
            // Cue Ball Path (now on solid side) can be a bit closer if needed, or same distance
            val cuePathLabelDist = state.currentLogicalRadius + screenMaxDimLogical * 0.30f


            val alphaDeg = state.protractorRotationAngle; val epsilon = 0.5f
            // isRightSideSolid means the deflectionVectorX, deflectionVectorY direction corresponds to the solid line
            val isRightSideDeflectionSolid = (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) || (alphaDeg <= epsilon || alphaDeg >= 360f - epsilon)

            val tangentText = "Tangent Line"
            val cuePathText = "Cue Ball Path"

            // Vector 1 (deflectionVectorX, deflectionVectorY)
            val angleVec1Rad = atan2(deflectionVectorY, deflectionVectorX)
            val textX1 = state.cueCircleCenter.x + cos(angleVec1Rad) * (if(isRightSideDeflectionSolid) cuePathLabelDist else tangentLabelDist)
            val textY1 = state.cueCircleCenter.y + sin(angleVec1Rad) * (if(isRightSideDeflectionSolid) cuePathLabelDist else tangentLabelDist)
            val rotation1 = Math.toDegrees(angleVec1Rad.toDouble()).toFloat()
            attemptTextDrawInternal(canvas,
                if(isRightSideDeflectionSolid) cuePathText else tangentText,
                textX1, textY1,
                if(isRightSideDeflectionSolid) cueBallPathPaint else tangentLinePaint,
                if(isRightSideDeflectionSolid) rotation1 else rotation1 // Both stay normally oriented for now
            )

            // Vector 2 (-deflectionVectorX, -deflectionVectorY)
            val angleVec2Rad = atan2(-deflectionVectorY, -deflectionVectorX)
            val textX2 = state.cueCircleCenter.x + cos(angleVec2Rad) * (if(!isRightSideDeflectionSolid) cuePathLabelDist else tangentLabelDist)
            val textY2 = state.cueCircleCenter.y + sin(angleVec2Rad) * (if(!isRightSideDeflectionSolid) cuePathLabelDist else tangentLabelDist)
            val rotation2 = Math.toDegrees(angleVec2Rad.toDouble()).toFloat()
            // If this side is Cue Ball Path (was originally dotted), flip its text
            val finalRotation2 = if(!isRightSideDeflectionSolid) rotation2 + 180f else rotation2

            attemptTextDrawInternal(canvas,
                if(!isRightSideDeflectionSolid) cuePathText else tangentText,
                textX2, textY2,
                if(!isRightSideDeflectionSolid) cueBallPathPaint else tangentLinePaint,
                finalRotation2
            )
        }

        // "Aim this at the pocket." - AppHelpTextPocketAim (Teal_200)
        val pocketAimPaint = paints.helperTextPocketAimPaint
        pocketAimPaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, config.POCKET_AIM_TEXT_BASE_SIZE_FACTOR)
        canvas.save()
        canvas.translate(state.targetCircleCenter.x, state.targetCircleCenter.y)
        canvas.rotate(state.protractorRotationAngle)
        pocketAimPaint.textAlign = Paint.Align.CENTER
        val pocketLineLabelY = -(state.currentLogicalRadius + (30f / zoomFactor)) // Closer
        val pocketLineLabelX = 0f // Centered on the line
        attemptTextDrawInternal(canvas, "Aim this at the pocket.", pocketLineLabelX, pocketLineLabelY, pocketAimPaint, 90f) // Rotated 90 deg
        canvas.restore()
    }
}