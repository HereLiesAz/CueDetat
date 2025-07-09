package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.model.ILogicalBall
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

class BallRenderer {

    private val textRenderer = BallTextRenderer()

    /**
     * Draws the protractor unit (Target and Ghost balls) as "lifted" screen-space elements.
     */
    fun drawGhostedProtractor(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val protractor = state.protractorUnit
        val ghostCuePaint = if (state.isImpossibleShot) paints.warningPaint else paints.cueCirclePaint

        // Draw glows first
        drawLiftedBall(canvas, protractor.protractorCueBallCenter, protractor.radius, state, paints.glowPaint)
        drawLiftedBall(canvas, protractor.center, protractor.radius, state, paints.glowPaint)

        // Draw primary balls
        drawLiftedBall(canvas, protractor.protractorCueBallCenter, protractor.radius, state, ghostCuePaint, paints.fillPaint)
        drawLiftedBall(canvas, protractor.center, protractor.radius, state, paints.targetCirclePaint, paints.fillPaint)

        // Draw text labels
        if (state.areHelpersVisible) {
            val textPaint = paints.textPaint.apply { this.typeface = typeface }
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, protractor, "T", state)
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, object : ILogicalBall {
                override val center = protractor.protractorCueBallCenter
                override val radius = protractor.radius
            }, "G", state)
        }
    }

    /**
     * Draws a single ball that exists on the logical plane (e.g., ActualCueBall or BankingBall).
     * This method assumes the canvas is already transformed by the pitchMatrix.
     */
    fun drawOnPlaneBall(canvas: Canvas, ball: ILogicalBall, strokePaint: Paint, paints: PaintCache) {
        // Since the canvas is already pitched, we draw at the logical coordinates.
        // The perspective is handled by the canvas transformation.
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, paints.glowPaint)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, strokePaint)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius / 5f, paints.fillPaint)
    }


    /**
     * Helper to draw a single "lifted" ball for the ghost effect.
     * It projects logical coordinates to the screen and applies a manual lift.
     */
    private fun drawLiftedBall(canvas: Canvas, logicalCenter: PointF, logicalRadius: Float, state: OverlayState, strokePaint: Paint, fillPaint: Paint? = null) {
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(logicalCenter, logicalRadius, state)
        val screenPos = DrawingUtils.mapPoint(logicalCenter, state.pitchMatrix)
        val yPos = screenPos.y - radiusInfo.lift

        canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius, strokePaint)
        fillPaint?.let {
            canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius / 5f, it)
        }
    }
}