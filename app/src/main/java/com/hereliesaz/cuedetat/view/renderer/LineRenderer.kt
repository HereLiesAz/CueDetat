// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
// import android.graphics.Matrix // Not strictly needed if local pos is correct
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
    // ... (constants as before) ...
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
    private data class IntersectionResult(
        val point: PointF?,
        val railHit: Rail,
        val distanceSq: Float
    )

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
                    drawBankingShotLinesWithReflection(
                        canvas,
                        bankingBall,
                        aimTarget,
                        state,
                        paints
                    )
                }
            }
            return
        }

        // --- Protractor Mode Lines ---
        // This line is drawn in absolute logical coordinates on the main (pitchMatrix-transformed) canvas
        val protractorShotLineDrawn = drawProtractorShotLine(canvas, state, paints)

        // For lines relative to the ProtractorUnit's orientation (tangents, aiming line, angles)
        // we transform the canvas to the ProtractorUnit's local space.
        canvas.save()
        canvas.translate(
            state.protractorUnit.center.x,
            state.protractorUnit.center.y
        ) // Move origin to Target Ball
        canvas.rotate(state.protractorUnit.rotationDegrees) // Rotate canvas by unit's rotation

        // Now, in this transformed space, the Target Ball is at (0,0).
        // We need the Ghost Cue Ball's position relative to this (0,0) in its UNROTATED orientation
        // because the canvas rotation handles the actual angular placement.
        val distanceBetweenProtractorCenters = 2 * state.protractorUnit.radius
        // Assuming 0-degree rotation in ProtractorUnit means GhostCue is along local +Y of TargetCue:
        PointF(0f, distanceBetweenProtractorCenters)
        // (If 0-deg means along +X, it would be (distanceBetweenProtractorCenters, 0f) )
        // Check ProtractorUnit.protractorCueBallCenter logic:
        // x = center.x - (distance * sin(angleRad)) -> for angleRad=0, x = center.x
        // y = center.y + (distance * cos(angleRad)) -> for angleRad=0, y = center.y + distance
        // So, relative to center, the 0-degree rotated point is (0, distance). This is correct.

        drawTangentLines(
            canvas,
            unrotatedProtractorGhostCueLocalPos,
            paints,
            state
        ) // Tangents from this local pos
        val protractorAimingLineDrawn = drawProtractorAimingAndAngleLines(
            canvas,
            unrotatedProtractorGhostCueLocalPos,
            paints,
            state
        ) // Aiming line uses this local pos

        canvas.restore()

        // Logging (no changes needed here, but ensure variable names in log match if changed)
        val pShotLog =
            "ProtShotLine: drawn=$protractorShotLineDrawn, actualCue=${state.actualCueBall != null}, hasInverse=${state.hasInverseMatrix}, radius=${state.protractorUnit.radius}"
        if (pShotLog != lastProtractorShotLineLog) {
            Log.d("LineRenderer", pShotLog); lastProtractorShotLineLog = pShotLog
        }
        val pAimLog =
            "ProtAimLine: drawn=$protractorAimingLineDrawn, radius=${state.protractorUnit.radius}, ghostLocalPos=(${unrotatedProtractorGhostCueLocalPos.x},${unrotatedProtractorGhostCueLocalPos.y})"
        if (pAimLog != lastProtractorAimingLineLog) {
            Log.d("LineRenderer", pAimLog); lastProtractorAimingLineLog = pAimLog
        }
    }

    private fun getAngle(from: PointF, to: PointF): Float { /* ... no change ... */
        return Math.toDegrees(atan2(to.y - from.y, to.x - from.x).toDouble()).toFloat()
    }

    private fun drawBankingShotLinesWithReflection( /* ... (no change from previous full file version) ... */
                                                    canvas: Canvas,
                                                    bankingBall: ActualCueBall,
                                                    initialAimTarget: PointF,
                                                    state: OverlayState,
                                                    paints: PaintCache
    ) {
        if (bankingBall.radius <= 0) return
        var startPoint = bankingBall.center
        var currentAimVector =
            PointF(initialAimTarget.x - startPoint.x, initialAimTarget.y - startPoint.y)
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
        val bankLinePaints =
            listOf(paints.bankShotLinePaint1, paints.bankShotLinePaint2, paints.bankShotLinePaint3)

        for (i in 0..2) {
            val farOffTarget = PointF(
                startPoint.x + currentAimVector.x * 1000,
                startPoint.y + currentAimVector.y * 1000
            )
            val hitResult =
                findClosestRailIntersection(startPoint, farOffTarget, tableBounds, lastHitRail)
            val currentSegmentPaint = bankLinePaints.getOrElse(i) { bankLinePaints.last() }

            if (hitResult.point != null) {
                canvas.drawLine(
                    startPoint.x,
                    startPoint.y,
                    hitResult.point.x,
                    hitResult.point.y,
                    currentSegmentPaint
                )
                if (state.areHelpersVisible) {
                    val midSegment = PointF(
                        (startPoint.x + hitResult.point.x) / 2,
                        (startPoint.y + hitResult.point.y) / 2
                    )
                    val labelTextPaint =
                        Paint(paints.lineTextPaint).apply { color = currentSegmentPaint.color }
                    textRenderer.draw(
                        canvas, "Bank ${i + 1}", midSegment, getAngle(startPoint, hitResult.point),
                        0f, BANKING_SHOT_LINE_LABEL_ANGLE_OFFSET, BANKING_SHOT_LINE_LABEL_ROTATION,
                        labelTextPaint, BANKING_SHOT_LINE_LABEL_FONT_SIZE, state.zoomSliderPosition
                    )
                }
                startPoint = hitResult.point
                currentAimVector = reflectVector(currentAimVector, hitResult.railHit)
                lastHitRail = hitResult.railHit
                if (i == 2) {
                    val finalFarOffTarget = PointF(
                        startPoint.x + currentAimVector.x * 1000,
                        startPoint.y + currentAimVector.y * 1000
                    )
                    canvas.drawLine(
                        startPoint.x,
                        startPoint.y,
                        finalFarOffTarget.x,
                        finalFarOffTarget.y,
                        currentSegmentPaint
                    )
                }
            } else {
                val finalFarOffTarget = PointF(
                    startPoint.x + currentAimVector.x * 1000,
                    startPoint.y + currentAimVector.y * 1000
                )
                canvas.drawLine(
                    startPoint.x,
                    startPoint.y,
                    finalFarOffTarget.x,
                    finalFarOffTarget.y,
                    currentSegmentPaint
                )
                if (i == 0 && state.areHelpersVisible) {
                    val labelTextPaint =
                        Paint(paints.lineTextPaint).apply { color = currentSegmentPaint.color }
                    textRenderer.draw(
                        canvas,
                        "Aim",
                        PointF(
                            (startPoint.x + finalFarOffTarget.x) / 2,
                            (startPoint.y + finalFarOffTarget.y) / 2
                        ),
                        getAngle(startPoint, finalFarOffTarget),
                        0f,
                        BANKING_SHOT_LINE_LABEL_ANGLE_OFFSET,
                        BANKING_SHOT_LINE_LABEL_ROTATION,
                        labelTextPaint,
                        BANKING_SHOT_LINE_LABEL_FONT_SIZE,
                        state.zoomSliderPosition
                    )
                }
                break
            }
        }
    }

    private fun findClosestRailIntersection(
        start: PointF,
        endRayTarget: PointF,
        bounds: RectF,
        ignoreRail: Rail
    ): IntersectionResult { /* ... (no change) ... */
        var closestIntersection: PointF? = null
        var railHit = Rail.NONE
        var minDistanceSq = Float.MAX_VALUE
        val candidates = mutableListOf<IntersectionResult>()
        if (ignoreRail != Rail.TOP) getLineSegmentRayIntersection(
            start,
            endRayTarget,
            PointF(bounds.left, bounds.top),
            PointF(bounds.right, bounds.top)
        )?.let { candidates.add(IntersectionResult(it, Rail.TOP, distanceSq(start, it))) }
        if (ignoreRail != Rail.BOTTOM) getLineSegmentRayIntersection(
            start,
            endRayTarget,
            PointF(bounds.left, bounds.bottom),
            PointF(bounds.right, bounds.bottom)
        )?.let { candidates.add(IntersectionResult(it, Rail.BOTTOM, distanceSq(start, it))) }
        if (ignoreRail != Rail.LEFT) getLineSegmentRayIntersection(
            start,
            endRayTarget,
            PointF(bounds.left, bounds.top),
            PointF(bounds.left, bounds.bottom)
        )?.let { candidates.add(IntersectionResult(it, Rail.LEFT, distanceSq(start, it))) }
        if (ignoreRail != Rail.RIGHT) getLineSegmentRayIntersection(
            start,
            endRayTarget,
            PointF(bounds.right, bounds.top),
            PointF(bounds.right, bounds.bottom)
        )?.let { candidates.add(IntersectionResult(it, Rail.RIGHT, distanceSq(start, it))) }
        for (candidate in candidates) {
            if (candidate.point != null) {
                val dotProduct =
                    (candidate.point.x - start.x) * (endRayTarget.x - start.x) + (candidate.point.y - start.y) * (endRayTarget.y - start.y)
                if (dotProduct >= -0.001f && candidate.distanceSq < minDistanceSq) {
                    minDistanceSq = candidate.distanceSq; closestIntersection =
                        candidate.point; railHit = candidate.railHit
                }
            }
        }
        return IntersectionResult(closestIntersection, railHit, minDistanceSq)
    }

    private fun getLineSegmentRayIntersection(
        rayOrigin: PointF,
        rayTarget: PointF,
        segP1: PointF,
        segP2: PointF
    ): PointF? { /* ... (no change) ... */
        val rDx = rayTarget.x - rayOrigin.x
        val rDy = rayTarget.y - rayOrigin.y
        val sDx = segP2.x - segP1.x
        val sDy = segP2.y - segP1.y
        val rMagSq = rDx * rDx + rDy * rDy
        val sMagSq = sDx * sDx + sDy * sDy
        if (rMagSq < 0.0001f || sMagSq < 0.0001f) return null
        val denominator = rDx * sDy - rDy * sDx
        if (kotlin.math.abs(denominator) < 0.0001f) return null
        val t = ((segP1.x - rayOrigin.x) * sDy - (segP1.y - rayOrigin.y) * sDx) / denominator
        val u = ((segP1.x - rayOrigin.x) * rDy - (segP1.y - rayOrigin.y) * rDx) / denominator
        if (t >= 0 && u >= 0 && u <= 1) return PointF(rayOrigin.x + t * rDx, rayOrigin.y + t * rDy)
        return null
    }

    private fun distanceSq(p1: PointF, p2: PointF): Float { /* ... (no change) ... */
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y; return dx * dx + dy * dy
    }

    private fun reflectVector(incident: PointF, rail: Rail): PointF { /* ... (no change) ... */
        return when (rail) {
            Rail.TOP, Rail.BOTTOM -> PointF(
                incident.x,
                -incident.y
            ); Rail.LEFT, Rail.RIGHT -> PointF(-incident.x, incident.y); Rail.NONE -> incident
        }
    }


    private fun drawProtractorShotLine(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache
    ): Boolean {
        // This line is drawn on the main canvas (already transformed by pitchMatrix)
        // It uses absolute logical coordinates.
        val startPoint: PointF = state.actualCueBall?.center ?: run {
            if (!state.hasInverseMatrix) return false
            val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
            val logicalAnchorArray = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        }
        val throughPoint = state.protractorUnit.protractorCueBallCenter // Absolute logical coord
        val paintToUse =
            if (state.isImpossibleShot && !state.isBankingMode) paints.warningPaintRed3 else paints.shotLinePaint
        val label = if (state.areHelpersVisible) "Shot Line" else null
        val textPaint = Paint(paints.lineTextPaint).apply { color = paintToUse.color }
        val lineAngle = getAngle(startPoint, throughPoint)
        val labelDistance = state.protractorUnit.radius * SHOT_LINE_LABEL_DISTANCE_FACTOR

        drawExtendedLineAndLabel(
            canvas, startPoint, throughPoint, paintToUse, label, textPaint,
            lineAngle, labelDistance, SHOT_LINE_LABEL_ANGLE_OFFSET, SHOT_LINE_LABEL_ROTATION,
            SHOT_LINE_LABEL_FONT_SIZE, state.zoomSliderPosition
        )
        return true
    }

    private fun drawExtendedLineAndLabel( /* ... (no changes from previous full file) ... */
                                          canvas: Canvas,
                                          start: PointF,
                                          through: PointF,
                                          linePaint: Paint,
                                          labelText: String?,
                                          textPaint: Paint,
                                          lineAngleForLabel: Float,
                                          labelDistanceFromThroughPoint: Float,
                                          labelAngleOffset: Float,
                                          labelRotation: Float,
                                          labelBaseFontSize: Float,
                                          zoomSliderPosition: Float
    ) {
        val dirX = through.x - start.x
        val dirY = through.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = 5000f
            val ndx = dirX / mag
            val ndy = dirY / mag
            canvas.drawLine(
                start.x,
                start.y,
                start.x + ndx * extendFactor,
                start.y + ndy * extendFactor,
                linePaint
            )
            if (labelText != null) {
                textRenderer.draw(
                    canvas,
                    labelText,
                    through,
                    lineAngleForLabel,
                    labelDistanceFromThroughPoint,
                    labelAngleOffset,
                    labelRotation,
                    textPaint,
                    labelBaseFontSize,
                    zoomSliderPosition
                )
            }
        }
    }

    private fun drawTangentLines(
        canvas: Canvas,
        ghostCuePosInUnitLocalSpace: PointF,
        paints: PaintCache,
        state: OverlayState
    ) { /* ... (no change from previous full file version, uses ghostCuePosInUnitLocalSpace correctly) ... */
        val dxToTarget = 0f - ghostCuePosInUnitLocalSpace.x
        val dyToTarget = 0f - ghostCuePosInUnitLocalSpace.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val extend = state.viewWidth.coerceAtLeast(state.viewHeight) * 1.5f
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget

            val rightPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            val leftPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint

            val rightEndPoint = PointF(
                ghostCuePosInUnitLocalSpace.x + tangentDx * extend,
                ghostCuePosInUnitLocalSpace.y + tangentDy * extend
            )
            val leftEndPoint = PointF(
                ghostCuePosInUnitLocalSpace.x - tangentDx * extend,
                ghostCuePosInUnitLocalSpace.y - tangentDy * extend
            )

            canvas.drawLine(
                ghostCuePosInUnitLocalSpace.x,
                ghostCuePosInUnitLocalSpace.y,
                rightEndPoint.x,
                rightEndPoint.y,
                rightPaint
            )
            canvas.drawLine(
                ghostCuePosInUnitLocalSpace.x,
                ghostCuePosInUnitLocalSpace.y,
                leftEndPoint.x,
                leftEndPoint.y,
                leftPaint
            )

            if (state.areHelpersVisible) {
                val textPaintRight = Paint(paints.lineTextPaint).apply { color = rightPaint.color }
                val textPaintLeft = Paint(paints.lineTextPaint).apply { color = leftPaint.color }
                val labelDistance =
                    state.protractorUnit.radius * RIGHT_TANGENT_LABEL_DISTANCE_FACTOR

                textRenderer.draw(
                    canvas,
                    "Tangent Line",
                    ghostCuePosInUnitLocalSpace,
                    getAngle(ghostCuePosInUnitLocalSpace, rightEndPoint),
                    labelDistance,
                    RIGHT_TANGENT_LABEL_ANGLE_OFFSET,
                    RIGHT_TANGENT_LABEL_ROTATION,
                    textPaintRight,
                    RIGHT_TANGENT_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
                textRenderer.draw(
                    canvas,
                    "Tangent Line",
                    ghostCuePosInUnitLocalSpace,
                    getAngle(ghostCuePosInUnitLocalSpace, leftEndPoint),
                    labelDistance,
                    LEFT_TANGENT_LABEL_ANGLE_OFFSET,
                    LEFT_TANGENT_LABEL_ROTATION,
                    textPaintLeft,
                    LEFT_TANGENT_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
            }
        }
    }

    private fun drawProtractorAimingAndAngleLines(
        canvas: Canvas,
        ghostCuePosInUnitLocalSpace: PointF,
        paints: PaintCache,
        state: OverlayState
    ): Boolean {
        // ghostCuePosInUnitLocalSpace IS the unrotated local position of the ghost cue ball
        // relative to the target ball (which is at (0,0) on this rotated canvas).
        val lineLength = 2000f
        val originTargetBall = PointF(0f, 0f)
        var lineDrawn = false

        val aimDirX = originTargetBall.x - ghostCuePosInUnitLocalSpace.x
        val aimDirY = originTargetBall.y - ghostCuePosInUnitLocalSpace.y
        val mag = sqrt(aimDirX * aimDirX + aimDirY * aimDirY)

        if (mag > state.protractorUnit.radius * 0.1f) {
            val nX = aimDirX / mag
            val nY = aimDirY / mag
            val endPointThroughTarget =
                PointF(originTargetBall.x + nX * lineLength, originTargetBall.y + nY * lineLength)
            canvas.drawLine(
                ghostCuePosInUnitLocalSpace.x,
                ghostCuePosInUnitLocalSpace.y,
                endPointThroughTarget.x,
                endPointThroughTarget.y,
                paints.aimingLinePaint
            )
            lineDrawn = true
            if (state.areHelpersVisible) {
                val textPaint =
                    Paint(paints.lineTextPaint).apply { color = paints.aimingLinePaint.color }
                val labelDistance = state.protractorUnit.radius * AIMING_LINE_LABEL_DISTANCE_FACTOR
                textRenderer.draw(
                    canvas,
                    "Aiming Line",
                    originTargetBall,
                    getAngle(ghostCuePosInUnitLocalSpace, originTargetBall),
                    labelDistance,
                    AIMING_LINE_LABEL_ANGLE_OFFSET,
                    AIMING_LINE_LABEL_ROTATION,
                    textPaint,
                    AIMING_LINE_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
            }
        }

        PROTRACTOR_ANGLES.forEach { angle -> /* ... (angle lines logic no change) ... */
            if (angle == 0f) return@forEach
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat()
            val eY = (lineLength * cos(r)).toFloat()
            val endPoints =
                listOf(PointF(eX, eY), PointF(-eX, -eY), PointF(-eX, eY), PointF(eX, -eY))
            endPoints.forEach { endPoint ->
                canvas.drawLine(
                    originTargetBall.x,
                    originTargetBall.y,
                    endPoint.x,
                    endPoint.y,
                    paints.protractorLinePaint
                )
                lineDrawn = true
                if (state.areHelpersVisible) {
                    val textPaint = Paint(paints.lineTextPaint).apply {
                        color = paints.protractorLinePaint.color
                    }
                    val labelDistance =
                        state.protractorUnit.radius * PROTRACTOR_LABEL_DISTANCE_FACTOR
                    val labelText = "${angle.toInt()}°"
                    textRenderer.draw(
                        canvas, labelText, originTargetBall, getAngle(originTargetBall, endPoint),
                        labelDistance, PROTRACTOR_LABEL_ANGLE_OFFSET, PROTRACTOR_LABEL_ROTATION,
                        textPaint, PROTRACTOR_LABEL_FONT_SIZE, state.zoomSliderPosition
                    )
                }
            }
        }
        return lineDrawn
    }
}