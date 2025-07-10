package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val textRenderer = LineTextRenderer()
    private val protractorAngles = floatArrayOf(14f, 30f, 43f, 48f)

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints, typeface)
        } else {
            drawProtractorLines(canvas, state, paints, typeface)
        }
    }

    // New public method for drawing screen-space guides
    fun drawProtractorGuides(canvas: Canvas, state: OverlayState, paints: PaintCache, screenCenter: PointF) {
        val ghostCueScreenCenter = DrawingUtils.mapPoint(state.protractorUnit.ghostCueBallCenter, state.pitchMatrix)
        protractorAngles.forEach { angle ->
            drawAngleGuide(canvas, screenCenter, ghostCueScreenCenter, angle, paints.angleGuidePaint)
        }
    }

    private fun drawProtractorLines(
        canvas: Canvas, state: OverlayState,
        paints: PaintCache, typeface: Typeface?
    ) {
        val targetCenter = state.protractorUnit.center
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter

        // Determine paint for shot line based on warnings
        val shotLinePaint = if (state.isImpossibleShot || state.isTiltBeyondLimit) {
            paints.warningPaint // Use warning paint if shot is invalid
        } else {
            paints.shotLinePaint // Use standard paint otherwise
        }
        val shotLineGlow = if(state.isImpossibleShot || state.isTiltBeyondLimit) paints.lineGlowPaint.apply { color = paints.warningPaint.color } else paints.lineGlowPaint

        // --- Draw Glows First ---
        drawExtendedLine(canvas, ghostCueCenter, targetCenter, paints.lineGlowPaint) // Aiming Line Glow
        if (!state.isTiltBeyondLimit) {
            drawExtendedLine(canvas, state.shotLineAnchor, ghostCueCenter, shotLineGlow) // Shot Line Glow (conditional color)
        }
        drawTangentLines(canvas, ghostCueCenter, targetCenter, paints.lineGlowPaint, paints.lineGlowPaint, state.tangentDirection) // Tangent Glows


        // --- Draw Lines Second ---
        drawExtendedLine(canvas, ghostCueCenter, targetCenter, paints.targetCirclePaint) // Aiming Line

        if (!state.isTiltBeyondLimit) {
            drawExtendedLine(canvas, state.shotLineAnchor, ghostCueCenter, shotLinePaint) // Shot Line
        }

        drawTangentLines(canvas, ghostCueCenter, targetCenter, paints.tangentLineSolidPaint, paints.tangentLineDottedPaint, state.tangentDirection) // Tangent Lines


        // Labels are drawn on the pitched plane
        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
            // Draw angle labels on the pitched plane as well
            val textPaint = Paint(paints.textPaint).apply { alpha = 80; textSize = 30f }
            protractorAngles.forEach { angle ->
                textRenderer.drawAngleLabel(canvas, targetCenter, ghostCueCenter, angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        state.onPlaneBall?.let { ball ->
            state.bankingAimTarget?.let { target ->
                // Draw glow first
                drawExtendedLine(canvas, ball.center, target, paints.lineGlowPaint)
                // Then the line
                drawExtendedLine(canvas, ball.center, target, paints.bankLinePaint)

                if (state.areHelpersVisible) {
                    textRenderer.drawBankingLabels(canvas, state, paints, typeface)
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

    private fun drawTangentLines(canvas: Canvas, from: PointF, towards: PointF, solidPaint: Paint, dottedPaint: Paint?, direction: Float) {
        val dxToTarget = towards.x - from.x
        val dyToTarget = towards.y - from.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget
            val extendFactor = 5000f

            canvas.drawLine(from.x, from.y, from.x + tangentDx * extendFactor * direction, from.y + tangentDy * extendFactor * direction, solidPaint)
            dottedPaint?.let {
                canvas.drawLine(from.x, from.y, from.x - tangentDx * extendFactor * direction, from.y - tangentDy * extendFactor * direction, it)
            }
        }
    }

    private fun drawAngleGuide(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val length = 5000f
        val endX = center.x + length * cos(finalAngleRad).toFloat()
        val endY = center.y + length * sin(finalAngleRad).toFloat()
        canvas.drawLine(center.x, center.y, endX, endY, paint)
    }
}