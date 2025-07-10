package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.line.AimingLine
import com.hereliesaz.cuedetat.view.config.line.BankLine1
import com.hereliesaz.cuedetat.view.config.line.BankLine2
import com.hereliesaz.cuedetat.view.config.line.BankLine3
import com.hereliesaz.cuedetat.view.config.line.BankLine4
import com.hereliesaz.cuedetat.view.config.line.ShotGuideLine
import com.hereliesaz.cuedetat.view.config.line.TangentLine
import com.hereliesaz.cuedetat.view.config.ui.ProtractorGuides
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
            drawBankingLines(canvas, state, paints)
        } else {
            drawProtractorLines(canvas, state, paints, typeface)
        }
    }

    fun drawProtractorGuides(canvas: Canvas, state: OverlayState, paints: PaintCache, center: PointF, referencePoint: PointF) {
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center
        val config = ProtractorGuides()

        val guidePaint = Paint(paints.angleGuidePaint).apply {
            color = config.strokeColor.toArgb()
            strokeWidth = config.strokeWidth
            alpha = (config.opacity * 255).toInt()
        }
        val textPaint = Paint(paints.textPaint).apply {
            alpha = (config.opacity * 255).toInt()
            textSize = 30f
        }


        protractorAngles.forEach { angle ->
            drawAngleGuide(canvas, center, referencePoint, angle, guidePaint)
            drawAngleGuide(canvas, center, referencePoint, -angle, guidePaint) // Draw on both sides
            textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, angle, textPaint, state.protractorUnit.radius)
            textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, -angle, textPaint, state.protractorUnit.radius)
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

        val tolerance = 5f // A small tolerance in logical units

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

        // Load configs
        val aimingLineConfig = AimingLine()
        val shotGuideLineConfig = ShotGuideLine()
        val tangentLineConfig = TangentLine()
        val bankLineConfig = BankLine1()

        // Configure paints based on configs and state
        val shotLinePaint = Paint(paints.shotLinePaint).apply {
            color = if (state.isImpossibleShot || state.isTiltBeyondLimit) paints.warningPaint.color else shotGuideLineConfig.strokeColor.toArgb()
            strokeWidth = shotGuideLineConfig.strokeWidth
        }
        val shotLineGlow = Paint(paints.lineGlowPaint).apply {
            color = shotLinePaint.color
            strokeWidth = shotGuideLineConfig.glowWidth
        }

        // Aiming line color is NOT affected by pocketing in ghost mode, only its bank is.
        val aimingLinePaint = Paint(paints.targetCirclePaint).apply {
            color = aimingLineConfig.strokeColor.toArgb()
            strokeWidth = aimingLineConfig.strokeWidth
        }
        val aimingLineGlow = Paint(paints.lineGlowPaint).apply {
            color = aimingLinePaint.color
            strokeWidth = aimingLineConfig.glowWidth
            alpha = (aimingLineConfig.glowColor.alpha * 255).toInt()
        }
        val pocketedBankPaint = Paint(aimingLinePaint).apply { color = android.graphics.Color.WHITE }
        val pocketedBankGlow = Paint(aimingLineGlow).apply { color = android.graphics.Color.WHITE }


        val tangentSolidPaint = Paint(paints.tangentLineSolidPaint).apply {
            color = tangentLineConfig.strokeColor.toArgb()
            strokeWidth = tangentLineConfig.strokeWidth
        }
        val tangentDottedPaint = Paint(paints.tangentLineDottedPaint).apply {
            color = tangentLineConfig.strokeColor.toArgb()
            strokeWidth = tangentLineConfig.strokeWidth
            alpha = (tangentLineConfig.opacity * 255).toInt()
        }
        val tangentGlow = Paint(paints.lineGlowPaint).apply {
            strokeWidth = tangentLineConfig.glowWidth
            color = tangentLineConfig.glowColor.toArgb()
        }

        val aimingLineEnd = state.aimingLineEndPoint ?: targetCenter

        // --- Draw Glows First ---
        drawExtendedLine(canvas, ghostCueCenter, aimingLineEnd, aimingLineGlow)
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLineGlow)
        drawTangentLines(canvas, ghostCueCenter, targetCenter, tangentGlow, tangentGlow, state.tangentDirection)

        // --- Draw Lines Second ---
        drawExtendedLine(canvas, ghostCueCenter, aimingLineEnd, aimingLinePaint)
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLinePaint)
        drawTangentLines(canvas, ghostCueCenter, targetCenter, tangentSolidPaint, tangentDottedPaint, state.tangentDirection)

        // --- Draw Aiming Line Bank ---
        if (state.aimingLineBankPath.size > 1) {
            val bankStart = state.aimingLineBankPath[0]
            val bankMid = state.aimingLineBankPath[1]
            canvas.drawLine(bankStart.x, bankStart.y, bankMid.x, bankMid.y, aimingLinePaint)
            if (state.aimingLineBankPath.size > 2) {
                val bankEnd = state.aimingLineBankPath[2]
                val bankPaint = if (state.aimedPocketIndex != null) pocketedBankPaint else aimingLinePaint
                val bankGlow = if (state.aimedPocketIndex != null) pocketedBankGlow else aimingLineGlow
                canvas.drawLine(bankMid.x, bankMid.y, bankEnd.x, bankEnd.y, bankGlow)
                canvas.drawLine(bankMid.x, bankMid.y, bankEnd.x, bankEnd.y, bankPaint)
            }
        }

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
            // Draw diamond label for shot guide impact point
            state.shotGuideBankImpactPoint?.let { impactPoint ->
                getRailForPoint(impactPoint, state)?.let { railType ->
                    textRenderer.drawDiamondLabel(canvas, impactPoint, railType, state, paints.textPaint)
                }
            }
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.bankShotPath.size < 2) return

        val bankLineConfigs = listOf(BankLine1(), BankLine2(), BankLine3(), BankLine4())
        val lastSegmentIndex = state.bankShotPath.size - 2

        for (i in 0..lastSegmentIndex) {
            val start = state.bankShotPath[i]
            val end = state.bankShotPath[i+1]

            val isLastSegment = i == lastSegmentIndex
            val isPocketed = state.pocketedBankShotPocketIndex != null

            val config = bankLineConfigs.getOrElse(i) { bankLineConfigs.last() }

            val linePaint = if (isLastSegment && isPocketed) {
                Paint(paints.shotLinePaint).apply { color = android.graphics.Color.WHITE }
            } else {
                Paint(paints.bankLine1Paint).apply { color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth }
            }

            val glowPaint = Paint(paints.lineGlowPaint).apply {
                color = linePaint.color
                alpha = (config.glowColor.alpha * 255).toInt()
                strokeWidth = config.glowWidth
            }

            // Draw Glow
            canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
            // Draw Line
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
        }
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.bankShotPath.size < 2) return

        for (i in 0 until state.bankShotPath.size - 1) {
            val end = state.bankShotPath[i+1]
            getRailForPoint(end, state)?.let { railType ->
                val textPaint = paints.textPaint.apply { this.typeface = typeface }
                textRenderer.drawDiamondLabel(canvas, end, railType, state, textPaint)
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