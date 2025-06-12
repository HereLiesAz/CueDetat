package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.*

class OverlayRenderer {

    private val paints = PaintCache()
    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val baseGhostBallTextSize = 42f
    private val minGhostBallTextSize = 15f
    private val maxGhostBallTextSize = 80f

    fun draw(canvas: Canvas, state: OverlayState) {
        if (state.protractorUnit.center.x == 0f) return
        paints.updateColors(state.dynamicColorScheme)

        canvas.save()
        canvas.concat(state.pitchMatrix)

        drawShotLine(canvas, state, paints)
        state.actualCueBall?.let { drawActualCueBallBase(canvas, it, paints) }
        drawProtractorUnit(canvas, state, paints)

        canvas.restore()

        state.actualCueBall?.let { drawActualCueBallGhost(canvas, it, state, paints) }
        drawGhostBalls(canvas, state, paints)
    }

    private fun drawProtractorUnit(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        canvas.save()
        canvas.translate(state.protractorUnit.center.x, state.protractorUnit.center.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)

        canvas.drawCircle(0f, 0f, state.protractorUnit.radius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius / 5f, paints.targetCenterMarkPaint)

        val cuePaint =
            if (state.isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint
        val cueLocalPos = state.protractorUnit.protractorCueBallCenter.apply {
            offset(
                -state.protractorUnit.center.x,
                -state.protractorUnit.center.y
            )
        }
        canvas.drawCircle(cueLocalPos.x, cueLocalPos.y, state.protractorUnit.radius, cuePaint)
        canvas.drawCircle(
            cueLocalPos.x,
            cueLocalPos.y,
            state.protractorUnit.radius / 5f,
            paints.cueCenterMarkPaint
        )

        // Tangent Lines & Protractor Lines
        drawTangentLines(canvas, cueLocalPos, paints, state)
        drawProtractorLines(canvas, paints)

        canvas.restore()
    }

    private fun drawTangentLines(
        canvas: Canvas,
        cueLocalPos: PointF,
        paints: PaintCache,
        state: OverlayState
    ) {
        val dx = 0 - cueLocalPos.x
        val dy = 0 - cueLocalPos.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > 0.001f) {
            val extend =
                max(state.protractorUnit.center.x * 2, state.protractorUnit.center.y * 2) * 1.5f
            val dX = -dy / mag
            val dY = dx / mag
            val rightPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            val leftPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            canvas.drawLine(
                cueLocalPos.x,
                cueLocalPos.y,
                cueLocalPos.x + dX * extend,
                cueLocalPos.y + dY * extend,
                rightPaint
            )
            canvas.drawLine(
                cueLocalPos.x,
                cueLocalPos.y,
                cueLocalPos.x - dX * extend,
                cueLocalPos.y - dY * extend,
                leftPaint
            )
        }
    }

    private fun drawProtractorLines(canvas: Canvas, paints: PaintCache) {
        val lineLength = 2000f // A large number
        PROTRACTOR_ANGLES.forEach { angle ->
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat()
            val eY = (lineLength * cos(r)).toFloat()
            if (angle == 0f) {
                canvas.drawLine(0f, 0f, eX, eY, paints.protractorLinePaint)
                canvas.drawLine(0f, 0f, -eX, -eY, paints.aimingLinePaint)
            } else {
                canvas.drawLine(0f, 0f, eX, eY, paints.protractorLinePaint); canvas.drawLine(
                    0f,
                    0f,
                    -eX,
                    -eY,
                    paints.protractorLinePaint
                )
                val nR = Math.toRadians(-angle.toDouble())
                val nEX = (lineLength * sin(nR)).toFloat()
                val nEY = (lineLength * cos(nR)).toFloat()
                canvas.drawLine(0f, 0f, nEX, nEY, paints.protractorLinePaint); canvas.drawLine(
                    0f,
                    0f,
                    -nEX,
                    -nEY,
                    paints.protractorLinePaint
                )
            }
        }
    }

    private fun drawShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val startPoint: PointF = state.actualCueBall?.center ?: run {
            if (!state.hasInverseMatrix) return
            val screenAnchor = floatArrayOf(
                state.protractorUnit.center.x * 2 / 2f,
                state.protractorUnit.center.y * 2
            )
            val logicalAnchorArray = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        }

        val throughPoint = state.protractorUnit.protractorCueBallCenter
        val dirX = throughPoint.x - startPoint.x
        val dirY = throughPoint.y - startPoint.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = 5000f
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

    private fun drawActualCueBallBase(canvas: Canvas, ball: ILogicalBall, paints: PaintCache) {
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, paints.actualCueBallBasePaint)
        canvas.drawCircle(
            ball.center.x,
            ball.center.y,
            ball.radius / 5f,
            paints.actualCueBallCenterMarkPaint
        )
    }

    private fun drawActualCueBallGhost(
        canvas: Canvas,
        ball: ILogicalBall,
        state: OverlayState,
        paints: PaintCache
    ) {
        val radiusInfo = getPerspectiveRadiusAndLift(ball, state)
        val screenBasePos = mapPoint(ball.center, state.pitchMatrix)
        val ghostCenterY = screenBasePos.y - radiusInfo.lift
        canvas.drawCircle(
            screenBasePos.x,
            ghostCenterY,
            radiusInfo.radius,
            paints.actualCueBallGhostPaint
        )
    }

    private fun drawGhostBalls(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val targetRadiusInfo = getPerspectiveRadiusAndLift(state.protractorUnit, state)
        val cueRadiusInfo =
            getPerspectiveRadiusAndLift(state.protractorUnit.protractorCueBallCenter.let {
                ILogicalBall {
                    center = it; radius = state.protractorUnit.radius
                }
            }, state)

        val pTGC = mapPoint(state.protractorUnit.center, state.pitchMatrix)
        val pCGC = mapPoint(state.protractorUnit.protractorCueBallCenter, state.pitchMatrix)

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
        ball: ILogicalBall,
        state: OverlayState
    ): PerspectiveRadiusInfo {
        if (!state.hasInverseMatrix) return PerspectiveRadiusInfo(ball.radius, 0f)
        val screenCenter = mapPoint(ball.center, state.pitchMatrix)
        val logicalEdge = PointF(ball.center.x + ball.radius, ball.center.y)
        val screenEdge = mapPoint(logicalEdge, state.pitchMatrix)
        val radius = distance(screenCenter, screenEdge)
        val lift =
            radius * abs(sin(Math.toRadians(state.protractorUnit.rotationDegrees.toDouble()))).toFloat()
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
