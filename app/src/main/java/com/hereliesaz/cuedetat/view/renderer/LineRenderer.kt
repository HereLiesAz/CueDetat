// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val SHOT_LINE_LABEL_DISTANCE_FACTOR = 15f
    private val RIGHT_TANGENT_LABEL_DISTANCE_FACTOR = 7f
    private val LEFT_TANGENT_LABEL_DISTANCE_FACTOR = 7f
    private val AIMING_LINE_LABEL_DISTANCE_FACTOR = 10f
    private val PROTRACTOR_LABEL_DISTANCE_FACTOR = 20f
    private val BANKING_SHOT_LINE_LABEL_DISTANCE_FACTOR = 8f

    private val SHOT_LINE_LABEL_ANGLE_OFFSET = -2f
    private val RIGHT_TANGENT_LABEL_ANGLE_OFFSET = -5f
    private val LEFT_TANGENT_LABEL_ANGLE_OFFSET = 5f
    private val AIMING_LINE_LABEL_ANGLE_OFFSET = -2f
    private val PROTRACTOR_LABEL_ANGLE_OFFSET = 0f
    private val BANKING_SHOT_LINE_LABEL_ANGLE_OFFSET = -2f

    private val SHOT_LINE_LABEL_ROTATION = 0f
    private val RIGHT_TANGENT_LABEL_ROTATION = 0f
    private val LEFT_TANGENT_LABEL_ROTATION = 180f
    private val AIMING_LINE_LABEL_ROTATION = 0f
    private val PROTRACTOR_LABEL_ROTATION = 90f
    private val BANKING_SHOT_LINE_LABEL_ROTATION = 0f

    private val SHOT_LINE_LABEL_FONT_SIZE = 38f
    private val RIGHT_TANGENT_LABEL_FONT_SIZE = 38f
    private val LEFT_TANGENT_LABEL_FONT_SIZE = 38f
    private val AIMING_LINE_LABEL_FONT_SIZE = 38f
    private val PROTRACTOR_LABEL_FONT_SIZE = 42f
    private val BANKING_SHOT_LINE_LABEL_FONT_SIZE = 34f

    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val textRenderer = LineTextRenderer()

    private val tableToBallRatioLong = 88f
    private val tableToBallRatioShort = 44f

    private enum class Rail { TOP, BOTTOM, LEFT, RIGHT, NONE }
    private data class IntersectionResult(val point: PointF?, val railHit: Rail, val distanceSq: Float)

    private var lastProtractorShotLineLog = ""
    private var lastProtractorAimingLineLog = ""


    fun drawLogicalLines(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        paints.lineTextPaint.typeface = typeface

        if (state.isBankingMode) {
            state.actualCueBall?.let { bankingBall ->
                state.bankingAimTarget?.let { aimTarget ->
                    drawBankingShotLinesWithReflection(canvas, bankingBall, aimTarget, state, paints)
                }
            }
            return
        }

        val protractorShotLineDrawn = drawProtractorShotLine(canvas, state, paints)

        var unrotatedGhostCueLogicalPosForLog: PointF? = null

        canvas.save()
        canvas.translate(state.protractorUnit.logicalPosition.x, state.protractorUnit.logicalPosition.y) // Use logicalPosition
        canvas.rotate(state.protractorUnit.rotationDegrees)

        val distanceBetweenProtractorCenters = 2 * state.protractorUnit.radius
        val unrotatedProtractorGhostCueLogicalPos = PointF(0f, distanceBetweenProtractorCenters)
        unrotatedGhostCueLogicalPosForLog = unrotatedProtractorGhostCueLogicalPos

        drawTangentLines(canvas, unrotatedProtractorGhostCueLogicalPos, paints, state)
        val protractorAimingLineDrawn = drawProtractorAimingAndAngleLines(canvas, unrotatedProtractorGhostCueLogicalPos, paints, state)

        canvas.restore()

        val pShotLog = "ProtShotLine: drawn=$protractorShotLineDrawn, actualCue=${state.actualCueBall != null}, hasInverse=${state.hasInverseMatrix}, radius=${state.protractorUnit.radius}"
        if (pShotLog != lastProtractorShotLineLog) {
            Log.d("LineRenderer", pShotLog); lastProtractorShotLineLog = pShotLog
        }
        val logicalPosString = unrotatedGhostCueLogicalPosForLog?.let { "(${it.x},${it.y})" } ?: "(not set)"
        val pAimLog = "ProtAimLine: drawn=$protractorAimingLineDrawn, radius=${state.protractorUnit.radius}, ghostLogicalPos=$logicalPosString"
        if (pAimLog != lastProtractorAimingLineLog) {
            Log.d("LineRenderer", pAimLog); lastProtractorAimingLineLog = pAimLog
        }
    }

    private fun getAngle(from: PointF, to: PointF): Float {
        return Math.toDegrees(atan2(to.y - from.y, to.x - from.x).toDouble()).toFloat()
    }

    private fun drawBankingShotLinesWithReflection(
        canvas: Canvas, bankingBall: ActualCueBall, initialAimTarget: PointF,
        state: OverlayState, paints: PaintCache
    ) {
        if (bankingBall.radius <= 0) return
        var startPoint = bankingBall.logicalPosition // Use logicalPosition
        var currentAimVector = PointF(initialAimTarget.x - startPoint.x, initialAimTarget.y - startPoint.y)
        val ballRadiusForTableScale = bankingBall.radius
        val tableHeight = tableToBallRatioShort * ballRadiusForTableScale
        val tableWidth = tableToBallRatioLong * ballRadiusForTableScale
        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f
        val tableBounds = RectF(
            tableCenterX - tableWidth / 2, tableCenterY - tableHeight / 2,
            tableCenterX + tableWidth / 2, tableCenterY + tableHeight / 2
        )
        var lastHitRail = Rail.NONE
        val bankLinePaints = listOf(paints.bankShotLinePaint1, paints.bankShotLinePaint2, paints.bankShotLinePaint3)

        for (i in 0..2) {
            val farOffTarget = PointF(startPoint.x + currentAimVector.x * 1000, startPoint.y + currentAimVector.y * 1000)
            val hitResult = findClosestRailIntersection(startPoint, farOffTarget, tableBounds, lastHitRail)
            val currentSegmentPaint = bankLinePaints.getOrElse(i) { bankLinePaints.last() }

            if (hitResult.point != null) {
                canvas.drawLine(startPoint.x, startPoint.y, hitResult.point.x, hitResult.point.y, currentSegmentPaint)
                if (state.areHelpersVisible) {
                    val midSegment = PointF((startPoint.x + hitResult.point.x) / 2, (startPoint.y + hitResult.point.y) / 2)
                    val labelTextPaint = Paint(paints.lineTextPaint).apply { color = currentSegmentPaint.color }
                    textRenderer.draw(canvas, "Bank ${i + 1}", midSegment, getAngle(startPoint, hitResult.point),
                        0f, BANKING_SHOT_LINE_LABEL_ANGLE_OFFSET, BANKING_SHOT_LINE_LABEL_ROTATION,
                        labelTextPaint, BANKING_SHOT_LINE_LABEL_FONT_SIZE, state.zoomSliderPosition)
                }
                startPoint = hitResult.point
                currentAimVector = reflectVector(currentAimVector, hitResult.railHit)
                lastHitRail = hitResult.railHit
                if (i == 2) {
                    val finalFarOffTarget = PointF(startPoint.x + currentAimVector.x * 1000, startPoint.y + currentAimVector.y * 1000)
                    canvas.drawLine(startPoint.x, startPoint.y, finalFarOffTarget.x, finalFarOffTarget.y, currentSegmentPaint)
                }
            } else {
                val finalFarOffTarget = PointF(startPoint.x + currentAimVector.x * 1000, startPoint.y + currentAimVector.y * 1000)
                canvas.drawLine(startPoint.x, startPoint.y, finalFarOffTarget.x, finalFarOffTarget.y, currentSegmentPaint)
                if (i == 0 && state.areHelpersVisible) {
                    val labelTextPaint = Paint(paints.lineTextPaint).apply { color = currentSegmentPaint.color }
                    textRenderer.draw(canvas, "Aim", PointF((startPoint.x + finalFarOffTarget.x)/2, (startPoint.y + finalFarOffTarget.y)/2),
                        getAngle(startPoint, finalFarOffTarget), 0f, BANKING_SHOT_LINE_LABEL_ANGLE_OFFSET, BANKING_SHOT_LINE_LABEL_ROTATION,
                        labelTextPaint, BANKING_SHOT_LINE_LABEL_FONT_SIZE, state.zoomSliderPosition)
                }
                break
            }
        }
    }

    private fun findClosestRailIntersection(start: PointF, endRayTarget: PointF, bounds: RectF, ignoreRail: Rail): IntersectionResult {
        var closestIntersection: PointF? = null; var railHit = Rail.NONE; var minDistanceSq = Float.MAX_VALUE
        val candidates = mutableListOf<IntersectionResult>()
        if (ignoreRail != Rail.TOP) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.left, bounds.top), PointF(bounds.right, bounds.top))?.let { candidates.add(IntersectionResult(it, Rail.TOP, distanceSq(start, it))) }
        if (ignoreRail != Rail.BOTTOM) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.left, bounds.bottom), PointF(bounds.right, bounds.bottom))?.let { candidates.add(IntersectionResult(it, Rail.BOTTOM, distanceSq(start, it))) }
        if (ignoreRail != Rail.LEFT) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.left, bounds.top), PointF(bounds.left, bounds.bottom))?.let { candidates.add(IntersectionResult(it, Rail.LEFT, distanceSq(start, it))) }
        if (ignoreRail != Rail.RIGHT) getLineSegmentRayIntersection(start, endRayTarget, PointF(bounds.right, bounds.top), PointF(bounds.right, bounds.bottom))?.let { candidates.add(IntersectionResult(it, Rail.RIGHT, distanceSq(start, it))) }
        for (candidate in candidates) {
            if (candidate.point != null) {
                val dotProduct = (candidate.point.x - start.x) * (endRayTarget.x - start.x) + (candidate.point.y - start.y) * (endRayTarget.y - start.y)
                if (dotProduct >= -0.001f && candidate.distanceSq < minDistanceSq) { minDistanceSq = candidate.distanceSq; closestIntersection = candidate.point; railHit = candidate.railHit }
            }
        }
        return IntersectionResult(closestIntersection, railHit, minDistanceSq)
    }
    private fun getLineSegmentRayIntersection(rayOrigin: PointF, rayTarget: PointF, segP1: PointF, segP2: PointF): PointF? {
        val rDx = rayTarget.x - rayOrigin.x; val rDy = rayTarget.y - rayOrigin.y; val sDx = segP2.x - segP1.x; val sDy = segP2.y - segP1.y
        val rMagSq = rDx * rDx + rDy * rDy; val sMagSq = sDx * sDx + sDy * sDy
        if (rMagSq < 0.0001f || sMagSq < 0.0001f) return null
        val denominator = rDx * sDy - rDy * sDx
        if (kotlin.math.abs(denominator) < 0.0001f) return null
        val t = ((segP1.x - rayOrigin.x) * sDy - (segP1.y - rayOrigin.y) * sDx) / denominator
        val u = ((segP1.x - rayOrigin.x) * rDy - (segP1.y - rayOrigin.y) * rDx) / denominator
        if (t >= 0 && u >= 0 && u <= 1) return PointF(rayOrigin.x + t * rDx, rayOrigin.y + t * rDy)
        return null
    }
    private fun distanceSq(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x; val dy = p1.y - p2.y; return dx * dx + dy * dy
    }
    private fun reflectVector(incident: PointF, rail: Rail): PointF {
        return when (rail) { Rail.TOP, Rail.BOTTOM -> PointF(incident.x, -incident.y); Rail.LEFT, Rail.RIGHT -> PointF(-incident.x, incident.y); Rail.NONE -> incident }
    }

    private fun drawProtractorShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache): Boolean {
        val startPoint: PointF = state.actualCueBall?.logicalPosition ?: run { // Use logicalPosition
            if (!state.hasInverseMatrix) return false
            val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
            val logicalAnchorArray = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        }
        val throughPoint = state.protractorUnit.protractorCueBallLogicalCenter // Use logical center
        val paintToUse = if (state.isImpossibleShot && !state.isBankingMode) paints.warningPaintRed3 else paints.shotLinePaint
        val label = if (state.areHelpersVisible) "Shot Line" else null
        val textPaint = Paint(paints.lineTextPaint).apply { color = paintToUse.color }
        val lineAngle = getAngle(startPoint, throughPoint)
        val labelDistance = state.protractorUnit.radius * SHOT_LINE_LABEL_DISTANCE_FACTOR

        drawExtendedLineAndLabel(canvas, startPoint, throughPoint, paintToUse, label, textPaint,
            lineAngle, labelDistance, SHOT_LINE_LABEL_ANGLE_OFFSET, SHOT_LINE_LABEL_ROTATION,
            SHOT_LINE_LABEL_FONT_SIZE, state.zoomSliderPosition)
        return true
    }

    private fun drawExtendedLineAndLabel(
        canvas: Canvas, start: PointF, through: PointF, linePaint: Paint,
        labelText: String?, textPaint: Paint,
        lineAngleForLabel: Float, labelDistanceFromThroughPoint: Float,
        labelAngleOffset: Float, labelRotation: Float,
        labelBaseFontSize: Float, zoomSliderPosition: Float
    ) {
        val dirX = through.x - start.x; val dirY = through.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = 5000f; val ndx = dirX / mag; val ndy = dirY / mag
            canvas.drawLine(start.x, start.y, start.x + ndx * extendFactor, start.y + ndy * extendFactor, linePaint)
            if (labelText != null) {
                textRenderer.draw(canvas, labelText, through,
                    lineAngleForLabel, labelDistanceFromThroughPoint,
                    labelAngleOffset, labelRotation, textPaint, labelBaseFontSize, zoomSliderPosition)
            }
        }
    }

    private fun drawTangentLines(canvas: Canvas, ghostCuePosInUnitLogicalSpace: PointF, paints: PaintCache, state: OverlayState) {
        val dxToTarget = 0f - ghostCuePosInUnitLogicalSpace.x
        val dyToTarget = 0f - ghostCuePosInUnitLogicalSpace.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val extend = state.viewWidth.coerceAtLeast(state.viewHeight) * 1.5f
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget

            val rightPaint = if (state.isImpossibleShot || state.protractorUnit.rotationDegrees <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            val leftPaint = if (state.isImpossibleShot || state.protractorUnit.rotationDegrees > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint

            val rightEndPoint = PointF(ghostCuePosInUnitLogicalSpace.x + tangentDx * extend, ghostCuePosInUnitLogicalSpace.y + tangentDy * extend)
            val leftEndPoint = PointF(ghostCuePosInUnitLogicalSpace.x - tangentDx * extend, ghostCuePosInUnitLogicalSpace.y - tangentDy * extend)

            canvas.drawLine(ghostCuePosInUnitLogicalSpace.x, ghostCuePosInUnitLogicalSpace.y, rightEndPoint.x, rightEndPoint.y, rightPaint)
            canvas.drawLine(ghostCuePosInUnitLogicalSpace.x, ghostCuePosInUnitLogicalSpace.y, leftEndPoint.x, leftEndPoint.y, leftPaint)

            if (state.areHelpersVisible) {
                val textPaintRight = Paint(paints.lineTextPaint).apply { color = rightPaint.color }
                val textPaintLeft = Paint(paints.lineTextPaint).apply { color = leftPaint.color }
                val labelDistance = state.protractorUnit.radius * RIGHT_TANGENT_LABEL_DISTANCE_FACTOR

                textRenderer.draw(canvas, "Tangent Line", ghostCuePosInUnitLogicalSpace, getAngle(ghostCuePosInUnitLogicalSpace, rightEndPoint),
                    labelDistance, RIGHT_TANGENT_LABEL_ANGLE_OFFSET, RIGHT_TANGENT_LABEL_ROTATION,
                    textPaintRight, RIGHT_TANGENT_LABEL_FONT_SIZE, state.zoomSliderPosition)
                textRenderer.draw(canvas, "Tangent Line", ghostCuePosInUnitLogicalSpace, getAngle(ghostCuePosInUnitLogicalSpace, leftEndPoint),
                    labelDistance, LEFT_TANGENT_LABEL_ANGLE_OFFSET, LEFT_TANGENT_LABEL_ROTATION,
                    textPaintLeft, LEFT_TANGENT_LABEL_FONT_SIZE, state.zoomSliderPosition)
            }
        }
    }

    private fun drawProtractorAimingAndAngleLines(canvas: Canvas, ghostCuePosInUnitLogicalSpace: PointF, paints: PaintCache, state: OverlayState): Boolean {
        val lineLength = 2000f
        val originOfAllLines = ghostCuePosInUnitLogicalSpace
        val targetBallLogicalOrigin = PointF(0f, 0f)
        var lineDrawn = false

        val aimDirX = targetBallLogicalOrigin.x - originOfAllLines.x
        val aimDirY = targetBallLogicalOrigin.y - originOfAllLines.y
        val mag = sqrt(aimDirX * aimDirX + aimDirY * aimDirY)

        if (mag > state.protractorUnit.radius * 0.1f) {
            val nX = aimDirX / mag; val nY = aimDirY / mag
            val endPointThroughTarget = PointF(originOfAllLines.x + nX * lineLength, originOfAllLines.y + nY * lineLength)
            canvas.drawLine(originOfAllLines.x, originOfAllLines.y, endPointThroughTarget.x, endPointThroughTarget.y, paints.aimingLinePaint)
            lineDrawn = true
            if (state.areHelpersVisible) {
                val textPaint = Paint(paints.lineTextPaint).apply { color = paints.aimingLinePaint.color }
                val labelDistance = state.protractorUnit.radius * AIMING_LINE_LABEL_DISTANCE_FACTOR
                textRenderer.draw(canvas, "Aiming Line", originOfAllLines, getAngle(originOfAllLines, targetBallLogicalOrigin),
                    labelDistance, AIMING_LINE_LABEL_ANGLE_OFFSET, AIMING_LINE_LABEL_ROTATION,
                    textPaint, AIMING_LINE_LABEL_FONT_SIZE, state.zoomSliderPosition)
            }
        }

        PROTRACTOR_ANGLES.forEach { angle ->
            if (angle == 0f) return@forEach
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat(); val eY = (lineLength * cos(r)).toFloat()

            canvas.drawLine(originOfAllLines.x, originOfAllLines.y, originOfAllLines.x + eX, originOfAllLines.y + eY, paints.protractorLinePaint)
            canvas.drawLine(originOfAllLines.x, originOfAllLines.y, originOfAllLines.x - eX, originOfAllLines.y - eY, paints.protractorLinePaint)
            canvas.drawLine(originOfAllLines.x, originOfAllLines.y, originOfAllLines.x - eX, originOfAllLines.y + eY, paints.protractorLinePaint)
            canvas.drawLine(originOfAllLines.x, originOfAllLines.y, originOfAllLines.x + eX, originOfAllLines.y - eY, paints.protractorLinePaint)

            lineDrawn = true
            if (state.areHelpersVisible) {
                val textPaint = Paint(paints.lineTextPaint).apply { color = paints.protractorLinePaint.color }
                val labelDistance = state.protractorUnit.radius * PROTRACTOR_LABEL_DISTANCE_FACTOR

                textRenderer.draw(canvas, "${angle.toInt()}°", originOfAllLines, getAngle(originOfAllLines, PointF(originOfAllLines.x + eX, originOfAllLines.y + eY)),
                    labelDistance, PROTRACTOR_LABEL_ANGLE_OFFSET, PROTRACTOR_LABEL_ROTATION,
                    textPaint, PROTRACTOR_LABEL_FONT_SIZE, state.zoomSliderPosition)
                textRenderer.draw(canvas, "${angle.toInt()}°", originOfAllLines, getAngle(originOfAllLines, PointF(originOfAllLines.x - eX, originOfAllLines.y - eY)),
                    labelDistance, PROTRACTOR_LABEL_ANGLE_OFFSET, PROTRACTOR_LABEL_ROTATION,
                    textPaint, PROTRACTOR_LABEL_FONT_SIZE, state.zoomSliderPosition)
                textRenderer.draw(canvas, "${angle.toInt()}°", originOfAllLines, getAngle(originOfAllLines, PointF(originOfAllLines.x - eX, originOfAllLines.y + eY)),
                    labelDistance, PROTRACTOR_LABEL_ANGLE_OFFSET, PROTRACTOR_LABEL_ROTATION,
                    textPaint, PROTRACTOR_LABEL_FONT_SIZE, state.zoomSliderPosition)
                textRenderer.draw(canvas, "${angle.toInt()}°", originOfAllLines, getAngle(originOfAllLines, PointF(originOfAllLines.x + eX, originOfAllLines.y - eY)),
                    labelDistance, PROTRACTOR_LABEL_ANGLE_OFFSET, PROTRACTOR_LABEL_ROTATION,
                    textPaint, PROTRACTOR_LABEL_FONT_SIZE, state.zoomSliderPosition)
            }
        }
        return lineDrawn
    }
}