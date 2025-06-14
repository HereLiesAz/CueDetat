package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)
    private val lineTextSize = 38f

    fun drawLogicalLines(
        canvas: Canvas,
        state: OverlayState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
        paints.lineTextPaint.typeface = typeface

        drawShotLine(canvas, state, paints)

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
            val lineStart = PointF(startPoint.x, startPoint.y)
            val lineEnd =
                PointF(startPoint.x + ndx * extendFactor, startPoint.y + ndy * extendFactor)
            canvas.drawLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y, paint)

            if (state.areHelpersVisible) {
                val textPaint = Paint(paints.lineTextPaint).apply { color = paint.color }
                val labelOffset = state.protractorUnit.radius * 1.5f
                drawLineText(
                    canvas,
                    "Shot Line",
                    throughPoint,
                    lineEnd,
                    textPaint,
                    labelOffset,
                    -20f
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
        val dx = 0 - cueLocalPos.x
        val dy = 0 - cueLocalPos.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > 0.001f) {
            val extend = state.viewWidth.coerceAtLeast(state.viewHeight) * 1.5f
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

            if (state.areHelpersVisible) {
                val textPaintRight = Paint(paints.lineTextPaint).apply { color = rightPaint.color }
                val textPaintLeft = Paint(paints.lineTextPaint).apply { color = leftPaint.color }
                val labelOffset = state.protractorUnit.radius * 1.5f
                drawLineText(
                    canvas,
                    "Tangent Line",
                    cueLocalPos,
                    PointF(cueLocalPos.x + dX * extend, cueLocalPos.y + dY * extend),
                    textPaintRight,
                    labelOffset,
                    -20f
                )
                drawLineText(
                    canvas,
                    "Tangent Line",
                    cueLocalPos,
                    PointF(cueLocalPos.x - dX * extend, cueLocalPos.y - dY * extend),
                    textPaintLeft,
                    labelOffset,
                    -20f
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

        val aimDirX = 0 - cueLocalPos.x
        val aimDirY = 0 - cueLocalPos.y
        val mag = sqrt(aimDirX * aimDirX + aimDirY * aimDirY)
        if (mag > 0.001f) {
            val nX = aimDirX / mag
            val nY = aimDirY / mag
            canvas.drawLine(
                cueLocalPos.x,
                cueLocalPos.y,
                cueLocalPos.x + nX * lineLength,
                cueLocalPos.y + nY * lineLength,
                paints.aimingLinePaint
            )
            if (state.areHelpersVisible) {
                val textPaint =
                    Paint(paints.lineTextPaint).apply { color = paints.aimingLinePaint.color }
                val labelOffset = state.protractorUnit.radius * 1.5f
                drawLineText(
                    canvas,
                    "Aiming Line",
                    cueLocalPos,
                    PointF(cueLocalPos.x + nX * lineLength, cueLocalPos.y + nY * lineLength),
                    textPaint,
                    labelOffset,
                    -20f
                )
            }
        }

        PROTRACTOR_ANGLES.forEach { angle ->
            if (angle == 0f) return@forEach
            val r = Math.toRadians(angle.toDouble())
            val eX = (lineLength * sin(r)).toFloat()
            val eY = (lineLength * cos(r)).toFloat()
            canvas.drawLine(0f, 0f, eX, eY, paints.protractorLinePaint)
            canvas.drawLine(0f, 0f, -eX, -eY, paints.protractorLinePaint)
            canvas.drawLine(0f, 0f, -eX, eY, paints.protractorLinePaint)
            canvas.drawLine(0f, 0f, eX, -eY, paints.protractorLinePaint)
        }
    }

    private fun drawLineText(
        canvas: Canvas,
        text: String,
        start: PointF,
        end: PointF,
        paint: Paint,
        hOffset: Float,
        vOffset: Float,
        size: Float = lineTextSize
    ) {
        paint.textSize = size
        val path = Path()
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
        canvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
    }
}