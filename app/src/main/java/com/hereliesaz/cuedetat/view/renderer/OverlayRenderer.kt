
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class OverlayRenderer {

    private val paints = PaintCache()
    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)

    fun draw(canvas: Canvas, state: OverlayState) {
        if (state.viewWidth == 0) return
        paints.updateColors(state.dynamicColorScheme)

        canvas.save()
        canvas.concat(state.pitchMatrix)

        drawShotLine(canvas, state, paints)
        if (state.isActualCueBallVisible) {
            drawActualCueBallBase(canvas, state, paints)
        }
        drawProtractorUnit(canvas, state, paints)

        canvas.restore()

        if (state.isActualCueBallVisible) {
            drawActualCueBallGhost(canvas, state, paints)
        }
        drawGhostBalls(canvas, state, paints)
    }

    private fun drawProtractorUnit(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        canvas.save()
        canvas.translate(state.targetCircleCenter.x, state.targetCircleCenter.y)
        canvas.rotate(state.rotationAngle)

        // Target Ball (at the local origin 0,0)
        canvas.drawCircle(0f, 0f, state.logicalRadius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.logicalRadius / 5f, paints.targetCenterMarkPaint)

        // Protractor Cue Ball (relative to the Target Ball)
        val cuePaint =
            if (state.isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint
        val distance = 2 * state.logicalRadius
        val cueX = 0f
        val cueY = distance
        canvas.drawCircle(cueX, cueY, state.logicalRadius, cuePaint)
        canvas.drawCircle(cueX, cueY, state.logicalRadius / 5f, paints.cueCenterMarkPaint)

        // Tangent Lines
        val dx = 0 - cueX
        val dy = 0 - cueY
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > 0.001f) {
            val extend = max(state.viewWidth, state.viewHeight) * 1.5f
            val dX = -dy / mag
            val dY = dx / mag
            val rightPaint =
                if (state.isImpossibleShot || state.rotationAngle <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            val leftPaint =
                if (state.isImpossibleShot || state.rotationAngle > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            canvas.drawLine(cueX, cueY, cueX + dX * extend, cueY + dY * extend, rightPaint)
            canvas.drawLine(cueX, cueY, cueX - dX * extend, cueY - dY * extend, leftPaint)
        }

        // Protractor & Aiming Line
        val lineLength = max(state.viewWidth, state.viewHeight) * 2f
        PROTRACTOR_ANGLES.forEach { angle ->
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat()
            val eY = (lineLength * cos(r)).toFloat()
            if (angle == 0f) {
                canvas.drawLine(0f, 0f, eX, eY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -eX, -eY, paints.aimingLinePaint)
            } else {
                canvas.drawLine(0f, 0f, eX, eY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -eX, -eY, paints.protractorLinePaint)
                val nR = Math.toRadians(-angle.toDouble())
                val nEX = (lineLength * sin(nR)).toFloat()
                val nEY = (lineLength * cos(nR)).toFloat()
                canvas.drawLine(0f, 0f, nEX, nEY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -nEX, -nEY, paints.protractorLinePaint)
            }
        }
        canvas.restore()
    }

    private fun drawShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val startPoint: PointF = if (state.isActualCueBallVisible) {
            state.logicalActualCueBallPosition
        } else {
            if (!state.hasInverseMatrix) return
            val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
            val logicalAnchorArray = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        }

        val throughPoint = state.cueCircleCenter
        val dirX = throughPoint.x - startPoint.x
        val dirY = throughPoint.y - startPoint.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = max(state.viewWidth, state.viewHeight) * 5f
            val ndx = dirX / mag
            val ndy = dirY / mag
            val paint =
                if (state.isImpossibleShot) paints.warningPaintRed3 else paints.shotLinePaint
            canvas.drawLine(
                startPoint.x,
                startPoint.y,
                startPoint.x + ndx * extendFactor,
                startPoint.y + ndy * extendFactor,
                paint
            )
        }
    }

    private fun drawActualCueBallBase(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        canvas.drawCircle(
            state.logicalActualCueBallPosition.x,
            state.logicalActualCueBallPosition.y,
            state.logicalRadius,
            paints.actualCueBallBasePaint
        )
        canvas.drawCircle(
            state.logicalActualCueBallPosition.x,
            state.logicalActualCueBallPosition.y,
            state.logicalRadius / 5f,
            paints.actualCueBallCenterMarkPaint
        )
    }

    private fun drawActualCueBallGhost(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val radiusInfo = getPerspectiveRadiusAndLift(state.logicalActualCueBallPosition, state)
        val screenBasePos = mapPoint(state.logicalActualCueBallPosition, state.pitchMatrix)
        val ghostCenterY = screenBasePos.y - radiusInfo.lift
        canvas.drawCircle(
            screenBasePos.x,
            ghostCenterY,
            radiusInfo.radius,
            paints.actualCueBallGhostPaint
        )
    }

    private fun drawGhostBalls(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val targetRadiusInfo = getPerspectiveRadiusAndLift(state.targetCircleCenter, state)
        val cueRadiusInfo = getPerspectiveRadiusAndLift(state.cueCircleCenter, state)

        val pTGC = mapPoint(state.targetCircleCenter, state.pitchMatrix)
        val pCGC = mapPoint(state.cueCircleCenter, state.pitchMatrix)

        val targetGhostCenterY = pTGC.y - targetRadiusInfo.lift
        val cueGhostCenterY = pCGC.y - cueRadiusInfo.lift

        canvas.drawCircle(
            pTGC.x,
            targetGhostCenterY,
            targetRadiusInfo.radius,
            paints.targetGhostBallOutlinePaint
        )
        val cueGhostPaint =
            if (state.isImpossibleShot) paints.warningPaintRed2 else paints.ghostCueOutlinePaint
        canvas.drawCircle(pCGC.x, cueGhostCenterY, cueRadiusInfo.radius, cueGhostPaint)
    }

    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        state: OverlayState
    ): PerspectiveRadiusInfo {
        if (!state.hasInverseMatrix) return PerspectiveRadiusInfo(state.logicalRadius, 0f)
        val screenCenter = mapPoint(logicalCenter, state.pitchMatrix)
        val logicalEdge = PointF(logicalCenter.x + state.logicalRadius, logicalCenter.y)
        val screenEdge = mapPoint(logicalEdge, state.pitchMatrix)
        val radius = distance(screenCenter, screenEdge)
        val lift = radius * abs(sin(Math.toRadians(state.pitchAngle.toDouble()))).toFloat()
        return PerspectiveRadiusInfo(radius, lift)
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

    fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
