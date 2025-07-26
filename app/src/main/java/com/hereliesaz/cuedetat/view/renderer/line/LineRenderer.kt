// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/line/LineRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.ui.theme.WarningRed
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.line.AimingLine
import com.hereliesaz.cuedetat.view.config.line.BankLine1
import com.hereliesaz.cuedetat.view.config.line.BankLine2
import com.hereliesaz.cuedetat.view.config.line.BankLine3
import com.hereliesaz.cuedetat.view.config.line.BankLine4
import com.hereliesaz.cuedetat.view.config.line.ShotGuideLine
import com.hereliesaz.cuedetat.view.config.line.TangentLine
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.config.ui.ProtractorGuides
import com.hereliesaz.cuedetat.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LineRenderer {
    private val textRenderer = LineTextRenderer()
    private val protractorAngles = floatArrayOf(10f, 20f, 30f, 40f, 50f, 60f, 70f, 80f)
    private val lineExtensionFactor = 5000f

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
        val shotLineAnchor = state.shotLineAnchor ?: return
        val ghostCueCenter = state.protractorUnit.ghostCueBallCenter
        val shotGuideLineConfig = ShotGuideLine()

        val shotLineIsWarning = state.isGeometricallyImpossible || state.isTiltBeyondLimit || state.isObstructed
        val shotLinePaint = Paint(paints.shotLinePaint).apply {
            color =
                if (shotLineIsWarning) paints.warningPaint.color else shotGuideLineConfig.strokeColor.toArgb()
            strokeWidth = shotGuideLineConfig.strokeWidth
        }
        val shotLineGlow = Paint(paints.lineGlowPaint).apply {
            // Explicitly set warning color, overriding the global glow stick color.
            color = if (shotLineIsWarning) paints.warningPaint.color else shotLinePaint.color
            strokeWidth = shotGuideLineConfig.glowWidth
        }
        val obstructionPaint = Paint(paints.pathObstructionPaint).apply {
            strokeWidth = state.protractorUnit.radius * 2
        }

        val shotGuideDirection = normalize(PointF(ghostCueCenter.x - shotLineAnchor.x, ghostCueCenter.y - shotLineAnchor.y))

        // Only draw the wide pathways if there's something to obstruct
        if (state.table.isVisible || state.obstacleBalls.isNotEmpty()) {
            // --- Pass 1: Wide Pathways ---
            if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
                // Do not draw shot guide line pathway in locked beginner mode
            } else {
                drawFadingLine(
                    canvas,
                    shotLineAnchor,
                    shotGuideDirection,
                    obstructionPaint,
                    null, // No glow for pathway
                    state,
                    paints
                )
            }


            state.aimingLineBankPath?.let {
                drawBankablePath(
                    canvas,
                    it,
                    obstructionPaint,
                    null, // No glow for pathway
                    isPocketed = false,
                    state,
                    paints
                )
            }
        }


        // --- Pass 2 & 3: Glows and Core Lines (Combined) ---
        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            // Do not draw shot guide line in locked beginner mode
        } else {
            drawFadingLine(
                canvas,
                shotLineAnchor,
                shotGuideDirection,
                shotLinePaint,
                shotLineGlow,
                state,
                paints
            )
        }
        drawTangentLines(canvas, state, paints)
        drawAimingLines(canvas, state, paints)
        drawSpinPaths(canvas, state, paints)

        if (state.areHelpersVisible) {
            textRenderer.drawProtractorLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawAimingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val aimingLineConfig = AimingLine()
        val isPocketed = state.aimedPocketIndex != null

        val baseAimingColor = if (isPocketed) SulfurDust else aimingLineConfig.strokeColor

        val aimingLinePaint = Paint(paints.targetCirclePaint).apply {
            color = baseAimingColor.toArgb()
            strokeWidth = aimingLineConfig.strokeWidth
        }
        val aimingLineGlow = Paint(paints.lineGlowPaint).apply {
            color = aimingLineConfig.glowColor.toArgb()
            strokeWidth = aimingLineConfig.glowWidth
            alpha = (aimingLineConfig.glowColor.alpha * 255).toInt()
        }

        state.aimingLineBankPath?.let {
            drawBankablePath(
                canvas,
                it,
                aimingLinePaint,
                aimingLineGlow,
                isPocketed,
                state,
                paints
            )
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

        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            val direction1 = normalize(PointF(tangentDx, tangentDy))
            val endPoint1 = PointF(
                start.x + direction1.x * (state.table.logicalHeight * 2.0f),
                start.y + direction1.y * (state.table.logicalHeight * 2.0f)
            )
            val path1 = Path().apply { moveTo(start.x, start.y); lineTo(endPoint1.x, endPoint1.y) }
            canvas.drawPath(path1, tangentGlow)
            canvas.drawPath(path1, tangentSolidPaint)

            val direction2 = normalize(PointF(-tangentDx, -tangentDy))
            val endPoint2 = PointF(
                start.x + direction2.x * (state.table.logicalHeight * 2.0f),
                start.y + direction2.y * (state.table.logicalHeight * 2.0f)
            )
            val path2 = Path().apply { moveTo(start.x, start.y); lineTo(endPoint2.x, endPoint2.y) }
            canvas.drawPath(path2, tangentGlow)
            canvas.drawPath(path2, tangentSolidPaint)
            return
        }

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
            // Active / Inactive logic
            state.tangentLineBankPath?.let {
                drawBankablePath(canvas, it, tangentSolidPaint, tangentGlow, isPocketed, state, paints)
            }

            // Draw inactive line manually as a non-fading dotted line
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
        val paths = state.spinPaths ?: return
        if (paths.isEmpty()) return

        val alpha = (255 * state.spinPathsAlpha).toInt()
        val spinPathPaint = Paint(paints.shotLinePaint).apply {
            strokeWidth = 4f
        }

        val spinGlowPaint = Paint(paints.lineGlowPaint).apply {
            strokeWidth = 8f
        }

        paths.forEach { (color, points) ->
            if (points.size < 2) return@forEach

            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            spinPathPaint.color = color.toArgb()
            spinPathPaint.alpha = alpha
            spinGlowPaint.color = color.toArgb()
            spinGlowPaint.alpha =
                (color.alpha * 100 * state.spinPathsAlpha).toInt().coerceIn(0, 255)

            canvas.drawPath(path, spinGlowPaint)
            canvas.drawPath(path, spinPathPaint)
        }
    }

    private fun drawBankingLines(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val path = state.bankShotPath ?: return
        if (path.size < 2) return

        val isPocketed = state.pocketedBankShotPocketIndex != null

        if (isPocketed) {
            val whitePaint = Paint(paints.shotLinePaint).apply { color = Color.White.toArgb() }
            val whiteGlowPaint = Paint(paints.lineGlowPaint).apply {
                color = Color.White.toArgb(); alpha = (paints.lineGlowPaint.alpha * 0.7f).toInt()
            }
            drawPath(canvas, path, whiteGlowPaint)
            drawPath(canvas, path, whitePaint)
        } else {
            val bankLineConfigs = listOf(BankLine1(), BankLine2(), BankLine3(), BankLine4())
            for (i in 0 until path.size - 1) {
                val start = path[i]
                val end = path[i + 1]
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
        val labelConfig = LabelConfig.angleGuide

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
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, angle, guidePaint, state, paints)
            drawAngleGuide(canvas, ghostCueCenter, targetCenter, -angle, guidePaint, state, paints)
            if (state.areHelpersVisible || labelConfig.isPersistentlyVisible) {
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, angle, textPaint, state.protractorUnit.radius)
                textRenderer.drawAngleLabel(canvas, ghostCueCenter, targetCenter, -angle, textPaint, state.protractorUnit.radius)
            }
        }
    }

    private fun drawBankablePath(
        canvas: Canvas,
        path: List<PointF>,
        primaryPaint: Paint,
        glowPaint: Paint?,
        isPocketed: Boolean,
        state: OverlayState,
        paints: PaintCache
    ) {
        if (path.size < 2) return

        val finalSegmentIndex = path.size - 2

        for (i in 0..finalSegmentIndex) {
            val start = path[i]
            val end = path[i + 1]
            val isLastSegment = i == finalSegmentIndex

            if (isLastSegment) {
                val direction = normalize(PointF(end.x - start.x, end.y - start.y))
                drawFadingLine(canvas, start, direction, primaryPaint, glowPaint, state, paints)
            } else { // For intermediate banked segments
                glowPaint?.let { canvas.drawLine(start.x, start.y, end.x, end.y, it) }
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


    private fun drawAngleGuide(canvas: Canvas, center: PointF, referencePoint: PointF, angleDegrees: Float, paint: Paint, state: OverlayState, paints: PaintCache) {
        val initialAngleRad = atan2(referencePoint.y - center.y, referencePoint.x - center.x)
        val finalAngleRad = initialAngleRad + Math.toRadians(angleDegrees.toDouble())

        val direction = normalize(PointF(cos(finalAngleRad).toFloat(), sin(finalAngleRad).toFloat()))
        drawFadingLine(canvas, center, direction, paint, null, state, paints)
    }

    private fun drawFadingLine(
        canvas: Canvas,
        start: PointF,
        direction: PointF,
        paint: Paint,
        glowPaint: Paint?,
        state: OverlayState,
        paints: PaintCache
    ) {
        val tableLength = state.table.logicalHeight
        val totalLength = tableLength * 2.0f
        val fadeStartDistance = tableLength * 1.2f
        totalLength

        val end = PointF(start.x + direction.x * totalLength, start.y + direction.y * totalLength)
        val fadeStart = PointF(start.x + direction.x * fadeStartDistance, start.y + direction.y * fadeStartDistance)

        val layer = canvas.saveLayer(null, null)
        try {
            // 1. Draw the full, opaque line(s)
            glowPaint?.let { canvas.drawLine(start.x, start.y, end.x, end.y, it) }
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)

            // 2. Create and apply the gradient mask
            val gradient = LinearGradient(
                fadeStart.x, fadeStart.y,
                end.x, end.y,
                intArrayOf(paint.color, Color.Transparent.toArgb()),
                null,
                Shader.TileMode.CLAMP
            )
            paints.gradientMaskPaint.shader = gradient
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paints.gradientMaskPaint)
        } finally {
            paints.gradientMaskPaint.shader = null // Reset the shader
            canvas.restoreToCount(layer)
        }
    }

    private fun normalize(p: PointF): PointF {
        val mag = kotlin.math.hypot(p.x.toDouble(), p.y.toDouble()).toFloat()
        return if (mag > 0.001f) PointF(p.x / mag, p.y / mag) else PointF(0f, 0f)
    }
}