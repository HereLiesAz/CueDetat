// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.ui.theme.SulfurDust
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.BankingBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.ObstacleBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape
import com.hereliesaz.cuedetat.view.config.ui.LabelConfig
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.util.createGlowPaint
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlin.math.hypot

class BallRenderer {

    private val textRenderer = BallTextRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            state.onPlaneBall?.let { bankingBall ->
                drawGhostedBall(canvas, bankingBall, BankingBall(), state, paints)
            }
        } else {
            drawProtractorAndActual(canvas, state, paints)
        }

        state.obstacleBalls.forEach { obstacle ->
            drawGhostedBall(canvas, obstacle, ObstacleBall(), state, paints)
        }

        drawBoundingBoxes(canvas, state, paints)

        val detectedBalls =
            (state.visionData?.genericBalls ?: emptyList()) + (state.visionData?.customBalls
                ?: emptyList())
        val snappedPaint = Paint(paints.targetCirclePaint).apply {
            color = SulfurDust.toArgb()
            style = Paint.Style.FILL
            alpha = 150
        }

        val allLogicalBalls = (listOfNotNull(state.onPlaneBall, state.protractorUnit) + state.obstacleBalls)
        allLogicalBalls.forEach { logicalBall ->
            val isSnapped = detectedBalls.any { detected ->
                hypot((logicalBall.center.x - detected.x).toDouble(), (logicalBall.center.y - detected.y).toDouble()) < 5.0
            }
            if (isSnapped) {
                state.pitchMatrix?.let { matrix ->
                    canvas.save()
                    canvas.concat(matrix)
                    canvas.drawCircle(
                        logicalBall.center.x,
                        logicalBall.center.y,
                        logicalBall.radius * 0.5f,
                        snappedPaint
                    )
                    canvas.restore()
                }
            }
        }

        drawAllLabels(canvas, state, paints, typeface)
    }

    private fun drawBoundingBoxes(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val visionData = state.visionData ?: return
        if (visionData.detectedBoundingBoxes.isEmpty() || visionData.sourceImageWidth == 0) return

        val paint = Paint(paints.cvResultPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 150
        }

        // Create the transformation matrix on the fly, now that we have the source dimensions.
        val imageToScreenMatrix = Matrix()
        val sx = canvas.width.toFloat() / visionData.sourceImageWidth.toFloat()
        val sy = canvas.height.toFloat() / visionData.sourceImageHeight.toFloat()
        imageToScreenMatrix.postScale(sx, sy)

        canvas.save()
        canvas.concat(imageToScreenMatrix)
        visionData.detectedBoundingBoxes.forEach { box ->
            // The box coordinates are from the un-rotated image. We must account for this.
            val rotatedBox = RectF(box)
            // Note: CameraX rotation is relative to portrait. If the sensor is landscape, a 90-degree
            // rotation is applied for portrait display. The bounding box coordinates are relative to
            // that un-rotated landscape image. For this simple debug view, a direct transform is sufficient.
            canvas.drawRect(rotatedBox, paint)
        }
        canvas.restore()
    }


    private fun drawProtractorAndActual(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val protractor = state.protractorUnit

        drawGhostedBall(canvas, protractor, TargetBall(), state, paints)

        drawGhostedBall(canvas, object : LogicalCircular {
            override val center = protractor.ghostCueBallCenter
            override val radius = protractor.radius
        }, GhostCueBall(), state, paints)

        state.onPlaneBall?.let {
            drawGhostedBall(canvas, it, ActualCueBall(), state, paints)
        }
    }

    private fun drawGhostedBall(canvas: Canvas, ball: LogicalCircular, config: BallsConfig, state: OverlayState, paints: PaintCache) {
        // Shared paint setup
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom)
        val logicalStrokePaint = Paint(paints.targetCirclePaint).apply {
            strokeWidth = config.strokeWidth / zoomFactor
            color = config.strokeColor.toArgb()
            alpha = (config.opacity * 255).toInt()
        }
        val dotPaint = Paint(paints.fillPaint).apply { color = android.graphics.Color.WHITE }
        val dotRadius = ball.radius * 0.1f

        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            // --- BUBBLE LEVEL LOGIC ---
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return // This matrix is now flat
            val logicalScreenPos = DrawingUtils.mapPoint(ball.center, logicalBallMatrix)
            val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                ball.center,
                ball.radius,
                state,
                logicalBallMatrix
            )

            // Draw 2D component (immobile on the flat plane)
            canvas.save()
            canvas.concat(logicalBallMatrix)
            canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, logicalStrokePaint)
            canvas.drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
            canvas.restore()

            // Calculate bubble offset
            val sensitivity = 2.5f // Pixels of offset per degree of tilt
            val screenOffsetX = state.currentOrientation.roll * sensitivity
            val screenOffsetY = -state.currentOrientation.pitch * sensitivity
            val bubbleRadius = radiusInfo.radius

            // Clamp the offset to the radius of the ball
            val offsetDistance = hypot(screenOffsetX, screenOffsetY)
            val finalOffsetX: Float
            val finalOffsetY: Float

            if (offsetDistance > bubbleRadius) {
                val scale = bubbleRadius / offsetDistance
                finalOffsetX = screenOffsetX * scale
                finalOffsetY = screenOffsetY * scale
            } else {
                finalOffsetX = screenOffsetX
                finalOffsetY = screenOffsetY
            }

            val bubbleCenter =
                PointF(logicalScreenPos.x + finalOffsetX, logicalScreenPos.y + finalOffsetY)


            // Draw 3D component at the new bubble position
            val strokePaint = Paint(paints.targetCirclePaint).apply {
                color = config.strokeColor.toArgb()
                strokeWidth = config.strokeWidth
                alpha = (config.opacity * 255).toInt()
            }
            val glowPaint = createGlowPaint(config.glowColor, config.glowWidth, state)
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, bubbleRadius, glowPaint)
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, bubbleRadius, strokePaint)

        } else {
            // --- PERSPECTIVE LIFT LOGIC ---
            val positionMatrix = state.pitchMatrix ?: return
            val sizeMatrix = state.sizeCalculationMatrix ?: positionMatrix
            val logicalBallMatrix = positionMatrix

            val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(
                ball.center,
                ball.radius,
                state,
                sizeMatrix
            )
            val logicalScreenPos = DrawingUtils.mapPoint(ball.center, logicalBallMatrix)
            val yPosLifted = logicalScreenPos.y - radiusInfo.lift
            val isWarning =
                (state.isGeometricallyImpossible || state.isObstructed) && config is GhostCueBall

            val strokePaint = Paint(paints.targetCirclePaint).apply {
                color = if (isWarning) paints.warningPaint.color else config.strokeColor.toArgb()
                strokeWidth = config.strokeWidth
                alpha = (config.opacity * 255).toInt()
            }
            val glowPaint = createGlowPaint(
                baseGlowColor = if (isWarning) Color(paints.warningPaint.color) else config.glowColor,
                baseGlowWidth = config.glowWidth,
                state = state
            )

            // Draw 2D logical ball
            canvas.save()
            canvas.concat(logicalBallMatrix)
            canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, logicalStrokePaint)
            canvas.drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
            canvas.restore()

            // Draw 3D ghost ball, anchored to the 2D ball's screen position
            canvas.drawCircle(
                logicalScreenPos.x,
                yPosLifted,
                radiusInfo.radius,
                glowPaint
            ) // Glow is centered on original radius
            canvas.drawCircle(logicalScreenPos.x, yPosLifted, radiusInfo.radius, strokePaint)

            val centerPaint = Paint(paints.fillPaint).apply { color = config.centerColor.toArgb() }
            val crosshairPaint = Paint(strokePaint).apply {
                color = config.centerColor.toArgb(); strokeWidth = config.strokeWidth
            }
            val centerSize = radiusInfo.radius * config.centerSize

            when (config.centerShape) {
                CenterShape.NONE -> {}
                CenterShape.DOT -> canvas.drawCircle(
                    logicalScreenPos.x,
                    yPosLifted,
                    centerSize,
                    centerPaint
                )

                CenterShape.CROSSHAIR -> {
                    val circleRadius = centerSize * 0.4f
                    crosshairPaint.style = Paint.Style.STROKE
                    canvas.drawCircle(logicalScreenPos.x, yPosLifted, circleRadius, crosshairPaint)
                    canvas.drawLine(
                        logicalScreenPos.x + circleRadius,
                        yPosLifted,
                        logicalScreenPos.x + centerSize,
                        yPosLifted,
                        crosshairPaint
                    )
                    canvas.drawLine(
                        logicalScreenPos.x - circleRadius,
                        yPosLifted,
                        logicalScreenPos.x - centerSize,
                        yPosLifted,
                        crosshairPaint
                    )
                    canvas.drawLine(
                        logicalScreenPos.x,
                        yPosLifted + circleRadius,
                        logicalScreenPos.x,
                        yPosLifted + centerSize,
                        crosshairPaint
                    )
                    canvas.drawLine(
                        logicalScreenPos.x,
                        yPosLifted - circleRadius,
                        logicalScreenPos.x,
                        yPosLifted - centerSize,
                        crosshairPaint
                    )
                }
            }
        }
    }

    private fun drawAllLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        state.onPlaneBall?.let {
            val (label, config) = if (state.isBankingMode) {
                "Banking Ball" to LabelConfig.bankingBall
            } else {
                "Actual Cue Ball" to LabelConfig.actualCueBall
            }
            textRenderer.draw(canvas, textPaint, it, label, config, state)
        }

        if (!state.isBankingMode) {
            textRenderer.draw(
                canvas,
                textPaint,
                state.protractorUnit,
                "Target Ball",
                LabelConfig.targetBall,
                state
            )
            textRenderer.draw(canvas, textPaint, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, "Ghost Cue Ball", LabelConfig.ghostCueBall, state)
        }

        state.obstacleBalls.forEachIndexed { index, obstacle ->
            textRenderer.draw(
                canvas,
                textPaint,
                obstacle,
                "Obstacle ${index + 1}",
                LabelConfig.obstacleBall,
                state
            )
        }
    }
}