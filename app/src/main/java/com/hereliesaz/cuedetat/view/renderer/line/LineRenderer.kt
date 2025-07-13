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
import com.hereliesaz.cuedetat.view.config.line.AimingLine
import com.hereliesaz.cuedetat.view.config.line.BankLine1
import com.hereliesaz.cuedetat.view.config.line.BankLine2
import com.hereliesaz.cuedetat.view.config.line.BankLine3
import com.hereliesaz.cuedetat.view.config.line.BankLine4
import com.hereliesaz.cuedetat.view.config.line.ShotGuideLine
import com.hereliesaz.cuedetat.view.config.line.TangentLine
import com.hereliesaz.cuedetat.view.config.ui.ProtractorGuides
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
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
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, obstructionPaint)
        if (state.aimingLineBankPath.isNotEmpty()) {
            drawPath(canvas, state.aimingLineBankPath, obstructionPaint)
        } else {
            drawExtendedLine(canvas, ghostCueCenter, state.aimingLineEndPoint ?: state.protractorUnit.center, obstructionPaint)
        }

        // --- Pass 2: Glows ---
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLineGlow)
        drawTangentLines(canvas, state, paints)

        // --- Pass 3: Core Lines ---
        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLinePaint)

        // Draw Aiming Line (Banked or Straight)
        drawAimingLines(canvas, state, paints)

        // Draw Spin Paths
        drawSpinPaths(canvas, state, paints)


        // --- Pass 4: Diamond Labels for Bank Points ---
        if (state.showTable) {
            val textPaint = paints.textPaint.apply { this.typeface = typeface }
            state.aimingLineBankPath.getOrNull(1)?.let { bankPoint ->
                getRailForPoint(bankPoint, state)?.let { railType ->
                    textRenderer.drawDiamondLabel(canvas, bankPoint, railType, state, textPaint)
                }
            }
            state.shotGuideImpactPoint?.let { impactPoint ->
                getRailForPoint(impactPoint, state)?.let { railType ->
                    textRenderer.drawDiamondLabel(canvas, impactPoint, railType, state, textPaint)
                }
            }
            state.tangentLineBankPath.getOrNull(1)?.let { tangentBankPoint ->
                getRailForPoint(tangentBankPoint, state)?.let { railType ->
                    textRenderer.drawDiamondLabel(canvas, tangentBankPoint, railType, state, textPaint)
                }
            }
        }


        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawAimingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val aimingLineConfig = AimingLine()
        val isDirectPocketed = state.aimedPocketIndex != null

        val baseAimingColor = if(isDirectPocketed && state.aimingLineBankPath.isEmpty()) RebelYellow else aimingLineConfig.strokeColor

        val aimingLinePaint = Paint(paints.targetCirclePaint).apply {
            color = baseAimingColor.toArgb()
            strokeWidth = aimingLineConfig.strokeWidth
        }
        val aimingLineGlow = Paint(paints.lineGlowPaint).apply {
            color = aimingLinePaint.color
            strokeWidth = aimingLineConfig.glowWidth
            alpha = (aimingLineConfig.glowColor.alpha * 255).toInt()
        }

        if (state.aimingLineBankPath.isNotEmpty()) {
            drawPath(canvas, state.aimingLineBankPath, aimingLineGlow)
            drawPath(canvas, state.aimingLineBankPath, aimingLinePaint)
        } else {
            drawExtendedLine(canvas, state.protractorUnit.ghostCueBallCenter, state.protractorUnit.center, aimingLineGlow)
            drawExtendedLine(canvas, state.protractorUnit.ghostCueBallCenter, state.protractorUnit.center, aimingLinePaint)
        }
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

        if (state.tangentLineBankPath.isNotEmpty()) {
            drawPath(canvas, state.tangentLineBankPath, tangentGlow)
            drawPath(canvas, state.tangentLineBankPath, tangentSolidPaint)
        } else {
            drawExtendedLine(canvas, state.protractorUnit.ghostCueBallCenter, state, state.tangentDirection, tangentGlow)
            drawExtendedLine(canvas, state.protractorUnit.ghostCueBallCenter, state, state.tangentDirection, tangentSolidPaint)
        }

        // Inactive tangent line no longer banks
        drawExtendedLine(canvas, state.protractorUnit.ghostCueBallCenter, state, -state.tangentDirection, tangentDottedPaint)
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
            drawAngleGuide(canvas, center, referencePoint, -angle, guidePaint)
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

        val tolerance = 5f

        return when {
            abs(point.y - top) < tolerance -> LineTextRenderer.RailType.TOP
            abs(point.y - bottom) < tolerance -> LineTextRenderer.RailType.BOTTOM
            abs(point.x - left) < tolerance -> LineTextRenderer.RailType.LEFT
            abs(point.x - right) < tolerance -> LineTextRenderer.RailType.RIGHT
            else -> null
        }
    }

    fun drawBankingLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?){
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

    private fun drawExtendedLine(canvas: Canvas, start: PointF, state: OverlayState, direction: Float, paint: Paint) {
        val towards = state.protractorUnit.center
        val dxToTarget = towards.x - start.x
        val dyToTarget = towards.y - start.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget > 0.001f) {
            val tangentDx = -dyToTarget / magToTarget
            val tangentDy = dxToTarget / magToTarget
            val extendFactor = 5000f

            canvas.drawLine(start.x, start.y, start.x + tangentDx * extendFactor * direction, start.y + tangentDy * extendFactor * direction, paint)
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