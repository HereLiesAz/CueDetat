// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.ui.theme.WarningRed
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

    private fun drawProtractorLines(
        canvas: Canvas, state: OverlayState,
        paints: PaintCache, typeface: Typeface?
    ) {
        // --- Paint and Config Preparation ---
        val shotLineAnchor = state.shotLineAnchor
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotGuideLineConfig = ShotGuideLine()
        val aimingLineConfig = AimingLine()

        val shotLineIsWarning = state.isGeometricallyImpossible || state.isTiltBeyondLimit || state.isObstructed
        val shotLineBaseColor = if (shotLineIsWarning) WarningRed.toArgb() else shotGuideLineConfig.strokeColor.toArgb()

        val isAimingLinePocketed = state.aimedPocketIndex != null
        val aimingLineBaseColor = if(isAimingLinePocketed) RebelYellow.toArgb() else aimingLineConfig.strokeColor.toArgb()

        val shotLinePaint = Paint(paints.shotLinePaint).apply { color = shotLineBaseColor; strokeWidth = shotGuideLineConfig.strokeWidth }
        val shotLineGlow = Paint(paints.lineGlowPaint).apply { color = shotLineBaseColor; strokeWidth = shotGuideLineConfig.glowWidth }
        val aimingLinePaint = Paint(paints.targetCirclePaint).apply { color = aimingLineBaseColor; strokeWidth = aimingLineConfig.strokeWidth }
        val aimingLineGlow = Paint(paints.lineGlowPaint).apply { color = aimingLineBaseColor; strokeWidth = aimingLineConfig.glowWidth }


        val shotGuideDirection = normalize(PointF(ghostCueCenter.x - shotLineAnchor.x, ghostCueCenter.y - shotLineAnchor.y))

        // --- Pass 1: Wide Pathways ---
        val shotGuidePathPaint = Paint(paints.pathObstructionPaint).apply {
            strokeWidth = state.protractorUnit.radius * 2
            color = shotGuideLineConfig.obstaclePathColor.toArgb()
            alpha = (shotGuideLineConfig.obstaclePathOpacity * 255).toInt()
        }
        val aimingLinePathPaint = Paint(paints.pathObstructionPaint).apply {
            strokeWidth = state.protractorUnit.radius * 2
            color = aimingLineConfig.obstaclePathColor.toArgb()
            alpha = (aimingLineConfig.obstaclePathOpacity * 255).toInt()
        }

        drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, shotGuidePathPaint, shotGuidePathPaint, state)
        drawBankablePath(canvas, state.aimingLineBankPath, aimingLinePathPaint, aimingLinePathPaint, false, state)


        // --- Pass 2: Glows ---
        drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, shotLineGlow, shotLineGlow, state)
        drawTangentLines(canvas, state, paints)

        // --- Pass 3: Core Lines ---
        drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, shotLinePaint, shotLinePaint, state)
        drawBankablePath(canvas, state.aimingLineBankPath, aimingLinePaint, aimingLineGlow, isAimingLinePocketed, state)
        drawSpinPaths(canvas, state, paints)

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawTangentLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val tangentLineConfig = TangentLine()
        val isPocketed = state.tangentAimedPocketIndex != null
        val baseTangentColor = if (isPocketed) WarningRed else tangentLineConfig.strokeColor

        val tangentSolidPaint = Paint(paints.tangentLineSolidPaint).apply {
            color = baseTangentColor.toArgb()
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

        val start = state.protractorUnit.ghostCueBallCenter
        val towards = state.protractorUnit.center
        val dxToTarget = towards.x - start.x
        val dyToTarget = towards.y - start.y
        val magToTarget = sqrt(dxToTarget * dxToTarget + dyToTarget * dyToTarget)

        if (magToTarget < 0.001f) return

        val tangentDx = -dyToTarget / magToTarget
        val tangentDy = dxToTarget / magToTarget

        if (state.isStraightShot) {
            val direction1 = normalize(PointF(tangentDx, tangentDy))
            val endPoint1 = PointF(start.x + direction1.x * (state.table.logicalHeight * 2.0f), start.y + direction1.y * (state.table.logicalHeight * 2.0f))
            val path1 = Path().apply { moveTo(start.x, start.y); lineTo(endPoint1.x, endPoint1.y) }
            canvas.drawPath(path1, tangentGlow)
            canvas.drawPath(path1, tangentDottedPaint)

            val direction2 = normalize(PointF(-tangentDx, -tangentDy))
            val endPoint2 = PointF(start.x + direction2.x * (state.table.logicalHeight * 2.0f), start.y + direction2.y * (state.table.logicalHeight * 2.0f))
            val path2 = Path().apply { moveTo(start.x, start.y); lineTo(endPoint2.x, endPoint2.y) }
            canvas.drawPath(path2, tangentGlow)
            canvas.drawPath(path2, tangentDottedPaint)
        } else {
            drawBankablePath(canvas, state.tangentLineBankPath, tangentSolidPaint, tangentGlow, isPocketed, state)
            val inactiveDirection = normalize(PointF(tangentDx * -state.tangentDirection, tangentDy * -state.tangentDirection))
            val endPoint = PointF(start.x + inactiveDirection.x * (state.table.logicalHeight * 2.0f), start.y + inactiveDirection.y * (state.table.logicalHeight * 2.0f))
            val path = Path()
            path.moveTo(start.x, start.y)
            path.lineTo(endPoint.x, endPoint.y)
            canvas.drawPath(path, tangentGlow)
            canvas.drawPath(path, tangentDottedPaint)
        }
    }

    private fun drawSpinPaths(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.spinPaths.isEmpty()) return
        val alpha = (255 * state.spinPathsAlpha).toInt()
        val spinPathPaint = Paint(paints.shotLinePaint).apply { strokeWidth = 4f }
        val spinGlowPaint = Paint(paints.lineGlowPaint).apply { strokeWidth = 8f }

        state.spinPaths.forEach { (color, points) ->
            if (points.size < 2) return@forEach
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) { path.lineTo(points[i].x, points[i].y) }
            spinPathPaint.color = color.toArgb(); spinPathPaint.alpha = alpha
            spinGlowPaint.color = color.toArgb(); spinGlowPaint.alpha = (color.alpha * 100 * state.spinPathsAlpha).toInt().coerceIn(0, 255)
            canvas.drawPath(path, spinGlowPaint); canvas.drawPath(path, spinPathPaint)
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        if (state.bankShotPath.size < 2) return
        if (state.pocketedBankShotPocketIndex != null) {
            val whitePaint = Paint(paints.shotLinePaint).apply { color = RebelYellow.toArgb() }
            val whiteGlowPaint = Paint(paints.lineGlowPaint).apply { color = RebelYellow.toArgb(); alpha = (paints.lineGlowPaint.alpha * 0.7f).toInt() }
            drawPath(canvas, state.bankShotPath, whiteGlowPaint)
            drawPath(canvas, state.bankShotPath, whitePaint)
        } else {
            val bankLineConfigs = listOf(BankLine1(), BankLine2(), BankLine3(), BankLine4())
            for (i in 0 until state.bankShotPath.size - 1) {
                val start = state.bankShotPath[i]; val end = state.bankShotPath[i+1]
                val config = bankLineConfigs.getOrElse(i) { bankLineConfigs.last() }
                val linePaint = Paint(paints.bankLine1Paint).apply { color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth }
                val glowPaint = Paint(paints.lineGlowPaint).apply {
                    color = linePaint.color; alpha = (config.glowColor.alpha * 255).toInt(); strokeWidth = config.glowWidth
                }
                canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
                canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
            }
        }
    }

    private fun drawProtractorGuides(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter; val targetCenter = state.protractorUnit.center
        val config = ProtractorGuides()
        val guidePaint = Paint(paints.angleGuidePaint).apply {
            color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth; alpha = (config.opacity * 255).toInt()
        }
        val textPaint = Paint(paints.textPaint).apply {
            alpha = (config.opacity * 255).toInt(); textSize = 30f
        }
        protractorAngles.forEach { angle ->
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, angle, guidePaint, state)
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, -angle, guidePaint, state)
            if (state.areHelpersVisible) {
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, angle, textPaint, state.protractorUnit.radius)
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, -angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun drawBankablePath(canvas: Canvas, path: List<PointF>, primaryPaint: Paint, glowPaint: Paint, isPocketed: Boolean, state: OverlayState) {
        if (path.size < 2) return
        val finalSegmentIndex = path.size - 2
        for (i in 0..finalSegmentIndex) {
            val start = path[i]; val end = path[i+1]
            if (i == finalSegmentIndex) {
                val direction = normalize(PointF(end.x - start.x, end.y - start.y))
                drawFadingLine(canvas, start, direction, primaryPaint, glowPaint, state)
            } else {
                canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
                canvas.drawLine(start.x, start.y, end.x, end.y, primaryPaint)
            }
        }
    }

    private fun drawPath(canvas: Canvas, path: List<PointF>, paint: Paint) {
        if (path.size < 2) return
        for (i in 0 until path.size - 1) {
            val start = path[i]; val end = path[i + 1]
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }

    private fun drawAngleGuide(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, state: OverlayState) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())
        val direction = normalize(PointF(cos(finalAngleRad).toFloat(), sin(finalAngleRad).toFloat()))
        drawFadingLine(canvas, center, direction, paint, paint, state)
    }

    private fun drawFadingLine(canvas: Canvas, start: PointF, direction: PointF, paint: Paint, glowPaint: Paint, state: OverlayState) {
        val tableLength = state.table.logicalHeight
        val totalLength = tableLength * 2.0f
        if (totalLength <= 0f) return

        val end = PointF(start.x + direction.x * totalLength, start.y + direction.y * totalLength)
        val fadeStartFraction = 1.2f / 2.0f // Fade starts at 1.2 table lengths out of a total 2.0

        val transparentColor = android.graphics.Color.TRANSPARENT

        // Create and apply gradient to the primary paint
        val primaryShader = LinearGradient(
            start.x, start.y, end.x, end.y,
            intArrayOf(paint.color, paint.color, transparentColor),
            floatArrayOf(0f, fadeStartFraction, 1.0f),
            Shader.TileMode.CLAMP
        )
        val gradientPaint = Paint(paint).apply { shader = primaryShader }
        canvas.drawLine(start.x, start.y, end.x, end.y, gradientPaint)

        // Create and apply gradient to the glow paint
        val glowShader = LinearGradient(
            start.x, start.y, end.x, end.y,
            intArrayOf(glowPaint.color, glowPaint.color, transparentColor),
            floatArrayOf(0f, fadeStartFraction, 1.0f),
            Shader.TileMode.CLAMP
        )
        val gradientGlowPaint = Paint(glowPaint).apply { shader = glowShader }
        canvas.drawLine(start.x, start.y, end.x, end.y, gradientGlowPaint)
    }

    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }
}