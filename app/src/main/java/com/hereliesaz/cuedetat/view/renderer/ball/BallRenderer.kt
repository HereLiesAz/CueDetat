package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.AcidPatina
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.config.ball.ActualCueBall
import com.hereliesaz.cuedetat.view.config.ball.BankingBall
import com.hereliesaz.cuedetat.view.config.ball.GhostCueBall
import com.hereliesaz.cuedetat.view.config.ball.ObstacleBall
import com.hereliesaz.cuedetat.view.config.ball.TargetBall
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

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

        // Draw results from the generic ML Kit model (e.g., as blue outlines)
        val genericPaint = Paint(paints.targetCirclePaint).apply {
            color = AcidPatina.toArgb()
            pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
            strokeWidth = 3f
        }
        state.visionData.genericBalls.forEach {
            canvas.drawCircle(it.x, it.y, 25f, genericPaint)
        }

        // Draw results from the custom TFLite model (e.g., as yellow outlines)
        val customPaint = Paint(paints.targetCirclePaint).apply {
            color = RebelYellow.toArgb()
            strokeWidth = 4f
        }
        state.visionData.customBalls.forEach {
            canvas.drawCircle(it.x, it.y, 25f, customPaint)
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
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state)
        val screenPos = DrawingUtils.mapPoint(ball.center, state.pitchMatrix)
        val yPosLifted = screenPos.y - radiusInfo.lift

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
            color = config.glowColor.toArgb()
            alpha = (config.glowColor.alpha * 255).toInt()
        }

        val dotPaint = Paint(paints.fillPaint).apply { color = android.graphics.Color.WHITE }
        val dotRadius = ball.radius * 0.1f

        // Draw on-plane shadow
        canvas.save()
        canvas.concat(state.pitchMatrix)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, strokePaint)
        canvas.drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
        canvas.restore()

        // Draw the lifted ghost effect
        canvas.drawCircle(screenPos.x, yPosLifted, radiusInfo.radius, glowPaint)
        canvas.drawCircle(screenPos.x, yPosLifted, radiusInfo.radius, strokePaint)

        // Draw center shape
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