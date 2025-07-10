package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
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
            // In Banking Mode, the ball is a simple on-plane circle.
            state.onPlaneBall?.let { bankingBall ->
                drawOnPlaneBall(canvas, bankingBall, paints.bankLinePaint, paints, state)
            }
        } else {
            // In Protractor Mode, all balls are "ghosted" with a shadow and lifted effect.
            drawGhostedProtractorAndActual(canvas, state, paints)
        }

        // --- Labels (Drawn last in screen-space to be on top and not distorted) ---
        if (state.areHelpersVisible) {
            drawAllLabels(canvas, state, paints, typeface)
        }
    }

    /**
     * Draws the protractor unit (Target and Ghost balls) and the ActualCueBall as "lifted"
     * screen-space elements, along with their on-plane "shadows".
     */
    private fun drawGhostedProtractorAndActual(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val protractor = state.protractorUnit
        val ghostCuePaint = if (state.isImpossibleShot) paints.warningPaint else paints.cueCirclePaint
        val targetPaint = paints.targetCirclePaint
        val actualCuePaint = paints.actualCueBallPaint

        // --- Pass 1: Draw on-plane shadows first ---
        canvas.save()
        canvas.concat(state.pitchMatrix)
        // Target Ball Shadow
        canvas.drawCircle(protractor.center.x, protractor.center.y, protractor.radius, targetPaint)
        // Ghost Cue Ball Shadow
        canvas.drawCircle(protractor.ghostCueBallCenter.x, protractor.ghostCueBallCenter.y, protractor.radius, ghostCuePaint)
        // Actual Cue Ball Shadow
        state.onPlaneBall?.let {
            canvas.drawCircle(it.center.x, it.center.y, it.radius, actualCuePaint)
        }
        canvas.restore()

        // --- Pass 2: Draw lifted ghost effects ---
        // Target Ball
        drawLiftedBall(canvas, protractor.center, protractor.radius, state, targetPaint, paints.fillPaint, paints.ballGlowPaint)
        // Ghost Cue Ball
        drawLiftedBall(canvas, protractor.ghostCueBallCenter, protractor.radius, state, ghostCuePaint, paints.fillPaint, paints.ballGlowPaint)
        // Actual Cue Ball
        state.onPlaneBall?.let {
            drawLiftedBall(canvas, it.center, it.radius, state, actualCuePaint, paints.fillPaint, paints.ballGlowPaint)
        }
    }


    /**
     * Draws a single ball that exists on the logical plane (used for Banking Mode).
     */
    private fun drawOnPlaneBall(canvas: Canvas, ball: LogicalCircular, strokePaint: Paint, paints: PaintCache, state: OverlayState) {
        canvas.save()
        canvas.concat(state.pitchMatrix)
        // Draw glow first
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, paints.ballGlowPaint)
        // Then the ball
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, strokePaint)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius / 5f, paints.fillPaint)
        canvas.restore()
    }


    /**
     * Draws all text labels in screen-space to ensure they are not distorted.
     */
    private fun drawAllLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        // Label for the OnPlaneBall (Actual Cue or Banking)
        state.onPlaneBall?.let {
            val label = if (state.isBankingMode) "B" else "A"
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, it, label, state)
        }

        // Labels for the Protractor Unit
        if (!state.isBankingMode) {
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, state.protractorUnit, "T", state)
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, "G", state)
        }
    }

    /**
     * Helper to draw a single "lifted" ball for the ghost effect.
     * It projects logical coordinates to the screen and applies a manual lift.
     */
    private fun drawLiftedBall(canvas: Canvas, logicalCenter: PointF, logicalRadius: Float, state: OverlayState, strokePaint: Paint, fillPaint: Paint?, glowPaint: Paint) {
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(logicalCenter, logicalRadius, state)
        val screenPos = DrawingUtils.mapPoint(logicalCenter, state.pitchMatrix)
        val yPos = screenPos.y - radiusInfo.lift

        // Draw glow first
        canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius, glowPaint)
        // Then the ball
        canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius, strokePaint)
        fillPaint?.let {
            canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius / 5f, it)
        }
    }
}