package com.hereliesaz.cuedetat.view.renderer

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

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        // Draw glow effects first
        drawGlows(canvas, state, paints)

        // Draw primary ball outlines and fills
        drawBalls(canvas, state, paints)

        // Draw text labels on top
        if (state.areHelpersVisible) {
            drawLabels(canvas, state, paints, typeface)
        }
    }

    private fun drawGlows(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val glowPaint = paints.glowPaint
        drawBall(canvas, state.protractorUnit.protractorCueBallCenter, state.protractorUnit.radius, state, glowPaint)
        drawBall(canvas, state.protractorUnit.center, state.protractorUnit.radius, state, glowPaint)
        state.actualCueBall?.let {
            drawBall(canvas, it.center, it.radius, state, glowPaint)
        }
    }

    private fun drawBalls(canvas: Canvas, state: OverlayState, paints: PaintCache) {
        val ghostCuePaint = if (state.isImpossibleShot) paints.warningPaint else paints.cueCirclePaint
        drawBall(canvas, state.protractorUnit.protractorCueBallCenter, state.protractorUnit.radius, state, ghostCuePaint, paints.fillPaint)
        drawBall(canvas, state.protractorUnit.center, state.protractorUnit.radius, state, paints.targetCirclePaint, paints.fillPaint)

        state.actualCueBall?.let {
            drawBall(canvas, it.center, it.radius, state, paints.actualCueBallPaint, paints.fillPaint)
        }
    }

    private fun drawLabels(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        val textPaint = paints.textPaint.apply { this.typeface = typeface }

        textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, state.protractorUnit, "T", state)

        textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, object : ILogicalBall {
            override val center = state.protractorUnit.protractorCueBallCenter
            override val radius = state.protractorUnit.radius
        }, "G", state)

        state.actualCueBall?.let {
            val label = if (state.isBankingMode) "B" else "A"
            textRenderer.draw(canvas, textPaint, state.zoomSliderPosition, it, label, state)
        }
    }

    private fun drawBall(canvas: Canvas, logicalCenter: PointF, logicalRadius: Float, state: OverlayState, strokePaint: Paint, fillPaint: Paint? = null) {
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(logicalCenter, logicalRadius, state)
        val screenPos = DrawingUtils.mapPoint(logicalCenter, state.pitchMatrix)
        val yPos = screenPos.y - radiusInfo.lift

        canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius, strokePaint)
        fillPaint?.let {
            canvas.drawCircle(screenPos.x, yPos, radiusInfo.radius / 5f, it)
        }
    }
}