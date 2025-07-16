// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
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
        val shotLineAnchor = state.shotLineAnchor
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotGuideLineConfig = ShotGuideLine()

        val shotLineIsWarning = state.isGeometricallyImpossible || state.isTiltBeyondLimit || state.isObstructed
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

        val shotGuideDirection = normalize(PointF(ghostCueCenter.x - shotLineAnchor.x, ghostCueCenter.y - shotLineAnchor.y))

        // --- Pass 1: Wide Pathways ---
        drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, obstructionPaint, obstructionPaint, state)
        drawPath(canvas, state.aimingLineBankPath, obstructionPaint)


        // --- Pass 2: Glows ---
        drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, shotLineGlow, shotLineGlow, state)
        drawTangentLines(canvas, state, paints)

        // --- Pass 3: Core Lines ---
        drawFadingLine(canvas, shotLineAnchor, shotGuideDirection, shotLinePaint, shotLinePaint, state)
        drawAimingLines(canvas, state, paints)
        drawSpinPaths(canvas, state, paints)

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawAimingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val aimingLineConfig = AimingLine()
        val isPocketed = state.aimedPocketIndex != null

        val baseAimingColor = if(isPocketed) RebelYellow else aimingLineConfig.strokeColor

        val aimingLinePaint = Paint(paints.targetCirclePaint).apply {
            color = baseAimingColor.toArgb()
            strokeWidth = aimingLineConfig.strokeWidth
        }
        val aimingLineGlow = Paint(paints.lineGlowPaint).apply {
            color = aimingLinePaint.color
            strokeWidth = aimingLineConfig.glowWidth
            alpha = (aimingLineConfig.glowColor.alpha * 255).toInt()
        }

        drawBankablePath(canvas, state.aimingLineBankPath, aimingLinePaint, aimingLineGlow, isPocketed, state)
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
            // Both lines are inactive/dotted
            val direction1 = normalize(PointF(tangentDx, tangentDy))
            val direction2 = normalize(PointF(-tangentDx, -tangentDy))
            drawFadingLine(canvas, start, direction1, tangentDottedPaint, tangentGlow, state)
            drawFadingLine(canvas, start, direction2, tangentDottedPaint, tangentGlow, state)
        } else {
            // Active / Inactive logic
            drawBankablePath(canvas, state.tangentLineBankPath, tangentSolidPaint, tangentGlow, isPocketed, state)

            val inactiveDirection = normalize(PointF(tangentDx * -state.tangentDirection, tangentDy * -state.tangentDirection))
            drawFadingLine(canvas, start, inactiveDirection, tangentDottedPaint, tangentGlow, state)
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

        val isPocketed = state.pocketedBankShotPocketIndex != null

        if (isPocketed) {
            // If pocketed, draw all segments in white.
            val whitePaint = Paint(paints.shotLinePaint).apply { color = Color.White.toArgb() }
            val whiteGlowPaint = Paint(paints.lineGlowPaint).apply { color = Color.White.toArgb(); alpha = (paints.lineGlowPaint.alpha * 0.7f).toInt() }
            drawPath(canvas, state.bankShotPath, whiteGlowPaint)
            drawPath(canvas, state.bankShotPath, whitePaint)
        } else {
            // Otherwise, use progressive styling.
            val bankLineConfigs = listOf(BankLine1(), BankLine2(), BankLine3(), BankLine4())
            for (i in 0 until state.bankShotPath.size - 1) {
                val start = state.bankShotPath[i]
                val end = state.bankShotPath[i+1]
                val config = bankLineConfigs.getOrElse(i) { bankLineConfigs.last() }

                val linePaint = Paint(paints.bankLine1Paint).apply { color = config.strokeColor.toArgb(); strokeWidth = config.strokeWidth }
                val glowPaint = Paint(paints.lineGlowPaint).apply {
                    color = linePaint.color
                    alpha = (config.glowColor.alpha * 255).toInt()
                    strokeWidth = config.glowWidth
                }
                canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
                canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
            }
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
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, angle, guidePaint, state)
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, -angle, guidePaint, state)
            if (state.areHelpersVisible) {
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, angle, textPaint, state.protractorUnit.radius)
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, -angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun drawBankablePath(
        canvas: Canvas,
        path: List<PointF>,
        primaryPaint: Paint,
        glowPaint: Paint,
        isPocketed: Boolean,
        state: OverlayState
    ) {
        if (path.size < 2) return

        val finalSegmentIndex = path.size - 2

        for (i in 0..finalSegmentIndex) {
            val start = path[i]
            val end = path[i+1]
            val isLastSegment = i == finalSegmentIndex

            if (isLastSegment) {
                val direction = normalize(PointF(end.x - start.x, end.y - start.y))
                drawFadingLine(canvas, start, direction, primaryPaint, glowPaint, state)
            } else { // For intermediate banked segments
                canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
                canvas.drawLine(start.x, start.y, end.x, end.y, primaryPaint)
            }
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


    private fun drawAngleGuide(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, state: OverlayState) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())

        val direction = normalize(PointF(cos(finalAngleRad).toFloat(), sin(finalAngleRad).toFloat()))
        drawFadingLine(canvas, center, direction, paint, paint, state)
    }

    private fun drawFadingLine(
        canvas: Canvas,
        start: PointF,
        direction: PointF,
        paint: Paint,
        glowPaint: Paint,
        state: OverlayState
    ) {
        val tableLength = state.table.logicalHeight
        val totalLength = tableLength * 2.0f
        val fadeStartDistance = tableLength * 1.2f
        val fadeEndDistance = totalLength

        val segmentLength = 15f // Draw in small segments for smooth fade
        val numSegments = (totalLength / segmentLength).toInt()

        val initialAlpha = paint.alpha
        val initialGlowAlpha = glowPaint.alpha

        for (i in 0 until numSegments) {
            val segmentStartDist = i * segmentLength
            val segmentEndDist = (i + 1) * segmentLength

            if (segmentStartDist > fadeEndDistance) break

            val p1 = PointF(start.x + direction.x * segmentStartDist, start.y + direction.y * segmentStartDist)
            val p2 = PointF(start.x + direction.x * segmentEndDist, start.y + direction.y * segmentEndDist)

            // Calculate alpha based on the midpoint of the segment
            val midPointDist = (segmentStartDist + segmentEndDist) / 2f
            val alphaMultiplier = if (midPointDist < fadeStartDistance) {
                1.0f
            } else {
                // Inverse linear interpolation from 1.0 down to 0.0
                1.0f - ((midPointDist - fadeStartDistance) / (fadeEndDistance - fadeStartDistance))
            }.coerceIn(0f, 1f)

            paint.alpha = (initialAlpha * alphaMultiplier).toInt()
            glowPaint.alpha = (initialGlowAlpha * alphaMultiplier).toInt()

            // Don't draw fully transparent segments
            if (paint.alpha > 5) { // Use a small threshold
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, glowPaint)
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
            }
        }
        // Restore original alpha
        paint.alpha = initialAlpha
        glowPaint.alpha = initialGlowAlpha
    }

    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }
}