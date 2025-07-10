package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
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
                val bankingPaint = paints.bankLine1Paint
                drawGhostedBall(canvas, bankingBall, BankingBall(), state, bankingPaint, paints.fillPaint, paints.ballGlowPaint)
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
        val ghostCuePaint = if (state.isImpossibleShot) paints.warningPaint else paints.cueCirclePaint
        val targetPaint = paints.targetCirclePaint
        val actualCuePaint = paints.actualCueBallPaint

        // Target Ball
        drawGhostedBall(canvas, protractor, TargetBall(), state, targetPaint, paints.fillPaint, paints.ballGlowPaint)

        // Ghost Cue Ball
        drawGhostedBall(canvas, object : LogicalCircular {
            override val center = protractor.ghostCueBallCenter
            override val radius = protractor.radius
        }, GhostCueBall(), state, ghostCuePaint, paints.fillPaint, paints.ballGlowPaint)

        // Actual Cue Ball
        state.onPlaneBall?.let {
            drawGhostedBall(canvas, it, ActualCueBall(), state, actualCuePaint, paints.fillPaint, paints.ballGlowPaint)
        }
    }

    private fun drawGhostedBall(canvas: Canvas, ball: LogicalCircular, config: BallsConfig, strokePaint: Paint, fillPaint: Paint?, glowPaint: Paint) {
        // Calculate the single source of truth for the ball's on-screen appearance.
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state)
        val screenPos = DrawingUtils.mapPoint(ball.center, state.pitchMatrix)
        val yPosLifted = screenPos.y - radiusInfo.lift

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
        val centerPaint = Paint(fillPaint).apply { color = config.centerColor.toArgb() }
        val crosshairPaint = Paint(strokePaint).apply { color = config.centerColor.toArgb(); strokeWidth = config.strokeWidth * 0.5f }
        val centerSize = radiusInfo.radius * config.centerSize

        when(config.centerShape){
            CenterShape.NONE -> {}
            CenterShape.DOT -> canvas.drawCircle(screenPos.x, yPosLifted, centerSize, centerPaint)
            CenterShape.CROSSHAIR -> {
                canvas.drawLine(screenPos.x - centerSize, yPosLifted, screenPos.x + centerSize, yPosLifted, crosshairPaint)
                canvas.drawLine(screenPos.x, yPosLifted - centerSize, screenPos.x, yPosLifted + centerSize, crosshairPaint)
            }
        }
    }

    private fun drawAllLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        state.onPlaneBall?.let {
            val label = if (state.isBankingMode) "Banking Ball" else "Actual Cue Ball"
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, it, label, state)
        }

        if (!state.isBankingMode) {
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, state.protractorUnit, "Target Ball", state)
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, "Ghost Cue Ball", state)
        }
    }
}