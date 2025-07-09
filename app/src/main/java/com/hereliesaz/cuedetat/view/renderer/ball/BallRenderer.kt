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
        // --- On-Plane Ball (ActualCueBall / BankingBall) ---
        state.onPlaneBall?.let { onPlaneBall ->
            val strokePaint = if (state.isBankingMode) paints.bankLinePaint else paints.actualCueBallPaint
            drawOnPlaneBall(canvas, onPlaneBall, strokePaint, paints, state)
        }

        // --- Ghosted Protractor Balls (Target / Ghost) ---
        if (!state.isBankingMode) {
            drawGhostedProtractor(canvas, state, paints)
        }

        // --- Labels (Drawn last in screen-space to be on top and not distorted) ---
        if (state.areHelpersVisible) {
            drawAllLabels(canvas, state, paints, typeface)
        }
    }

    /**
     * Draws the protractor unit (Target and Ghost balls) as "lifted" screen-space elements.
     * Also draws their on-plane "shadows".
     */
    private fun drawGhostedProtractor(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val protractor = state.protractorUnit
        val ghostCuePaint = if (state.isImpossibleShot) paints.warningPaint else paints.cueCirclePaint
        val targetPaint = paints.targetCirclePaint

        // Draw on-plane shadows first
        canvas.save()
        canvas.concat(state.pitchMatrix)
        canvas.drawCircle(protractor.ghostCueBallCenter.x, protractor.ghostCueBallCenter.y, protractor.radius, ghostCuePaint)
        canvas.drawCircle(protractor.center.x, protractor.center.y, protractor.radius, targetPaint)
        canvas.restore()

        // Draw lifted ghost effects
        drawLiftedBall(canvas, protractor.ghostCueBallCenter, protractor.radius, state, ghostCuePaint, paints.fillPaint, paints.glowPaint)
        drawLiftedBall(canvas, protractor.center, protractor.radius, state, targetPaint, paints.fillPaint, paints.glowPaint)
    }

    /**
     * Draws a single ball that exists on the logical plane (e.g., ActualCueBall or BankingBall).
     */
    private fun drawOnPlaneBall(canvas: Canvas, ball: LogicalCircular, strokePaint: Paint, paints: PaintCache, state: OverlayState) {
        canvas.save()
        canvas.concat(state.pitchMatrix)
        // Draw glow first
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, paints.glowPaint)
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
