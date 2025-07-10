package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.BankingBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

class BallRenderer {

    private val textRenderer = BallTextRenderer()

    /**
     * Main drawing method for all ball elements. It handles the different rendering passes
     * for on-plane and ghosted/lifted elements.
     */
    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.isBankingMode) {
            state.onPlaneBall?.let { bankingBall ->
                drawGhostedBall(canvas, bankingBall, BankingBall(), state, paints)
            }
        } else {
            drawProtractorAndActual(canvas, state, paints)
        }

        if (state.areHelpersVisible) {
            drawAllLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawProtractorAndActual(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val protractor = state.protractorUnit

        // Target Ball
        drawGhostedBall(canvas, protractor, TargetBall(), state, paints)

        // Ghost Cue Ball
        drawGhostedBall(canvas, object : LogicalCircular {
            override val center = protractor.ghostCueBallCenter
            override val radius = protractor.radius
        }, GhostCueBall(), state, paints)

        // Actual Cue Ball
        state.onPlaneBall?.let {
            drawGhostedBall(canvas, it, ActualCueBall(), state, paints)
        }
    }

    private fun drawGhostedBall(canvas: Canvas, ball: LogicalCircular, config: BallsConfig, state: OverlayState, paints: PaintCache) {
        // Calculate the single source of truth for the ball's on-screen appearance.
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state)
        val screenPos = DrawingUtils.mapPoint(ball.center, state.pitchMatrix)
        val yPosLifted = screenPos.y - radiusInfo.lift

        // Configure paints based on the specific config object
        val strokePaint = Paint(paints.targetCirclePaint).apply {
            color = config.strokeColor.toArgb()
            strokeWidth = config.strokeWidth
            alpha = (config.opacity * 255).toInt()
        }
        if (state.isImpossibleShot && config is GhostCueBall) {
            strokePaint.color = paints.warningPaint.color
        }

        val glowPaint = Paint(paints.ballGlowPaint).apply {
            strokeWidth = config.glowWidth
        }


        // --- Draw on-plane shadow first, using its pure logical radius ---
        canvas.save()
        canvas.concat(state.pitchMatrix)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, strokePaint)
        canvas.restore()

        // --- Then draw the lifted ghost effect ---
        // Glow first
        canvas.drawCircle(screenPos.x, yPosLifted, radiusInfo.radius, glowPaint)
        // Then the ball
        canvas.drawCircle(screenPos.x, yPosLifted, radiusInfo.radius, strokePaint)

        // Draw center shape
        val centerPaint = Paint(paints.fillPaint).apply { color = config.centerColor.toArgb() }
        val crosshairPaint = Paint(strokePaint).apply { color = config.centerColor.toArgb(); strokeWidth = config.strokeWidth }
        val centerSize = radiusInfo.radius * config.centerSize

        when(config.centerShape){
            CenterShape.NONE -> {}
            CenterShape.DOT -> canvas.drawCircle(screenPos.x, yPosLifted, centerSize, centerPaint)
            CenterShape.CROSSHAIR -> {
                val circleRadius = centerSize * 0.4f // Inner circle is 40% of the crosshair arm length
                crosshairPaint.style = Paint.Style.STROKE
                // Draw the central circle
                canvas.drawCircle(screenPos.x, yPosLifted, circleRadius, crosshairPaint)
                // Draw the four radiating lines
                canvas.drawLine(screenPos.x + circleRadius, yPosLifted, screenPos.x + centerSize, yPosLifted, crosshairPaint) // Right
                canvas.drawLine(screenPos.x - circleRadius, yPosLifted, screenPos.x - centerSize, yPosLifted, crosshairPaint) // Left
                canvas.drawLine(screenPos.x, yPosLifted + circleRadius, screenPos.x, yPosLifted + centerSize, crosshairPaint) // Bottom
                canvas.drawLine(screenPos.x, yPosLifted - circleRadius, screenPos.x, yPosLifted - centerSize, crosshairPaint) // Top
            }
        }
    }

    private fun drawAllLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        state.onPlaneBall?.let {
            val label = if (state.isBankingMode) BankingBall().label else ActualCueBall().label
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, it, label, state)
        }

        if (!state.isBankingMode) {
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, state.protractorUnit, TargetBall().label, state)
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, GhostCueBall().label, state)
        }
    }
}