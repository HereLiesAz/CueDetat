// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.line.*
import com.hereliesaz.cuedetat.view.config.ui.ProtractorGuides
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val textRenderer = LineTextRenderer()
    private val protractorAngles = floatArrayOf(5f, 10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f)

    fun drawLogicalLines(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state, paints)
        } else {
            drawProtractorLines(canvas, state, paints, typeface)
            drawProtractorGuides(canvas, state, paints)
        }
    }

    fun drawRailLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (!state.showTable) return

        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        // Diamond label for banked Aiming Line
        if (state.aimingLineBankPath.size > 1) {
            val bankPoint = state.aimingLineBankPath[1]
            getRailForPoint(bankPoint, state)?.let { railType ->
                textRenderer.drawDiamondLabel(canvas, bankPoint, railType, state, textPaint)
            }
        }
        // Diamond label for banked Tangent Line
        if (state.tangentLineBankPath.size > 1) {
            val tangentBankPoint = state.tangentLineBankPath[1]
            getRailForPoint(tangentBankPoint, state)?.let { railType ->
                textRenderer.drawDiamondLabel(canvas, tangentBankPoint, railType, state, textPaint)
            }
        }
        // Diamond label for Shot Guide Line
        state.shotGuideImpactPoint?.let { impactPoint ->
            getRailForPoint(impactPoint, state)?.let { railType ->
                textRenderer.drawDiamondLabel(canvas, impactPoint, railType, state, textPaint)
            }
        }
    }

    private fun drawProtractorLines(
        canvas: Canvas, state: OverlayState,
        paints: PaintCache, typeface: Typeface?
    ) {
        val shotLineAnchor = state.shotLineAnchor
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotGuideLineConfig = ShotGuideLine()

        val shotLineIsWarning = state.isGeometricallyImpossible || state.isTiltBeyondLimit || state.isObstructed || state.tangentAimedPocketIndex != null
        val shotLinePaint = Paint(paints.shotLinePaint).apply {
            color = if (shotLineIsWarning) paints.warningPaint.color else shotGuideLineConfig.strokeColor.toArgb()
            strokeWidth = shotGuideLineConfig.strokeWidth
        }
        val shotLineGlow = Paint(paints.lineGlowPaint).apply {
            color = shotLinePaint.color
            strokeWidth = shotGuideLineConfig.glowWidth
        }
        val obstructionPaint = Paint(paints.pathObstructionPaint).apply {
            strokeWidth = state.protractorUnit.radius * 2
        }

        // --- Pass 1: Wide Pathways ---
        drawPath(canvas, listOf(shotLineAnchor, ghostCueCenter), obstructionPaint)
        if (state.aimingLineBankPath.isNotEmpty()) {
            drawPath(canvas, state.aimingLineBankPath, obstructionPaint)
        } else {
            drawPath(canvas, listOf(ghostCueCenter, state.aimingLineEndPoint ?: state.protractorUnit.center), obstructionPaint)
        }

        // --- Pass 2: Glows ---
        drawPath(canvas, listOf(shotLineAnchor, ghostCueCenter), shotLineGlow)
        drawTangentLines(canvas, state, paints)

        // --- Pass 3: Core Lines ---
        drawPath(canvas, listOf(shotLineAnchor, ghostCueCenter), shotLinePaint)
        drawAimingLines(canvas, state, paints)
        drawSpinPaths(canvas, state, paints)

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawAimingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val aimingLineConfig = AimingLine()
        val isPocketed = state.aimedPocketIndex != null

        val baseAimingColor = if (isPocketed) RebelYellow else aimingLineConfig.strokeColor

        val aimingLinePaint = Paint(paints.targetCirclePaint).apply {
            color = baseAimingColor.toArgb()
            strokeWidth = aimingLineConfig.strokeWidth
        }
        val aimingLineGlow = Paint(paints.lineGlowPaint).apply {
            color = aimingLinePaint.color
            strokeWidth = aimingLineConfig.glowWidth
            alpha = (aimingLineConfig.glowColor.alpha * 255).toInt()
        }

        drawPath(canvas, state.aimingLineBankPath, aimingLineGlow)
        drawPath(canvas, state.aimingLineBankPath, aimingLinePaint)
    }


    private fun drawTangentLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val tangentLineConfig = TangentLine()
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

        drawPath(canvas, state.tangentLineBankPath, tangentGlow)
        drawPath(canvas, state.tangentLineBankPath, tangentSolidPaint)

        // Inactive tangent line
        val start = state.protractorUnit.ghostCueBallCenter
        val towards = state.protractorUnit.center
        val dxToTarget = towards.x - start.x
        val dyToTarget = towards.y - start.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget
            val extendFactor = 5000f
            val endX = start.x + tangentDx * extendFactor * -state.tangentDirection
            val endY = start.y + tangentDy * extendFactor * -state.tangentDirection
            canvas.drawLine(start.x, start.y, endX, endY, tangentDottedPaint)
        }
    }


    private fun drawSpinPaths(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.spinPaths.isEmpty()) return

        val alpha = (255 * state.spinPathsAlpha).toInt()
        val spinPathPaint = Paint(paints.shotLinePaint).apply {
            strokeWidth = 4f
        }

        val spinGlowPaint = Paint(paints.lineGlowPaint).apply {
            strokeWidth = 8f
        }

        state.spinPaths.forEach { (color, points) ->
            if (points.size < 2) return@forEach

            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            spinPathPaint.color = color.toArgb()
            spinPathPaint.alpha = alpha
            spinGlowPaint.color = color.toArgb()
            spinGlowPaint.alpha = (color.alpha * 100 * state.spinPathsAlpha).toInt().coerceIn(0, 255)

            canvas.drawPath(path, spinGlowPaint)
            canvas.drawPath(path, spinPathPaint)
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
                Paint(paints.shotLinePaint).apply { color = RebelYellow.toArgb() }
            } else {
                Paint(paints.bankLine1Paint).apply { color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth }
            }

            val glowPaint = Paint(paints.lineGlowPaint).apply {
                color = linePaint.color
                alpha = (config.glowColor.alpha * 255).toInt()
                strokeWidth = config.glowWidth
            }

            canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
        }
    }

    private fun drawProtractorGuides(canvas: Canvas, state: OverlayState, paints: PaintCache) {
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
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, angle, guidePaint)
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, -angle, guidePaint)
            if (state.areHelpersVisible) {
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, angle, textPaint, state.protractorUnit.radius)
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, -angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun getRailForPoint(point: PointF, state: OverlayState): LineTextRenderer.RailType? {
        val referenceRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
        if (referenceRadius <= 0) return null

        val ballRealDiameter = 2.25f
        val ballLogicalDiameter = referenceRadius * 2
        val scale = ballLogicalDiameter / ballRealDiameter
        val tableWidth = state.tableSize.longSideInches * scale
        val tableHeight = state.tableSize.shortSideInches * scale

        val halfW = tableWidth / 2f
        val halfH = tableHeight / 2f
        val canvasCenterX = state.viewWidth / 2f
        val canvasCenterY = state.viewHeight / 2f

        val left = canvasCenterX - halfW
        val top = canvasCenterY - halfH
        val right = canvasCenterX + halfW
        val bottom = canvasCenterY + halfH

        val tolerance = 5f

        return when {
            point.y > top - tolerance && point.y < top + tolerance -> LineTextRenderer.RailType.TOP
            point.y > bottom - tolerance && point.y < bottom + tolerance -> LineTextRenderer.RailType.BOTTOM
            point.x > left - tolerance && point.x < left + tolerance -> LineTextRenderer.RailType.LEFT
            point.x > right - tolerance && point.x < right + tolerance -> LineTextRenderer.RailType.RIGHT
            else -> null
        }
    }

    private fun drawPath(canvas: Canvas, path: List<PointF>, paint: Paint) {
        if (path.size < 2) return
        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }


    private fun drawAngleGuide(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val length = 5000f
        val endX = center.x + (length * cos(finalAngleRad)).toFloat()
        val endY = center.y + (length * sin(finalAngleRad)).toFloat()
        canvas.drawLine(center.x, center.y, endX, endY, paint)
    }
}