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
import androidx.core.graphics.withMatrix
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
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
import kotlin.math.hypot

class BallRenderer {

    private val textRenderer = BallTextRenderer()

    fun draw(canvas: Canvas, state: CueDetatState, paints: PaintCache, typeface: Typeface?) {
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

    private fun drawBoundingBoxes(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
        val visionData = state.visionData ?: return
        if (visionData.detectedBoundingBoxes.isEmpty() || visionData.sourceImageWidth == 0) return

        val paint = Paint(paints.cvResultPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 150
        }

        val rotation = visionData.sourceImageRotation.toFloat()
        val srcWidth = visionData.sourceImageWidth.toFloat()
        val srcHeight = visionData.sourceImageHeight.toFloat()
        val canvasWidth = canvas.width.toFloat()
        val canvasHeight = canvas.height.toFloat()

        val matrix = Matrix()
        val srcRect = RectF(0f, 0f, srcWidth, srcHeight)
        val destRect = RectF(0f, 0f, canvasWidth, canvasHeight)
        matrix.setRectToRect(srcRect, destRect, Matrix.ScaleToFit.FILL)
        matrix.postRotate(rotation, canvasWidth / 2f, canvasHeight / 2f)


        visionData.detectedBoundingBoxes.forEach { box ->
            val boxRect = RectF(box)
            matrix.mapRect(boxRect)
            canvas.drawRect(boxRect, paint)
        }
    }


    private fun drawProtractorAndActual(canvas: Canvas, state: CueDetatState, paints: PaintCache) {
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

    private fun drawGhostedBall(
        canvas: Canvas,
        ball: LogicalCircular,
        config: BallsConfig,
        state: CueDetatState,
        paints: PaintCache
    ) {
        val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked

        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode,
            state.isBeginnerViewLocked
        )
        val zoomFactor = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom)

        // ONLY in Static Beginner Mode, force TargetBall to match GhostCueBall's exact stroke width
        val effectiveStrokeWidth = if (isBeginnerLocked && config is TargetBall) {
            GhostCueBall().strokeWidth
        } else {
            config.strokeWidth
        }

        val logicalStrokePaint = Paint(paints.targetCirclePaint).apply {
            strokeWidth = effectiveStrokeWidth / zoomFactor
            color = config.strokeColor.toArgb()
            alpha = (config.opacity * 255).toInt()
            style = Paint.Style.STROKE
        }
        val dotPaint = Paint(paints.fillPaint).apply { color = android.graphics.Color.WHITE }
        val dotRadius = ball.radius * 0.1f

        if (isBeginnerLocked) {
            val logicalBallMatrix = state.logicalPlaneMatrix ?: return
            val logicalScreenPos = DrawingUtils.mapPoint(ball.center, logicalBallMatrix)

            // EXACT SIZE MATCH: Calculate on-screen radius identically to keep bubble and stroke perfectly synchronized
            val mappedEdge = DrawingUtils.mapPoint(PointF(ball.center.x + ball.radius, ball.center.y), logicalBallMatrix)
            val exactScreenRadius = hypot((mappedEdge.x - logicalScreenPos.x).toDouble(), (mappedEdge.y - logicalScreenPos.y).toDouble()).toFloat()

            // To draw the stroke on the INSIDE of the target ball, subtract half the stroke width from the radius
            val logicalInnerRadius = if (config is TargetBall) {
                ball.radius - (effectiveStrokeWidth / zoomFactor / 2f)
            } else {
                ball.radius
            }

            canvas.withMatrix(logicalBallMatrix) {
                drawCircle(ball.center.x, ball.center.y, logicalInnerRadius, logicalStrokePaint)
                drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
            }

            val sensitivity = 2.5f
            // INVERTED math: negative roll and positive pitch so it floats upward against gravity like an air bubble
            val screenOffsetX = -state.currentOrientation.roll * sensitivity
            val screenOffsetY = state.currentOrientation.pitch * sensitivity

            val offsetDistance = hypot(screenOffsetX, screenOffsetY)
            val finalOffsetX: Float
            val finalOffsetY: Float

            // Clamp bubble to the EXACT matched screen radius
            if (offsetDistance > exactScreenRadius) {
                val scale = exactScreenRadius / offsetDistance
                finalOffsetX = screenOffsetX * scale
                finalOffsetY = screenOffsetY * scale
            } else {
                finalOffsetX = screenOffsetX
                finalOffsetY = screenOffsetY
            }

            val bubbleCenter = PointF(logicalScreenPos.x + finalOffsetX, logicalScreenPos.y + finalOffsetY)

            val strokePaint = Paint(paints.targetCirclePaint).apply {
                color = config.strokeColor.toArgb()
                strokeWidth = effectiveStrokeWidth
                alpha = (config.opacity * 255).toInt()
                style = Paint.Style.STROKE
            }
            val glowPaint = createGlowPaint(config.glowColor, config.glowWidth, state, paints)

            // To draw the stroke on the INSIDE of the target ball in screen space, offset the screen radius
            val screenInnerRadius = if (config is TargetBall) {
                exactScreenRadius - (effectiveStrokeWidth / 2f)
            } else {
                exactScreenRadius
            }

            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, exactScreenRadius, glowPaint) // Keep glow bleeding outward from the true edge
            canvas.drawCircle(bubbleCenter.x, bubbleCenter.y, screenInnerRadius, strokePaint)

        } else {
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
                style = Paint.Style.STROKE
            }
            val glowPaint = createGlowPaint(
                baseGlowColor = if (isWarning) Color(paints.warningPaint.color) else config.glowColor,
                baseGlowWidth = config.glowWidth,
                state = state,
                paints = paints
            )

            canvas.save()
            canvas.concat(logicalBallMatrix)
            canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, logicalStrokePaint)
            canvas.drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
            canvas.restore()

            canvas.drawCircle(
                logicalScreenPos.x,
                yPosLifted,
                radiusInfo.radius,
                glowPaint
            )
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

    private fun drawAllLabels(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?
    ) {
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

            val isBeginnerLocked = state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked
            val ghostCueText = if (isBeginnerLocked) {
                "Aim the cue ball at\nthe center of the blue circle."
            } else {
                "Ghost Cue Ball"
            }

            textRenderer.draw(canvas, textPaint, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, ghostCueText, LabelConfig.ghostCueBall, state, drawBelow = true)
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