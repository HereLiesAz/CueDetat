package com.hereliesaz.cuedetat.protractor.drawer.element

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetat.protractor.ProtractorConfig
import com.hereliesaz.cuedetat.protractor.ProtractorPaints
import com.hereliesaz.cuedetat.protractor.ProtractorState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.max


internal fun getDynamicTextSizePPD(baseSize: Float, zoomFactor: Float, config: ProtractorConfig, isHelperLineLabel: Boolean = false, sizeMultiplier: Float = 1f): Float {
    val effectiveZoom = if (isHelperLineLabel) zoomFactor.coerceIn(0.7f, 1.3f) else zoomFactor
    return (baseSize * sizeMultiplier * effectiveZoom.coerceIn(config.HELPER_TEXT_MIN_SIZE_FACTOR, config.HELPER_TEXT_MAX_SIZE_FACTOR))
        .coerceAtLeast(baseSize * sizeMultiplier * config.HELPER_TEXT_MIN_SIZE_FACTOR * 0.7f)
}

class ProtractorPlaneTextDrawer(
    private val attemptTextDrawInternal: (canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, rotationDegrees: Float, targetCenter: PointF) -> Boolean,
    private val viewWidthProvider: () -> Int,
    private val viewHeightProvider: () -> Int
) {

    private fun getReadableRotation(degrees: Float): Float {
        var normalizedAngle = degrees % 360f
        if (normalizedAngle < 0) {
            normalizedAngle += 360f
        }
        return if (normalizedAngle > 90f && normalizedAngle < 270f) {
            degrees + 180f
        } else {
            degrees
        }
    }

    fun draw(
        canvas: Canvas,
        state: ProtractorState,
        paints: ProtractorPaints,
        config: ProtractorConfig,
        aimLineStartX: Float, aimLineStartY: Float,
        aimLineCueX: Float, aimLineCueY: Float,
        aimLineNormDirX: Float, aimLineNormDirY: Float,
        deflectionVectorX: Float, deflectionVectorY: Float // Geometric +deflectionVector from cue to target tangent
    ) {
        if (!state.areTextLabelsVisible) return

        val zoomFactor = state.zoomFactor.coerceAtLeast(0.4f)
        val baseHelperTextSize = config.HELPER_TEXT_BASE_SIZE
        val textOffsetFromLineGeneral = 30f / zoomFactor
        val cueBallCenter = state.cueCircleCenter
        val targetBallCenter = state.targetCircleCenter

        // "Projected Shot Line"
        val projectedShotLinePaint = paints.helperTextProjectedShotLinePaint
        projectedShotLinePaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, 1.0f)
        if (aimLineNormDirX != 0f || aimLineNormDirY != 0f) {
            val angleRad = atan2(aimLineNormDirY, aimLineNormDirX)
            val perpOffsetX = sin(angleRad) * textOffsetFromLineGeneral
            val perpOffsetY = -cos(angleRad) * textOffsetFromLineGeneral
            val distanceFromCueCenter = state.currentLogicalRadius * 1.3f
            val displayX = aimLineCueX + aimLineNormDirX * distanceFromCueCenter
            val displayY = aimLineCueY + aimLineNormDirY * distanceFromCueCenter
            attemptTextDrawInternal(canvas, "Projected Shot Line", displayX + perpOffsetX, displayY + perpOffsetY, projectedShotLinePaint, Math.toDegrees(angleRad.toDouble()).toFloat(), cueBallCenter)
        }

        // Deflection Line Labels
        if (deflectionVectorX != 0f || deflectionVectorY != 0f) { // Check if deflectionVector is valid
            val tangentLineTextPaint = paints.helperTextCueBallPathPaint
            tangentLineTextPaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, config.TANGENT_LINE_TEXT_SIZE_FACTOR)

            val cueBallPathTextPaint = paints.helperTextTangentLinePaint
            cueBallPathTextPaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, config.CUE_BALL_PATH_TEXT_SIZE_FACTOR)

            val deflectionLabelDistFromCue = state.currentLogicalRadius * 3.4f // How far from cue ball center

            val tangentText = "Tangent Line"
            val cuePathText = "Cue Ball Path"

            // deflectionVectorX,Y is already the unit vector perpendicular to cue-target line, pointing "right"
            // Let's call it perpRightX, perpRightY
            val perpRightX = deflectionVectorX
            val perpRightY = deflectionVectorY
            // And perpLeftX, perpLeftY is its opposite
            val perpLeftX = -deflectionVectorX
            val perpLeftY = -deflectionVectorY

            val alphaDeg = state.protractorRotationAngle; val epsilon = 0.5f
            // isPositiveDeflectionVectorSolid means the line along (+deflectionVectorX,Y) is drawn solid
            val isPerpRightSideSolid = (alphaDeg > epsilon && alphaDeg < (180f - epsilon)) || (alphaDeg <= epsilon || alphaDeg >= 360f - epsilon)


            val tangentLineAngleRad: Float
            val tangentLineRotation: Float
            val tangentLineX: Float
            val tangentLineY: Float

            val cuePathAngleRad: Float
            val cuePathRotation: Float
            val cuePathX: Float
            val cuePathY: Float

            if (isPerpRightSideSolid) {
                // Solid line is on the "perpRight" side. "Tangent Line" goes here.
                tangentLineAngleRad = atan2(perpRightY, perpRightX)
                tangentLineRotation = getReadableRotation(Math.toDegrees(tangentLineAngleRad.toDouble()).toFloat())
                tangentLineX = cueBallCenter.x + cos(tangentLineAngleRad) * deflectionLabelDistFromCue
                tangentLineY = cueBallCenter.y + sin(tangentLineAngleRad) * deflectionLabelDistFromCue

                // Dotted line is on the "perpLeft" side. "Cue Ball Path" goes here (flipped).
                cuePathAngleRad = atan2(perpLeftY, perpLeftX)
                cuePathRotation = getReadableRotation(Math.toDegrees(cuePathAngleRad.toDouble()).toFloat() + 180f)
                cuePathX = cueBallCenter.x + cos(cuePathAngleRad) * deflectionLabelDistFromCue
                cuePathY = cueBallCenter.y + sin(cuePathAngleRad) * deflectionLabelDistFromCue
            } else {
                // Solid line is on the "perpLeft" side. "Tangent Line" goes here.
                tangentLineAngleRad = atan2(perpLeftY, perpLeftX)
                tangentLineRotation = getReadableRotation(Math.toDegrees(tangentLineAngleRad.toDouble()).toFloat())
                tangentLineX = cueBallCenter.x + cos(tangentLineAngleRad) * deflectionLabelDistFromCue
                tangentLineY = cueBallCenter.y + sin(tangentLineAngleRad) * deflectionLabelDistFromCue

                // Dotted line is on the "perpRight" side. "Cue Ball Path" goes here (flipped).
                cuePathAngleRad = atan2(perpRightY, perpRightX)
                cuePathRotation = getReadableRotation(Math.toDegrees(cuePathAngleRad.toDouble()).toFloat() + 180f)
                cuePathX = cueBallCenter.x + cos(cuePathAngleRad) * deflectionLabelDistFromCue
                cuePathY = cueBallCenter.y + sin(cuePathAngleRad) * deflectionLabelDistFromCue
            }

            attemptTextDrawInternal(canvas, tangentText, tangentLineX, tangentLineY, tangentLineTextPaint, tangentLineRotation, cueBallCenter)
            attemptTextDrawInternal(canvas, cuePathText, cuePathX, cuePathY, cueBallPathTextPaint, cuePathRotation, cueBallCenter)
        }

        // "Aim this at the pocket."
        val pocketAimPaint = paints.helperTextPocketAimPaint
        pocketAimPaint.textSize = getDynamicTextSizePPD(baseHelperTextSize, state.zoomFactor, config, true, config.POCKET_AIM_TEXT_BASE_SIZE_FACTOR)
        canvas.save()
        canvas.translate(targetBallCenter.x, targetBallCenter.y)
        canvas.rotate(state.protractorRotationAngle)
        val pocketLineLabelY = -(state.currentLogicalRadius + (30f / zoomFactor))
        val pocketLineLabelX = 0f
        attemptTextDrawInternal(canvas, "Aim this at the pocket.", pocketLineLabelX, pocketLineLabelY, pocketAimPaint, 90f, PointF(0f,0f))
        canvas.restore()
    }
}