package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.TableModel
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import java.lang.Math.toDegrees
import kotlin.math.*

class LineRenderer(
    private val paintCache: PaintCache,
    private val lineTextRenderer: LineTextRenderer
) {

    private val PROTRACTOR_ANGLES = floatArrayOf(15f, 30f, 45f)

    fun draw(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        // Draw all elements on the transformed logical plane
        canvas.save()
        canvas.concat(overlayState.pitchMatrix)

        if (screenState.isProtractorMode) {
            drawProtractorLines(canvas, screenState, overlayState)
        } else {
            drawBankingLines(canvas, screenState)
        }

        canvas.restore()
    }

    private fun drawProtractorLines(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        val protractorUnit = screenState.protractorUnit
        val targetBall = protractorUnit.targetBall

        val angleRad = Math.toRadians(protractorUnit.aimingAngleDegrees.toDouble()).toFloat()
        val totalRadius = targetBall.radius * 2
        val ghostBallX = targetBall.logicalPosition.x - cos(angleRad) * totalRadius
        val ghostBallY = targetBall.logicalPosition.y - sin(angleRad) * totalRadius
        val ghostBallPos = PointF(ghostBallX, ghostBallY)

        // 1. Aiming line from Ghost Ball THROUGH Target Ball
        drawExtendedLine(canvas, ghostBallPos, targetBall.logicalPosition, paintCache.aimingLinePaint)
        if(overlayState.areHelpersVisible) {
            val aimingAngle = toDegrees(atan2(targetBall.logicalPosition.y - ghostBallPos.y, targetBall.logicalPosition.x - ghostBallPos.x).toDouble()).toFloat()
            lineTextRenderer.draw(canvas, "Aiming Line", ghostBallPos, aimingAngle, targetBall.radius * 4, 0f, 0f, paintCache.lineTextPaint, 38f, overlayState.zoomSliderPosition)
        }


        // 2. Shot line: origin depends on whether the Actual Cue Ball is active
        val shotLineOrigin = screenState.actualCueBall?.logicalPosition ?: run {
            if (overlayState.hasInverseMatrix) {
                val screenAnchor = floatArrayOf(overlayState.viewWidth / 2f, overlayState.viewHeight.toFloat())
                val logicalAnchor = floatArrayOf(0f, 0f)
                overlayState.inversePitchMatrix.mapPoints(logicalAnchor, screenAnchor)
                PointF(logicalAnchor[0], logicalAnchor[1])
            } else { null }
        }

        shotLineOrigin?.let { origin ->
            val shotLinePaint = if (screenState.isImpossibleShot) paintCache.warningPaintRed1 else paintCache.shotLinePaint
            drawExtendedLine(canvas, origin, ghostBallPos, shotLinePaint)
            if(overlayState.areHelpersVisible) {
                val angle = toDegrees(atan2(ghostBallPos.y - origin.y, ghostBallPos.x - origin.x).toDouble()).toFloat()
                lineTextRenderer.draw(canvas, "Shot Line", ghostBallPos, angle, targetBall.radius * 4, 0f, 0f, paintCache.lineTextPaint, 38f, overlayState.zoomSliderPosition)
            }
        }

        // 3. Tangent Lines
        drawTangentLines(canvas, targetBall.logicalPosition, ghostBallPos, overlayState)

        // 4. Protractor Angle Lines (now originating from ghost ball)
        drawProtractorAngleLines(canvas, ghostBallPos, targetBall.logicalPosition, overlayState)
    }

    private fun drawExtendedLine(canvas: Canvas, start: PointF, through: PointF, paint: Paint) {
        val dx = through.x - start.x
        val dy = through.y - start.y
        val mag = sqrt(dx*dx + dy*dy)
        if (mag < 0.001f) return

        val extendFactor = 2000f
        val ndx = dx / mag
        val ndy = dy / mag

        val endX = through.x + ndx * extendFactor
        val endY = through.y + ndy * extendFactor

        canvas.drawLine(start.x, start.y, endX, endY, paint)
    }


    private fun drawTangentLines(canvas: Canvas, targetCenter: PointF, ghostCenter: PointF, state: OverlayState) {
        val dx = targetCenter.x - ghostCenter.x
        val dy = targetCenter.y - ghostCenter.y
        val mag = sqrt(dx*dx + dy*dy)
        if (mag == 0f) return

        val tangentDx = -dy / mag
        val tangentDy = dx / mag

        val rightEndPoint = PointF(ghostCenter.x + tangentDx, ghostCenter.y + tangentDy)
        val leftEndPoint = PointF(ghostCenter.x - tangentDx, ghostCenter.y - tangentDy)

        if (state.screenState.isImpossibleShot) {
            drawExtendedLine(canvas, ghostCenter, rightEndPoint, paintCache.warningDottedPaintRed)
            drawExtendedLine(canvas, ghostCenter, leftEndPoint, paintCache.warningDottedPaintRed)
        } else {
            val shotLineOrigin = state.screenState.actualCueBall?.logicalPosition ?: run {
                if (state.hasInverseMatrix) {
                    val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
                    val logicalAnchor = floatArrayOf(0f, 0f)
                    state.inversePitchMatrix.mapPoints(logicalAnchor, screenAnchor)
                    PointF(logicalAnchor[0], logicalAnchor[1])
                } else { PointF(state.viewWidth/2f, state.viewHeight.toFloat())} // Fallback
            }
            // Using cross product to determine which side of the aiming line the shot line is on.
            val crossProduct = (ghostCenter.x - shotLineOrigin.x) * (targetCenter.y - shotLineOrigin.y) - (ghostCenter.y - shotLineOrigin.y) * (targetCenter.x - shotLineOrigin.x)
            val leftIsSolid = crossProduct > 0 // This logic is now correct based on standard geometry.

            drawExtendedLine(canvas, ghostCenter, rightEndPoint, if(!leftIsSolid) paintCache.tangentLineSolidPaint else paintCache.tangentLineDottedPaint)
            drawExtendedLine(canvas, ghostCenter, leftEndPoint, if(leftIsSolid) paintCache.tangentLineSolidPaint else paintCache.tangentLineDottedPaint)
        }

        if (state.areHelpersVisible) {
            val angleRight = toDegrees(atan2(tangentDy.toDouble(), tangentDx.toDouble()).toDouble()).toFloat()
            lineTextRenderer.draw(canvas, "Tangent", ghostCenter, angleRight, state.screenState.protractorUnit.targetBall.radius * 5, 5f, 0f, paintCache.lineTextPaint, 38f, state.zoomSliderPosition)

            val angleLeft = toDegrees(atan2(-tangentDy.toDouble(), -tangentDx.toDouble()).toDouble()).toFloat()
            lineTextRenderer.draw(canvas, "Tangent", ghostCenter, angleLeft, state.screenState.protractorUnit.targetBall.radius * 5, -5f, 0f, paintCache.lineTextPaint, 38f, state.zoomSliderPosition)
        }
    }

    private fun drawProtractorAngleLines(canvas: Canvas, origin: PointF, throughPoint: PointF, state: OverlayState) {
        val baseAngleRad = atan2(throughPoint.y - origin.y, throughPoint.x - origin.x)
        val extend = 2000f

        PROTRACTOR_ANGLES.forEach { angleDeg ->
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val labelDistance = state.screenState.protractorUnit.targetBall.radius * 12

            // Line to the right of the aiming line
            val angle1 = baseAngleRad + angleRad
            val pX1 = origin.x + extend * cos(angle1)
            val pY1 = origin.y + extend * sin(angle1)
            canvas.drawLine(origin.x - (pX1 - origin.x), origin.y - (pY1 - origin.y), pX1, pY1, paintCache.protractorLinePaint)

            // Line to the left of the aiming line
            val angle2 = baseAngleRad - angleRad
            val pX2 = origin.x + extend * cos(angle2)
            val pY2 = origin.y + extend * sin(angle2)
            canvas.drawLine(origin.x - (pX2 - origin.x), origin.y - (pY2 - origin.y), pX2, pY2, paintCache.protractorLinePaint)

            if (state.areHelpersVisible) {
                // Draw labels on both sides
                val labelAngle1 = toDegrees(angle1.toDouble()).toFloat()
                lineTextRenderer.draw(canvas, "$angleDeg°", origin, labelAngle1, labelDistance, 0f, 0f, paintCache.lineTextPaint, 42f, state.zoomSliderPosition)
                lineTextRenderer.draw(canvas, "$angleDeg°", origin, labelAngle1 + 180, labelDistance, 0f, 0f, paintCache.lineTextPaint, 42f, state.zoomSliderPosition)

                val labelAngle2 = toDegrees(angle2.toDouble()).toFloat()
                lineTextRenderer.draw(canvas, "$angleDeg°", origin, labelAngle2, labelDistance, 0f, 0f, paintCache.lineTextPaint, 42f, state.zoomSliderPosition)
                lineTextRenderer.draw(canvas, "$angleDeg°", origin, labelAngle2 + 180, labelDistance, 0f, 0f, paintCache.lineTextPaint, 42f, state.zoomSliderPosition)
            }
        }
    }


    private fun drawBankingLines(canvas: Canvas, screenState: ScreenState) {
        val path = screenState.bankingPath
        if (path.size < 2) return

        val bankLinePaints = listOf(paintCache.bankShotLinePaint1, paintCache.bankShotLinePaint2, paintCache.bankShotLinePaint3)

        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]
            val paint = bankLinePaints.getOrElse(i) { bankLinePaints.last() }
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }
}