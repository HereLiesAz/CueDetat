package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    // --- POSITION CONTROL CONSTANTS ---
    private val SHOT_LINE_LABEL_DISTANCE_FACTOR = 15f
    private val RIGHT_TANGENT_LABEL_DISTANCE_FACTOR = 7f
    private val LEFT_TANGENT_LABEL_DISTANCE_FACTOR = 7f
    private val AIMING_LINE_LABEL_DISTANCE_FACTOR = 10f
    private val PROTRACTOR_LABEL_DISTANCE_FACTOR = 20f

    // --- ANGLE CONTROL CONSTANTS ---
    private val SHOT_LINE_LABEL_ANGLE_OFFSET = -2f
    private val RIGHT_TANGENT_LABEL_ANGLE_OFFSET = -5f
    private val LEFT_TANGENT_LABEL_ANGLE_OFFSET = 5f
    private val AIMING_LINE_LABEL_ANGLE_OFFSET = -2f
    private val PROTRACTOR_LABEL_ANGLE_OFFSET = 0f

    // --- ROTATION CONTROL CONSTANTS ---
    private val SHOT_LINE_LABEL_ROTATION = 0f
    private val RIGHT_TANGENT_LABEL_ROTATION = 0f
    private val LEFT_TANGENT_LABEL_ROTATION = 180f
    private val AIMING_LINE_LABEL_ROTATION = 0f
    private val PROTRACTOR_LABEL_ROTATION = 90f

    // --- NEW FONT SIZE CONTROL CONSTANTS ---
    private val SHOT_LINE_LABEL_FONT_SIZE = 38f
    private val RIGHT_TANGENT_LABEL_FONT_SIZE = 38f
    private val LEFT_TANGENT_LABEL_FONT_SIZE = 38f
    private val AIMING_LINE_LABEL_FONT_SIZE = 38f
    private val PROTRACTOR_LABEL_FONT_SIZE = 42f


    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val textRenderer = LineTextRenderer()

    fun drawLogicalLines(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        paints.lineTextPaint.typeface = typeface
        drawShotLine(canvas, state, paints)

        if (state.isBankingMode) return // Don't draw protractor lines in banking mode

        canvas.save()
        canvas.translate(state.protractorUnit.center.x, state.protractorUnit.center.y)
        canvas.rotate(state.protractorUnit.rotationDegrees)
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
        drawTangentLines(canvas, cuePos, paints, state)
        drawProtractorLines(canvas, cuePos, paints, state)
        canvas.restore()
    }

    private fun getAngle(from: PointF, to: PointF): Float {
        return Math.toDegrees(atan2(to.y - from.y, to.x - from.x).toDouble()).toFloat()
    }

    private fun drawShotLine(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val startPoint: PointF = state.actualCueBall?.center ?: run {
            if (!state.hasInverseMatrix) return
            val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
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

            if (state.areHelpersVisible) {
                val textPaint = Paint(paints.lineTextPaint).apply { color = paint.color }
                val labelDistance = state.protractorUnit.radius * SHOT_LINE_LABEL_DISTANCE_FACTOR
                val lineAngle = getAngle(startPoint, throughPoint)
                textRenderer.draw(
                    canvas,
                    "Shot Line",
                    throughPoint,
                    lineAngle,
                    labelDistance,
                    SHOT_LINE_LABEL_ANGLE_OFFSET,
                    SHOT_LINE_LABEL_ROTATION,
                    textPaint,
                    SHOT_LINE_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
            }
        }
    }

    private fun drawTangentLines(
        canvas: Canvas,
        cueLocalPos: PointF,
        paints: PaintCache,
        state: OverlayState
    ) {
        val dx = 0f - cueLocalPos.x
        val dy = 0f - cueLocalPos.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > 0.001f) {
            val extend = state.viewWidth.coerceAtLeast(state.viewHeight) * 1.5f
            val dX = -dy / mag
            val dY = dx / mag

            val rightPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees <= 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            val leftPaint =
                if (state.isImpossibleShot || state.protractorUnit.rotationDegrees > 180f) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint

            val rightEndPoint = PointF(cueLocalPos.x + dX * extend, cueLocalPos.y + dY * extend)
            val leftEndPoint = PointF(cueLocalPos.x - dX * extend, cueLocalPos.y - dY * extend)

            canvas.drawLine(
                cueLocalPos.x,
                cueLocalPos.y,
                rightEndPoint.x,
                rightEndPoint.y,
                rightPaint
            )
            canvas.drawLine(cueLocalPos.x, cueLocalPos.y, leftEndPoint.x, leftEndPoint.y, leftPaint)

            if (state.areHelpersVisible) {
                val textPaintRight = Paint(paints.lineTextPaint).apply { color = rightPaint.color }
                val textPaintLeft = Paint(paints.lineTextPaint).apply { color = leftPaint.color }

                val rightLabelDistance =
                    state.protractorUnit.radius * RIGHT_TANGENT_LABEL_DISTANCE_FACTOR
                val leftLabelDistance =
                    state.protractorUnit.radius * LEFT_TANGENT_LABEL_DISTANCE_FACTOR

                val rightLineAngle = getAngle(cueLocalPos, rightEndPoint)
                val leftLineAngle = getAngle(cueLocalPos, leftEndPoint)

                textRenderer.draw(
                    canvas,
                    "Tangent Line",
                    cueLocalPos,
                    rightLineAngle,
                    rightLabelDistance,
                    RIGHT_TANGENT_LABEL_ANGLE_OFFSET,
                    RIGHT_TANGENT_LABEL_ROTATION,
                    textPaintRight,
                    RIGHT_TANGENT_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
                textRenderer.draw(
                    canvas,
                    "Tangent Line",
                    cueLocalPos,
                    leftLineAngle,
                    leftLabelDistance,
                    LEFT_TANGENT_LABEL_ANGLE_OFFSET,
                    LEFT_TANGENT_LABEL_ROTATION,
                    textPaintLeft,
                    LEFT_TANGENT_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
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
        val origin = PointF(0f, 0f)

        // Aiming Line
        val aimDirX = 0 - cueLocalPos.x
        val aimDirY = 0 - cueLocalPos.y
        val mag = sqrt(aimDirX * aimDirX + aimDirY * aimDirY)
        if (mag > 0.001f) {
            val nX = aimDirX / mag
            val nY = aimDirY / mag
            val endPoint = PointF(cueLocalPos.x + nX * lineLength, cueLocalPos.y + nY * lineLength)
            canvas.drawLine(
                cueLocalPos.x,
                cueLocalPos.y,
                endPoint.x,
                endPoint.y,
                paints.aimingLinePaint
            )

            if (state.areHelpersVisible) {
                val textPaint =
                    Paint(paints.lineTextPaint).apply { color = paints.aimingLinePaint.color }
                val labelDistance = state.protractorUnit.radius * AIMING_LINE_LABEL_DISTANCE_FACTOR
                val lineAngle = getAngle(cueLocalPos, endPoint)
                textRenderer.draw(
                    canvas,
                    "Aiming Line",
                    cueLocalPos,
                    lineAngle,
                    labelDistance,
                    AIMING_LINE_LABEL_ANGLE_OFFSET,
                    AIMING_LINE_LABEL_ROTATION,
                    textPaint,
                    AIMING_LINE_LABEL_FONT_SIZE,
                    state.zoomSliderPosition
                )
            }
        }

        // Angle lines
        PROTRACTOR_ANGLES.forEach { angle ->
            if (angle == 0f) return@forEach
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat()
            val eY = (lineLength * cos(r)).toFloat()

            val endPoints =
                listOf(PointF(eX, eY), PointF(-eX, -eY), PointF(-eX, eY), PointF(eX, -eY))
            endPoints.forEach { endPoint ->
                canvas.drawLine(
                    origin.x,
                    origin.y,
                    endPoint.x,
                    endPoint.y,
                    paints.protractorLinePaint
                )
                if (state.areHelpersVisible) {
                    val textPaint = Paint(paints.lineTextPaint).apply {
                        color = paints.protractorLinePaint.color
                    }
                    val labelDistance =
                        state.protractorUnit.radius * PROTRACTOR_LABEL_DISTANCE_FACTOR
                    val lineAngle = getAngle(origin, endPoint)
                    val label = "${angle.toInt()}°"
                    textRenderer.draw(
                        canvas,
                        label,
                        origin,
                        lineAngle,
                        labelDistance,
                        PROTRACTOR_LABEL_ANGLE_OFFSET,
                        PROTRACTOR_LABEL_ROTATION,
                        textPaint,
                        PROTRACTOR_LABEL_FONT_SIZE,
                        state.zoomSliderPosition
                    )
                }
            }
        }
    }
}