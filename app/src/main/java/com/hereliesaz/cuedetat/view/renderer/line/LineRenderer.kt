// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.config.line.*
import com.hereliesaz.cuedetat.view.config.ui.ProtractorGuides
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer @Inject constructor(private val textRenderer: LineTextRenderer) {
    private val protractorAngles = floatArrayOf(5f, 10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f)

    fun drawLogicalLines(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        if (state.isBankingMode) {
            drawBankingLines(canvas, state)
        } else {
            drawProtractorLines(canvas, state, typeface)
            drawProtractorGuides(canvas, state, typeface)
        }
    }

    fun drawRailLabels(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        if (!state.table.isVisible) return
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface }

        if (state.aimingLineBankPath.size > 1) {
            val bankPoint = state.aimingLineBankPath[1]
            textRenderer.getRailForPoint(bankPoint, state)?.let { railType ->
                textRenderer.drawDiamondLabel(canvas, bankPoint, railType, state, textPaint)
            }
        }
        if (state.tangentLineBankPath.size > 1) {
            val tangentBankPoint = state.tangentLineBankPath[1]
            textRenderer.getRailForPoint(tangentBankPoint, state)?.let { railType ->
                textRenderer.drawDiamondLabel(canvas, tangentBankPoint, railType, state, textPaint)
            }
        }
        state.shotGuideImpactPoint?.let { impactPoint ->
            textRenderer.getRailForPoint(impactPoint, state)?.let { railType ->
                textRenderer.drawDiamondLabel(canvas, impactPoint, railType, state, textPaint)
            }
        }
    }

    private fun drawProtractorLines(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        val shotLineAnchor = state.shotLineAnchor
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotGuideLineConfig = ShotGuideLine()

        val shotLineIsWarning = state.isGeometricallyImpossible || state.isTiltBeyondLimit || state.isObstructed || state.tangentAimedPocketIndex != null
        val shotLinePaint = shotGuideLineConfig.getLinePaint(state.luminanceAdjustment, shotLineIsWarning)
        val shotLineGlow = shotGuideLineConfig.getGlowPaint(state.glowStickValue)
        val obstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = state.protractorUnit.radius * 2
            color = shotGuideLineConfig.strokeColor.copy(alpha = 0.2f).toArgb()
        }

        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, obstructionPaint)
        drawPath(canvas, state.aimingLineBankPath, obstructionPaint)

        shotLineGlow?.let { drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, it) }
        drawTangentLines(canvas, state)

        drawExtendedLine(canvas, shotLineAnchor, ghostCueCenter, shotLinePaint)
        drawAimingLines(canvas, state)
        drawSpinPaths(canvas, state)

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, typeface)
        }
    }

    private fun drawAimingLines(canvas: Canvas, state: OverlayState) {
        val aimingLineConfig = AimingLine()
        val isPocketed = state.aimedPocketIndex != null

        val aimingLinePaint = aimingLineConfig.getLinePaint(state.luminanceAdjustment, isWarning = false).apply {
            if (isPocketed) color = RebelYellow.toArgb()
        }
        val aimingLineGlow = aimingLineConfig.getGlowPaint(state.glowStickValue)?.apply {
            if (isPocketed) color = RebelYellow.toArgb()
        }

        aimingLineGlow?.let { drawPath(canvas, state.aimingLineBankPath, it) }
        drawPath(canvas, state.aimingLineBankPath, aimingLinePaint)
    }

    private fun drawTangentLines(canvas: Canvas, state: OverlayState) {
        val tangentLineConfig = TangentLine()
        val tangentSolidPaint = tangentLineConfig.getLinePaint(state.luminanceAdjustment, isWarning = false)
        val tangentDottedPaint = tangentLineConfig.getDottedPaint(state.luminanceAdjustment)
        val tangentGlow = tangentLineConfig.getGlowPaint(state.glowStickValue)

        tangentGlow?.let { drawPath(canvas, state.tangentLineBankPath, it) }
        drawPath(canvas, state.tangentLineBankPath, tangentSolidPaint)

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


    private fun drawSpinPaths(canvas: Canvas, state: OverlayState) {
        if (state.spinPaths.isEmpty()) return

        val alpha = (255 * state.spinPathsAlpha).toInt()

        state.spinPaths.forEach { (color, points) ->
            if (points.size < 2) return@forEach

            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            val spinPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                this.color = color.toArgb()
                this.alpha = alpha
            }
            val spinGlowPaint = Paint(spinPathPaint).apply {
                strokeWidth = 8f
                this.alpha = (color.alpha * 0.4f * state.spinPathsAlpha).toInt().coerceIn(0, 255)
            }

            canvas.drawPath(path, spinGlowPaint)
            canvas.drawPath(path, spinPathPaint)
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState) {
        if (state.bankShotPath.size < 2) return

        val bankLineConfigs = listOf(BankLine1(), BankLine2(), BankLine3(), BankLine4())
        val lastSegmentIndex = state.bankShotPath.size - 2

        for (i in 0..lastSegmentIndex) {
            val start = state.bankShotPath[i]
            val end = state.bankShotPath[i+1]

            val isLastSegment = i == lastSegmentIndex
            val isPocketed = state.pocketedBankShotPocketIndex != null

            val config = bankLineConfigs.getOrElse(i) { bankLineConfigs.last() }
            val linePaint = config.getLinePaint(state.luminanceAdjustment, isWarning = false)

            if (isLastSegment && isPocketed) {
                linePaint.color = RebelYellow.toArgb()
            }

            val glowPaint = config.getGlowPaint(state.glowStickValue)?.apply {
                color = linePaint.color
            }

            glowPaint?.let { canvas.drawLine(start.x, start.y, end.x, end.y, it) }
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
        }
    }

    private fun drawProtractorGuides(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val targetCenter = state.protractorUnit.center
        val config = ProtractorGuides()

        val guidePaint = config.getLinePaint(state.luminanceAdjustment, false)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            color = config.strokeColor.toArgb()
            alpha = (config.opacity * 255).toInt()
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

    private fun drawExtendedLine(canvas: Canvas, start: PointF, end: PointF, paint: Paint) {
        val dirX = end.x - start.x; val dirY = end.y - start.y
        val mag = sqrt(dirX * dirX + dirY * dirY)
        if (mag > 0.001f) {
            val extendFactor = 5000f; val ndx = dirX / mag; val ndy = dirY / mag
            canvas.drawLine(start.x, start.y, start.x + ndx * extendFactor, start.y + ndy * extendFactor, paint)
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