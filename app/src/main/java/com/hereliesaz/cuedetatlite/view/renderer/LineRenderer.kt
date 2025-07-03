package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer(private val paints: PaintCache, private val textRenderer: LineTextRenderer) {

    private val PROTRACTOR_ANGLES = floatArrayOf(0f, 14f, 30f, 36f, 43f, 48f)

    fun drawLogicalLines(
        canvas: Canvas,
        state: OverlayState,
        typeface: Typeface?
    ) {
        if (state.screenState.isBankingMode) {
            drawBankingShotLines(canvas, state)
            return
        }

        drawProtractorShotLine(canvas, state)
        drawProtractorAimingAndAngleLines(canvas, state)
    }

    private fun drawBankingShotLines(canvas: Canvas, state: OverlayState) {
        if (state.screenState.bankingPath.size < 2) return

        val bankLinePaints = listOf(paints.bankShotLinePaint1, paints.bankShotLinePaint2, paints.bankShotLinePaint3)

        for (i in 0 until state.screenState.bankingPath.size - 1) {
            val start = state.screenState.bankingPath[i]
            val end = state.screenState.bankingPath[i + 1]
            val paint = bankLinePaints.getOrElse(i) { bankLinePaints.last() }
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }

    private fun drawProtractorShotLine(canvas: Canvas, state: OverlayState) {
        val startPoint: PointF = state.screenState.actualCueBall?.logicalPosition ?: run {
            if (!state.hasInverseMatrix) return
            val screenAnchor = floatArrayOf(state.viewWidth / 2f, state.viewHeight.toFloat())
            val logicalAnchorArray = FloatArray(2)
            state.inversePitchMatrix.mapPoints(logicalAnchorArray, screenAnchor)
            PointF(logicalAnchorArray[0], logicalAnchorArray[1])
        }
        val throughPoint = state.screenState.protractorUnit.ghostCueBall.logicalPosition
        val paintToUse = if (state.screenState.isImpossibleShot) paints.warningDottedPaintRed else paints.shotLinePaint
        drawExtendedLine(canvas, startPoint, throughPoint, paintToUse)
    }

    private fun drawProtractorAimingAndAngleLines(canvas: Canvas, state: OverlayState) {
        val protractorUnit = state.screenState.protractorUnit
        val ghostCuePos = protractorUnit.ghostCueBall.logicalPosition
        val targetPos = protractorUnit.targetBall.logicalPosition

        // Aiming Line
        drawExtendedLine(canvas, ghostCuePos, targetPos, paints.aimingLinePaint)

        // Tangent Lines
        val dxToTarget = targetPos.x - ghostCuePos.x
        val dyToTarget = targetPos.y - ghostCuePos.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val extend = state.viewWidth.coerceAtLeast(state.viewHeight) * 1.5f
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget

            val rightEndPoint = PointF(ghostCuePos.x + tangentDx * extend, ghostCuePos.y + tangentDy * extend)
            val leftEndPoint = PointF(ghostCuePos.x - tangentDx * extend, ghostCuePos.y - tangentDy * extend)

            canvas.drawLine(ghostCuePos.x, ghostCuePos.y, rightEndPoint.x, rightEndPoint.y, paints.tangentLineDottedPaint)
            canvas.drawLine(ghostCuePos.x, ghostCuePos.y, leftEndPoint.x, leftEndPoint.y, paints.tangentLineSolidPaint)
        }

        // Angle lines (simplified for brevity, can be expanded)
        PROTRACTOR_ANGLES.forEach { angle ->
            if (angle == 0f) return@forEach
            // This logic would need to be more complex to draw from the target ball center correctly
        }
    }

    private fun drawExtendedLine(canvas: Canvas, start: PointF, through: PointF, paint: Paint) {
        val dirX = through.x - start.x
        val dirY = through.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = 5000f
            val ndx = dirX / mag
            val ndy = dirY / mag
            canvas.drawLine(start.x, start.y, start.x + ndx * extendFactor, start.y + ndy * extendFactor, paint)
        }
    }
}
