package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class OverlayRenderer {

    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val baseGhostBallTextSize = 30f
    private val minGhostBallTextSize = 15f
    private val maxGhostBallTextSize = 60f
    private val lineTextSize = 28f

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.protractorUnit.center.x == 0f) return

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

        // Draw Target Ball (at the new origin)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius, paints.targetCirclePaint)
        canvas.drawCircle(0f, 0f, state.protractorUnit.radius / 5f, paints.targetCenterMarkPaint)

        // Determine position of Ghost Cue Ball relative to the now-rotated canvas
        val cueBallLocalCenter = state.protractorUnit.protractorCueBallCenter.let {
            val p = PointF(it.x, it.y)
            p.offset(-state.protractorUnit.center.x, -state.protractorUnit.center.y)
            p
        }
        val rotationInvertedMatrix =
            Matrix().apply { setRotate(-state.protractorUnit.rotationDegrees) }
        val cueBallRelativePosition = floatArrayOf(cueBallLocalCenter.x, cueBallLocalCenter.y)
        rotationInvertedMatrix.mapPoints(cueBallRelativePosition)
        val cuePos = PointF(cueBallRelativePosition[0], cueBallRelativePosition[1])

        // Draw Ghost Cue Ball
        val cuePaint =
            if (state.isImpossibleShot) paints.warningPaintRed1 else paints.cueCirclePaint
        canvas.drawCircle(cuePos.x, cuePos.y, state.protractorUnit.radius, cuePaint)
        canvas.drawCircle(
            cuePos.x,
            cuePos.y,
            state.protractorUnit.radius / 5f,
            paints.cueCenterMarkPaint
        )

        // Draw Tangent lines and Protractor lines from the perspective of the rotated canvas
        drawTangentLines(canvas, cuePos, paints, state)
        drawProtractorLines(canvas, cuePos, paints, state)

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
            val extend = max(state.viewWidth, state.viewHeight) * 1.5f
            val dX = -dy / mag
            val dY = dx / mag
            val rightPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            val leftPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint

            val rightStart = PointF(cueLocalPos.x, cueLocalPos.y)
            val rightEnd = PointF(cueLocalPos.x + dX * extend, cueLocalPos.y + dY * extend)
            canvas.drawLine(rightStart.x, rightStart.y, rightEnd.x, rightEnd.y, rightPaint)

            val leftStart = PointF(cueLocalPos.x, cueLocalPos.y)
            val leftEnd = PointF(cueLocalPos.x - dX * extend, cueLocalPos.y - dY * extend)
            canvas.drawLine(leftStart.x, leftStart.y, leftEnd.x, leftEnd.y, leftPaint)

            if (state.areHelpersVisible) {
                drawLineText(
                    canvas,
                    "Tangent Line",
                    rightStart,
                    rightEnd,
                    paints.lineTextPaint,
                    150f
                )
                drawLineText(canvas, "Tangent Line", leftStart, leftEnd, paints.lineTextPaint, 150f)
            }
        }
    }


    private fun drawProtractorLines(
        canvas: Canvas,
        cueLocalPos: PointF,
        paints: PaintCache,
        state: OverlayState
    ) {
        val lineLength = 2000f

        // Draw the Aiming Line (Line of Centers)
        val aimDirX = 0 - cueLocalPos.x // Vector from cue to target (origin)
        val aimDirY = 0 - cueLocalPos.y
        val mag = sqrt(aimDirX * aimDirX + aimDirY * aimDirY)
        if (mag > 0.001f) {
            val nX = aimDirX / mag
            val nY = aimDirY / mag
            val start = cueLocalPos
            val end = PointF(cueLocalPos.x + nX * lineLength, cueLocalPos.y + nY * lineLength)
            canvas.drawLine(start.x, start.y, end.x, end.y, paints.aimingLinePaint)
            if (state.areHelpersVisible) {
                drawLineText(canvas, "Aiming Line", start, end, paints.lineTextPaint, 250f)
            }
        }


        // Draw the other protractor angles relative to the target ball (origin)
        PROTRACTOR_ANGLES.forEach { angle ->
            if (angle == 0f) return@forEach // Skip the 0-degree line as it's the aiming line now
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat()
            val eY = (lineLength * cos(r)).toFloat()

            canvas.drawLine(0f, 0f, eX, eY, paints.protractorLinePaint)
            canvas.drawLine(0f, 0f, -eX, -eY, paints.protractorLinePaint)
            canvas.drawLine(0f, 0f, -eX, eY, paints.protractorLinePaint)
            canvas.drawLine(0f, 0f, eX, -eY, paints.protractorLinePaint)
        }
    }

    private fun drawShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val startPoint: PointF = state.actualCueBall?.center ?: run {
            if (!state.hasInverseMatrix) return
            val screenAnchor = floatArrayOf(
                state.viewWidth / 2f,
                state.viewHeight.toFloat()
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
            val lineStart = PointF(startPoint.x, startPoint.y)
            val lineEnd =
                PointF(startPoint.x + ndx * extendFactor, startPoint.y + ndy * extendFactor)
            canvas.drawLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y, paint)
            if (state.areHelpersVisible && state.actualCueBall != null) {
                drawLineText(canvas, "Shot Line", lineStart, lineEnd, paints.lineTextPaint, 350f)
            }
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
        val radiusInfo = getPerspectiveRadiusAndLift(ball, state, false) // Decouple from rotation
        val screenBasePos = mapPoint(ball.center, state.pitchMatrix)
        val ghostCenterY = screenBasePos.y - radiusInfo.lift
        canvas.drawCircle(
            screenBasePos.x,
            ghostCenterY,
            radiusInfo.radius,
            paints.actualCueBallGhostPaint
        )
        if (state.areHelpersVisible) {
            drawGhostBallText(
                canvas,
                paints.cueBallTextPaint,
                state.zoomSliderPosition,
                screenBasePos.x,
                ghostCenterY,
                radiusInfo.radius,
                "Actual Cue Ball"
            )
        }
    }

    private fun drawGhostBalls(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val targetRadiusInfo =
            getPerspectiveRadiusAndLift(state.protractorUnit, state, true) // Coupled to rotation
        val cueRadiusInfo =
            getPerspectiveRadiusAndLift(state.protractorUnit.let {
                object : ILogicalBall {
                    override val center = it.protractorCueBallCenter
                    override val radius = it.radius
                }
            }, state, true) // Coupled to rotation


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

        if (state.areHelpersVisible) {
            drawGhostBallText(
                canvas,
                paints.targetBallTextPaint,
                state.zoomSliderPosition,
                pTGC.x,
                targetGhostCenterY,
                targetRadiusInfo.radius,
                "Target Ball"
            )
            drawGhostBallText(
                canvas,
                paints.cueBallTextPaint,
                state.zoomSliderPosition,
                pCGC.x,
                cueGhostCenterY,
                cueRadiusInfo.radius,
                "Ghost Cue Ball"
            )
        }
    }

    private fun drawLineText(
        canvas: Canvas,
        text: String,
        start: PointF,
        end: PointF,
        paint: Paint,
        offset: Float
    ) {
        paint.textSize = lineTextSize
        val path = Path()
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
        canvas.drawTextOnPath(text, path, offset, -20f, paint)
    }

    private fun drawGhostBallText(
        canvas: Canvas,
        paint: Paint,
        zoomSliderPosition: Float,
        x: Float,
        y: Float,
        radius: Float,
        text: String
    ) {
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPosition) / ZoomMapping.DEFAULT_ZOOM
        val currentTextSize = (baseGhostBallTextSize * zoomFactor).coerceIn(
            minGhostBallTextSize,
            maxGhostBallTextSize
        )
        paint.textSize = currentTextSize
        val textMetrics = paint.fontMetrics
        val textPadding = 5f * zoomFactor.coerceAtLeast(0.5f)
        val visualTop = y - radius
        val baseline = visualTop - textPadding - textMetrics.descent
        canvas.drawText(text, x, baseline, paint)
    }


    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    internal fun getPerspectiveRadiusAndLift(
        ball: ILogicalBall,
        state: OverlayState,
        useProtractorRotation: Boolean
    ): PerspectiveRadiusInfo {
        if (!state.hasInverseMatrix) return PerspectiveRadiusInfo(ball.radius, 0f)
        val screenCenter = mapPoint(ball.center, state.pitchMatrix)
        val logicalEdge = PointF(ball.center.x + ball.radius, ball.center.y)
        val screenEdge = mapPoint(logicalEdge, state.pitchMatrix)
        val radius = distance(screenCenter, screenEdge)

        val angleForLift = if (useProtractorRotation) state.protractorUnit.rotationDegrees else 0f

        val lift = if (abs(state.pitchAngle) > 0.5) { // Only apply lift if phone is tilted
            radius * abs(sin(Math.toRadians(angleForLift.toDouble()))).toFloat()
        } else {
            0f
        }

        return PerspectiveRadiusInfo(radius, lift)
    }

    private fun distance(p1: PointF, p2: PointF): Float =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

    internal fun mapPoint(p: PointF, m: Matrix): PointF {
        val arr = floatArrayOf(p.x, p.y)
        m.mapPoints(arr)
        return PointF(arr[0], arr[1])
    }
}
