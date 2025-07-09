package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.sqrt

class LineRenderer {
    private val textRenderer = LineTextRenderer()
    private val protractorAngles = floatArrayOf(14f, 30f)

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        drawGlows(canvas, state, paints)
        drawLines(canvas, state, paints, typeface)
    }

    private fun drawGlows(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints.glowPaint)
        } else {
            drawProtractorLines(canvas, state, paints.glowPaint, paints.glowPaint, paints.glowPaint)
        }
    }

    private fun drawLines(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints.bankLinePaint, paints, typeface)
        } else {
            val shotLinePaint = if (state.isImpossibleShot) paints.warningPaint else paints.shotLinePaint
            val tangentPaint = if (state.isImpossibleShot) paints.tangentLineDottedPaint else paints.tangentLineSolidPaint
            drawProtractorLines(canvas, state, paints.aimingLinePaint, shotLinePaint, tangentPaint, paints, typeface)
        }
    }

    private fun drawProtractorLines(
        canvas: Canvas, state: OverlayState,
        aimingPaint: Paint, shotPaint: Paint, tangentPaint: Paint,
        allPaints: PaintCache? = null, typeface: Typeface? = null
    ) {
        val protractorUnit = state.protractorUnit
        val targetCenter = protractorUnit.center
        val ghostCueCenter = protractorUnit.protractorCueBallCenter

        drawExtendedLine(canvas, ghostCueCenter, targetCenter, aimingPaint)

        state.actualCueBall?.let {
            drawExtendedLine(canvas, it.center, ghostCueCenter, shotPaint)
        }

        drawTangentLines(canvas, ghostCueCenter, targetCenter, tangentPaint, allPaints?.tangentLineDottedPaint, state.protractorUnit.radius * 2)

        if (allPaints != null && typeface != null && state.areHelpersVisible) {
            val angleGuidePaint = Paint(allPaints.aimingLinePaint).apply { alpha = 80 }
            val textPaint = Paint(allPaints.textPaint).apply { alpha = 80; textSize = 30f }

            protractorAngles.forEach { angle ->
                drawAngleGuide(canvas, targetCenter, ghostCueCenter, angle, angleGuidePaint)
                textRenderer.drawAngleLabel(canvas, targetCenter, ghostCueCenter, angle, textPaint, state.protractorUnit.radius)
            }
            textRenderer.drawProtractorLabels(canvas, state, allPaints, typeface)
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paint: Paint, allPaints: PaintCache? = null, typeface: Typeface? = null) {
        state.actualCueBall?.let { ball ->
            state.bankingAimTarget?.let { target ->
                drawExtendedLine(canvas, ball.center, target, paint)
                if (allPaints != null && typeface != null && state.areHelpersVisible) {
                    textRenderer.drawBankingLabels(canvas, state, allPaints, typeface)
                }
            }
        }
    }

    private fun drawExtendedLine(canvas: Canvas, start: PointF, end: PointF, paint: Paint) {
        val dirX = end.x - start.x; val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = 5000f; val ndx = dirX / mag; val ndy = dirY / mag
            canvas.drawLine(start.x, start.y, start.x + ndx * extendFactor, start.y + ndy * extendFactor, paint)
        }
    }

    private fun drawTangentLines(canvas: Canvas, from: PointF, towards: PointF, solidPaint: Paint, dottedPaint: Paint?, length: Float) {
        val dxToTarget = towards.x - from.x
        val dyToTarget = towards.y - from.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget
            canvas.drawLine(from.x, from.y, from.x + tangentDx * length, from.y + tangentDy * length, solidPaint)
            dottedPaint?.let {
                canvas.drawLine(from.x, from.y, from.x - tangentDx * length, from.y - tangentDy * length, it)
            }
        }
    }

    private fun drawAngleGuide(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val length = 5000f
        val endX = center.x + length * kotlin.math.cos(finalAngleRad).toFloat()
        val endY = center.y + length * kotlin.math.sin(finalAngleRad).toFloat()
        canvas.drawLine(center.x, center.y, endX, endY, paint)
    }
}