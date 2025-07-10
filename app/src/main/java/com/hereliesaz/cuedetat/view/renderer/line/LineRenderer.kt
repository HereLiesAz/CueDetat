package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val textRenderer = LineTextRenderer()
    private val protractorAngles = floatArrayOf(5f, 10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f)

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints, typeface)
        } else {
            drawProtractorLines(canvas, state, paints, typeface)
        }
    }

    // New public method for drawing screen-space guides
    fun drawProtractorGuides(canvas: Canvas, state: OverlayState, paints: PaintCache, center: PointF, referencePoint: PointF) {
        protractorAngles.forEach { angle ->
            drawAngleGuide(canvas, center, referencePoint, angle, paints.angleGuidePaint)
            drawAngleGuide(canvas, center, referencePoint, -angle, paints.angleGuidePaint) // Draw on both sides
        }
    }

    private fun getRailForPoint(point: PointF, state: OverlayState): LineTextRenderer.RailType? {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return null

        val tableToBallRatioLong = state.tableSize.getTableToBallRatioLong()
        val tableToBallRatioShort = tableToBallRatioLong / state.tableSize.aspectRatio
        val tableWidth = tableToBallRatioLong * referenceRadius
        val tableHeight = tableToBallRatioShort * referenceRadius

        val halfW = tableWidth / 2f
        val halfH = tableHeight / 2f
        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f

        val left = canvasCenterX - halfW
        val top = canvasCenterY - halfH
        val right = canvasCenterX + halfW
        val bottom = canvasCenterY + halfH

        val tolerance = 5f // A small tolerance in pixels

        return when {
            abs(point.y - top) < tolerance -> LineTextRenderer.RailType.TOP
            abs(point.y - bottom) < tolerance -> LineTextRenderer.RailType.BOTTOM
            abs(point.x - left) < tolerance -> LineTextRenderer.RailType.LEFT
            abs(point.x - right) < tolerance -> LineTextRenderer.RailType.RIGHT
            else -> null
        }
    }

    private fun drawProtractorLines(
        canvas: Canvas, state: OverlayState,
        paints: PaintCache, typeface: Typeface?
    ) {
        val targetCenter = state.protractorUnit.center
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotLineAnchor = state.shotLineAnchor


        val shotLinePaint = if (state.isImpossibleShot || state.isTiltBeyondLimit) paints.warningPaint else paints.shotLinePaint
        val shotLineGlow = if (state.isImpossibleShot || state.isTiltBeyondLimit) {
            Paint(paints.lineGlowPaint).apply { color = paints.warningPaint.color }
        } else {
            paints.lineGlowPaint
        }

        val aimingLinePaint = if (state.aimedPocketIndex != null) Paint(paints.shotLinePaint).apply { color = android.graphics.Color.WHITE } else paints.targetCirclePaint
        val aimingLineGlow = if (state.aimedPocketIndex != null) {
            Paint(paints.lineGlowPaint).apply { color = android.graphics.Color.WHITE; alpha = 150 }
        } else {
            paints.lineGlowPaint
        }

        val aimingLineEnd = state.aimingLineEndPoint ?: targetCenter

        // --- Draw Glows First ---
        drawExtendedLine(canvas, ghostCueCenter, aimingLineEnd, aimingLineGlow) // Aiming Line Glow
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLineGlow) // Shot Line Glow (conditional color)
        drawTangentLines(canvas, ghostCueCenter, targetCenter, paints.lineGlowPaint, paints.lineGlowPaint, state.tangentDirection) // Tangent Glows

        // --- Draw Lines Second ---
        drawExtendedLine(canvas, ghostCueCenter, aimingLineEnd, aimingLinePaint) // Aiming Line
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLinePaint) // Shot Line
        drawTangentLines(canvas, ghostCueCenter, targetCenter, paints.tangentLineSolidPaint, paints.tangentLineDottedPaint, state.tangentDirection) // Tangent Lines

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
            val textPaint = Paint(paints.textPaint).apply { alpha = 80; textSize = 30f }
            protractorAngles.forEach { angle ->
                textRenderer.drawAngleLabel(canvas, targetCenter, ghostCueCenter, angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.bankShotPath.size < 2) return

        val bankLinePaints = listOf(paints.bankLine1Paint, paints.bankLine2Paint, paints.bankLine3Paint, paints.bankLine4Paint)
        val lastSegmentIndex = state.bankShotPath.size - 2

        for (i in 0..lastSegmentIndex) {
            val start = state.bankShotPath[i]
            val end = state.bankShotPath[i+1]

            val isLastSegment = i == lastSegmentIndex
            val isPocketed = state.pocketedBankShotPocketIndex != null

            // Choose paint for the line based on segment index and whether it's the final, pocketed shot
            val linePaint = if (isLastSegment && isPocketed) {
                Paint(paints.shotLinePaint).apply { color = android.graphics.Color.WHITE }
            } else {
                bankLinePaints.getOrElse(i) { bankLinePaints.last() } // Fallback to the last color
            }

            val glowPaint = Paint(paints.lineGlowPaint).apply {
                color = linePaint.color
                alpha = 100 // a consistent glow alpha
            }

            // Draw Glow
            canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
            // Draw Line
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)

            // Draw diamond label at impact point
            getRailForPoint(end, state)?.let { railType ->
                val textPaint = paints.textPaint.apply { this.typeface = typeface }
                textRenderer.drawDiamondLabel(canvas, end, railType, state, textPaint)
            }
        }

        if (state.areHelpersVisible) {
            textRenderer.drawBankingLabels(canvas, state, paints, typeface)
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