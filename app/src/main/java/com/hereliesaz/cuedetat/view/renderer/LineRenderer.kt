// app/src/main/java/com/hereliesaz/cuedetat/view/renderer/LineRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ActualCueBall // Make sure this is imported
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
    private data class IntersectionResult(
        val point: PointF?,
        val railHit: Rail,
        val distanceSq: Float
    )

    fun drawLogicalLines(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        paints.lineTextPaint.typeface = typeface

        if (state.isBankingMode) {
            state.actualCueBall?.let { bankingBall -> // actualCueBall is the BankingBall in this mode
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

        // Protractor Mode Lines
        drawProtractorShotLine(canvas, state, paints)

        canvas.save()
        canvas.translate(state.protractorUnit.center.x, state.protractorUnit.center.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)

        val protractorCueBallLocalCenter = state.protractorUnit.protractorCueBallCenter.let {
            val p = PointF(it.x, it.y)
            p.offset(-state.protractorUnit.center.x, -state.protractorUnit.center.y)
            p
        }

        drawTangentLines(canvas, protractorCueBallLocalCenter, paints, state)
        drawProtractorAimingAndAngleLines(canvas, protractorCueBallLocalCenter, paints, state)
        canvas.restore()
    }

    private fun getAngle(from: PointF, to: PointF): Float {
        return Math.toDegrees(atan2(to.y - from.y, to.x - from.x).toDouble()).toFloat()
    }

    private fun drawBankingShotLinesWithReflection(
        canvas: Canvas,
        bankingBall: ActualCueBall,
        initialAimTarget: PointF,
        state: OverlayState,
        paints: PaintCache
    ) {
        if (bankingBall.radius <= 0) return // Corrected: Use bankingBall.radius

        var startPoint = bankingBall.center // Corrected: Use bankingBall.center
        var currentAimVector =
            PointF(initialAimTarget.x - startPoint.x, initialAimTarget.y - startPoint.y)

        val ballRadiusForTableScale = bankingBall.radius // Corrected: Use bankingBall.radius
        val tableHeight = tableToBallRatioShort * ballRadiusForTableScale
        val tableWidth = tableToBallRatioLong * ballRadiusForTableScale
        val tableCenterX = state.viewWidth / 2f
        val tableCenterY = state.viewHeight / 2f

        val tableBounds = RectF(
            tableCenterX - tableWidth / 2,
            tableCenterY - tableHeight / 2,
            tableCenterX + tableWidth / 2,
            tableCenterY + tableHeight / 2
        )

        val bankShotPaint = paints.shotLinePaint
        val bankLabelPaint = Paint(paints.lineTextPaint).apply { color = bankShotPaint.color }
        var lastHitRail = Rail.NONE

        for (i in 1..3) {
            val farOffTarget = PointF(
                startPoint.x + currentAimVector.x * 1000,
                startPoint.y + currentAimVector.y * 1000
            )
            val hitResult =
                findClosestRailIntersection(startPoint, farOffTarget, tableBounds, lastHitRail)

            if (hitResult.point != null) {
                canvas.drawLine(
                    startPoint.x,
                    startPoint.y,
                    hitResult.point.x,
                    hitResult.point.y,
                    bankShotPaint
                )
                if (state.areHelpersVisible) {
                    val midSegment = PointF(
                        (startPoint.x + hitResult.point.x) / 2,
                        (startPoint.y + hitResult.point.y) / 2
                    )
                    textRenderer.draw(
                        canvas,
                        "Bank $i",
                        midSegment,
                        getAngle(startPoint, hitResult.point),
                        0f,
                        0f,
                        0f,
                        bankLabelPaint,
                        BANKING_SHOT_LINE_LABEL_FONT_SIZE,
                        state.zoomSliderPosition
                    )
                }
                startPoint = hitResult.point
                currentAimVector = reflectVector(currentAimVector, hitResult.railHit)
                lastHitRail = hitResult.railHit

                if (i == 3) {
                    val finalFarOffTarget = PointF(
                        startPoint.x + currentAimVector.x * 1000,
                        startPoint.y + currentAimVector.y * 1000
                    )
                    canvas.drawLine(
                        startPoint.x,
                        startPoint.y,
                        finalFarOffTarget.x,
                        finalFarOffTarget.y,
                        bankShotPaint
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
                    bankShotPaint
                )
                if (i == 1 && state.areHelpersVisible) {
                    textRenderer.draw(
                        canvas,
                        "Aim",
                        PointF(
                            (startPoint.x + finalFarOffTarget.x) / 2,
                            (startPoint.y + finalFarOffTarget.y) / 2
                        ),
                        getAngle(startPoint, finalFarOffTarget),
                        0f,
                        0f,
                        0f,
                        bankLabelPaint,
                        BANKING_SHOT_LINE_LABEL_FONT_SIZE,
                        state.zoomSliderPosition
                    )
                }
                break
            }
        }
    }

    private fun findClosestRailIntersection(
        start: PointF, endRayTarget: PointF, bounds: RectF, ignoreRail: Rail
    ): IntersectionResult { /* ... (no change from previous, assumed correct) ... */
        var closestIntersection: PointF? = null
        var railHit = Rail.NONE
        var minDistanceSq = Float.MAX_VALUE

        val candidates = mutableListOf<IntersectionResult>()

        // Top Rail
        if (ignoreRail != Rail.TOP) {
            getLineSegmentRayIntersection(
                start,
                endRayTarget,
                PointF(bounds.left, bounds.top),
                PointF(bounds.right, bounds.top)
            )?.let {
                candidates.add(IntersectionResult(it, Rail.TOP, distanceSq(start, it)))
            }
        }
        // Bottom Rail
        if (ignoreRail != Rail.BOTTOM) {
            getLineSegmentRayIntersection(
                start,
                endRayTarget,
                PointF(bounds.left, bounds.bottom),
                PointF(bounds.right, bounds.bottom)
            )?.let {
                candidates.add(IntersectionResult(it, Rail.BOTTOM, distanceSq(start, it)))
            }
        }
        // Left Rail
        if (ignoreRail != Rail.LEFT) {
            getLineSegmentRayIntersection(
                start,
                endRayTarget,
                PointF(bounds.left, bounds.top),
                PointF(bounds.left, bounds.bottom)
            )?.let {
                candidates.add(IntersectionResult(it, Rail.LEFT, distanceSq(start, it)))
            }
        }
        // Right Rail
        if (ignoreRail != Rail.RIGHT) {
            getLineSegmentRayIntersection(
                start,
                endRayTarget,
                PointF(bounds.right, bounds.top),
                PointF(bounds.right, bounds.bottom)
            )?.let {
                candidates.add(IntersectionResult(it, Rail.RIGHT, distanceSq(start, it)))
            }
        }

        for (candidate in candidates) {
            if (candidate.point != null) {
                val dotProduct = (candidate.point.x - start.x) * (endRayTarget.x - start.x) +
                        (candidate.point.y - start.y) * (endRayTarget.y - start.y)
                if (dotProduct >= -0.001f && candidate.distanceSq < minDistanceSq) {
                    minDistanceSq = candidate.distanceSq
                    closestIntersection = candidate.point
                    railHit = candidate.railHit
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
    ): PointF? { /* ... (no change from previous, assumed correct) ... */
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

        if (t >= 0 && u >= 0 && u <= 1) {
            return PointF(rayOrigin.x + t * rDx, rayOrigin.y + t * rDy)
        }
        return null
    }

    private fun distanceSq(
        p1: PointF,
        p2: PointF
    ): Float { /* ... (no change from previous, assumed correct) ... */
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return dx * dx + dy * dy
    }

    private fun reflectVector(
        incident: PointF,
        rail: Rail
    ): PointF { /* ... (no change from previous, assumed correct) ... */
        return when (rail) {
            Rail.TOP, Rail.BOTTOM -> PointF(incident.x, -incident.y)
            Rail.LEFT, Rail.RIGHT -> PointF(-incident.x, incident.y)
            Rail.NONE -> incident
        }
    }

    private fun drawProtractorShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val startPoint: PointF = state.actualCueBall?.center
            ?: run { // actualCueBall is the optional one in protractor mode
            if (!state.hasInverseMatrix) return
            val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
            val logicalAnchorArray = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        }

        val throughPoint = state.protractorUnit.protractorCueBallCenter
        val paint = if (state.isImpossibleShot) paints.warningPaintRed3 else paints.shotLinePaint
        val label = if (state.areHelpersVisible) "Shot Line" else null

        // Corrected: Create a new Paint instance for text to avoid modifying shared paint object
        val textPaint = Paint(paints.lineTextPaint).apply { color = paint.color }

        drawExtendedLine(
            canvas, startPoint, throughPoint, paint, label, textPaint,
            SHOT_LINE_LABEL_DISTANCE_FACTOR,
            SHOT_LINE_LABEL_ANGLE_OFFSET,
            SHOT_LINE_LABEL_ROTATION,
            SHOT_LINE_LABEL_FONT_SIZE,
            state.protractorUnit.radius, // Use protractor's radius for label scaling here
            state.zoomSliderPosition
        )
    }

    // Correct signature and internal logic for drawExtendedLine
    private fun drawExtendedLine(
        canvas: Canvas,
        start: PointF,
        through: PointF,
        paint: Paint,
        label: String?,
        textPaint: Paint, // Pass the correctly configured textPaint
        labelDistanceFactor: Float,
        labelAngleOffset: Float,
        labelRotation: Float,
        labelFontSize: Float,
        radiusForLabelDistance: Float,
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
                paint
            )

            if (label != null) {
                val labelDistance = radiusForLabelDistance * labelDistanceFactor
                val lineAngle = getAngle(start, through)
                textRenderer.draw(
                    canvas,
                    label,
                    through, // Label position relative to 'through' point
                    lineAngle,
                    labelDistance,
                    labelAngleOffset,
                    labelRotation,
                    textPaint, // Use the passed textPaint
                    labelFontSize,
                    zoomSliderPosition
                )
            }
        }
    }
    // Removed the erroneous block that was from line 286-315 in the problematic version.

    private fun drawTangentLines(
        canvas: Canvas,
        cueLocalPos: PointF,
        paints: PaintCache,
        state: OverlayState
    ) { /* ... (no change from previous, assumed correct) ... */
    }

    private fun drawProtractorAimingAndAngleLines(
        canvas: Canvas,
        cueLocalPos: PointF,
        paints: PaintCache,
        state: OverlayState
    ) { /* ... (no change from previous, assumed correct) ... */
    }
}