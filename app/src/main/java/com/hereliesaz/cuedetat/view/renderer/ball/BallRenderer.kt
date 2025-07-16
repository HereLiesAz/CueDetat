// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/ball/BallRenderer.kt
package com.hereliesaz.cuedetat.view.renderer.ball

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hereliesaz.cuedetat.ui.theme.RebelYellow
import com.hereliesaz.cuedetat.view.config.ball.*
import com.hereliesaz.cuedetat.view.config.base.BallsConfig
import com.hereliesaz.cuedetat.view.config.base.CenterShape
import com.hereliesaz.cuedetat.view.model.LogicalCircular
import com.hereliesaz.cuedetat.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.hypot

class BallRenderer @Inject constructor(private val textRenderer: BallTextRenderer) {

    fun draw(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        if (state.isBankingMode) {
            state.onPlaneBall?.let { bankingBall ->
                drawOnPlaneBall(canvas, bankingBall, BankingBall(), state)
            }
        } else {
            drawProtractorAndActual(canvas, state)
        }

        state.obstacleBalls.forEach { obstacle ->
            drawOnPlaneBall(canvas, obstacle, ObstacleBall(), state)
        }

        drawBoundingBoxes(canvas, state)
        drawSnapIndicators(canvas, state)

        if (state.areHelpersVisible) {
            drawAllLabels(canvas, state, typeface)
        }
    }

    private fun drawBoundingBoxes(canvas: Canvas, state: OverlayState) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.Blue.toArgb()
            alpha = 150
        }
        canvas.save()
        canvas.concat(state.pitchMatrix)
        state.visionData.detectedBoundingBoxes.forEach { box ->
            canvas.drawRect(RectF(box), paint)
        }
        canvas.restore()
    }

    private fun drawSnapIndicators(canvas: Canvas, state: OverlayState) {
        val snappedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RebelYellow.toArgb()
            style = Paint.Style.FILL
            alpha = 150
        }
        val detectedBalls = state.visionData.genericBalls + state.visionData.customBalls
        val allLogicalBalls = (listOfNotNull(state.onPlaneBall, state.protractorUnit) + state.obstacleBalls)

        allLogicalBalls.forEach { logicalBall ->
            val isSnapped = detectedBalls.any { detected ->
                hypot((logicalBall.center.x - detected.x).toDouble(), (logicalBall.center.y - detected.y).toDouble()) < 5.0
            }
            if (isSnapped) {
                canvas.save()
                canvas.concat(state.pitchMatrix)
                canvas.drawCircle(logicalBall.center.x, logicalBall.center.y, logicalBall.radius * 0.5f, snappedPaint)
                canvas.restore()
            }
        }
    }

    private fun drawProtractorAndActual(canvas: Canvas, state: OverlayState) {
        val protractor = state.protractorUnit
        drawGhostedBall(canvas, protractor, TargetBall(), state)
        drawGhostedBall(canvas, object : LogicalCircular {
            override val center = protractor.ghostCueBallCenter
            override val radius = protractor.radius
        }, GhostCueBall(), state)
        state.onPlaneBall?.let {
            drawGhostedBall(canvas, it, ActualCueBall(), state)
        }
    }

    private fun drawOnPlaneBall(canvas: Canvas, ball: LogicalCircular, config: BallsConfig, state: OverlayState) {
        val strokePaint = config.getStrokePaint(state.luminanceAdjustment, isWarning = false)
        val fillPaint = config.getFillPaint(state.luminanceAdjustment)
        val centerPaint = config.getCenterPaint(state.luminanceAdjustment)
        val glowPaint = config.getGlowPaint(state.glowStickValue)

        canvas.save()
        canvas.concat(state.pitchMatrix)
        glowPaint?.let { canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, it) }
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, fillPaint)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, strokePaint)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius * config.centerSize, centerPaint)
        canvas.restore()
    }

    private fun drawGhostedBall(canvas: Canvas, ball: LogicalCircular, config: BallsConfig, state: OverlayState) {
        val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ball.center, ball.radius, state)
        val screenPos = DrawingUtils.mapPoint(ball.center, state.pitchMatrix)
        val yPosLifted = screenPos.y - radiusInfo.lift

        val isWarning = (state.isGeometricallyImpossible || state.isObstructed) && config is GhostCueBall
        val strokePaint = config.getStrokePaint(state.luminanceAdjustment, isWarning)
        val glowPaint = config.getGlowPaint(state.glowStickValue)
        val dotPaint = config.getCenterPaint(state.luminanceAdjustment).apply { style = Paint.Style.FILL }
        val dotRadius = ball.radius * 0.1f

        // On-plane shadow
        canvas.save()
        canvas.concat(state.pitchMatrix)
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, strokePaint)
        canvas.drawCircle(ball.center.x, ball.center.y, dotRadius, dotPaint)
        canvas.restore()

        // Lifted ghost
        glowPaint?.let { canvas.drawCircle(screenPos.x, yPosLifted, radiusInfo.radius, it) }
        canvas.drawCircle(screenPos.x, yPosLifted, radiusInfo.radius, strokePaint)

        val centerPaint = config.getCenterPaint(state.luminanceAdjustment)
        val crosshairPaint = config.getStrokePaint(state.luminanceAdjustment, isWarning).apply { color = config.centerColor.toArgb() }
        val centerSize = radiusInfo.radius * config.centerSize

        when (config.centerShape) {
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

    private fun drawAllLabels(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            color = state.appControlColorScheme.onSurface.toArgb()
        }

        state.onPlaneBall?.let {
            val label = if (state.isBankingMode) BankingBall().label else ActualCueBall().label
            textRenderer.draw(canvas, textPaint, state, it, label)
        }

        if (!state.isBankingMode) {
            textRenderer.draw(canvas, textPaint, state, state.protractorUnit, TargetBall().label)
            textRenderer.draw(canvas, textPaint, state, object : LogicalCircular {
                override val center = state.protractorUnit.ghostCueBallCenter
                override val radius = state.protractorUnit.radius
            }, GhostCueBall().label)
        }
    }
}