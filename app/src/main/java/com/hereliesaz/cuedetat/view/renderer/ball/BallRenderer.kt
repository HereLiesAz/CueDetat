// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt

package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
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
        Paint(paints.cvResultPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 150
        }

        // Bounding boxes are in image coordinates, not logical coordinates.
        // We need to transform them to screen coordinates.
        // The OverlayState's pitchMatrix transforms logical to screen.
        // We can't use it directly.
        // For now, this feature is disabled until a proper image-to-screen matrix is passed down.
        // A direct draw would be misaligned.
        // Example of what would be needed:
        // canvas.save()
        // canvas.concat(state.imageToScreenMatrix) // This matrix doesn't exist yet
        // state.visionData.detectedBoundingBoxes.forEach { box ->
        //     canvas.drawRect(RectF(box), paint)
        // }
        // canvas.restore()
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
        val positionMatrix = state.pitchMatrix ?: return
        val sizeMatrix = state.sizeCalculationMatrix ?: positionMatrix // Fallback to positionMatrix

        val radiusInfo =
            DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state, sizeMatrix)
        val screenPos = DrawingUtils.mapPoint(ball.center, positionMatrix)
        val yPosLifted = screenPos.y - radiusInfo.lift

        val strokePaint = Paint(paints.targetCirclePaint).apply {
            color = config.strokeColor.toArgb()
            strokeWidth = config.strokeWidth
            alpha = (config.opacity * 255).toInt()
        }
        if ((state.isGeometricallyImpossible || state.isObstructed) && config is GhostCueBall) {
            strokePaint.color = paints.warningPaint.color
        }

        val glowPaint = Paint(paints.ballGlowPaint).apply {
            strokeWidth = config.glowWidth
            color = config.glowColor.toArgb()
            alpha = (config.glowColor.alpha * 255).toInt()
        }

        val dotPaint = Paint(paints.fillPaint).apply { color = android.graphics.Color.WHITE }
        val dotRadius = ball.radius * 0.1f

        canvas.save()
        canvas.concat(positionMatrix)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius + strokePaint.strokeWidth / 2f, strokePaint)
        canvas.drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
        canvas.restore()

        val liftedRadius = radiusInfo.radius + strokePaint.strokeWidth / 2f
        canvas.drawCircle(screenPos.x, yPosLifted, liftedRadius, glowPaint)
        canvas.drawCircle(screenPos.x, yPosLifted, liftedRadius, strokePaint)

        val centerPaint = Paint(paints.fillPaint).apply { color = config.centerColor.toArgb() }
        val crosshairPaint = Paint(strokePaint).apply { color = config.centerColor.toArgb(); strokeWidth = config.strokeWidth }
        val centerSize = radiusInfo.radius * config.centerSize

        when(config.centerShape){
            CenterShape.NONE -> {}
            CenterShape.DOT -> canvas.drawCircle(screenPos.x, yPosLifted, centerSize, centerPaint)
            CenterShape.CROSSHAIR -> {
                val circleRadius = centerSize * 0.4f
                crosshairPaint.style = Paint.Style.STROKE
                canvas.drawCircle(screenPos.x, yPosLifted, circleRadius, crosshairPaint)
                canvas.drawLine(screenPos.x + circleRadius, yPosLifted, screenPos.x + centerSize, yPosLifted, crosshairPaint)
                canvas.drawLine(screenPos.x - circleRadius, yPosLifted, screenPos.x - centerSize, yPosLifted, crosshairPaint)
                canvas.drawLine(screenPos.x, yPosLifted + circleRadius, screenPos.x, yPosLifted + centerSize, crosshairPaint)
                canvas.drawLine(screenPos.x, yPosLifted - circleRadius, screenPos.x, yPosLifted - centerSize, crosshairPaint)
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
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, it, label, config, state)
        }

        if (!state.isBankingMode) {
            textRenderer.draw(
                canvas,
                textPaint,
                state.zoomSliderPosition,
                state.protractorUnit,
                "Target Ball",
                LabelConfig.targetBall,
                state
            )
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, "Ghost Cue Ball", LabelConfig.ghostCueBall, state)
        }

        state.obstacleBalls.forEachIndexed { index, obstacle ->
            textRenderer.draw(
                canvas,
                textPaint,
                state.zoomSliderPosition,
                obstacle,
                "Obstacle ${index + 1}",
                LabelConfig.obstacleBall,
                state
            )
        }
    }
}