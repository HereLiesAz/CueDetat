package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.ILogicalBall
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import kotlin.math.cos
import kotlin.math.sin

class BallRenderer(
    private val paints: PaintCache,
    private val ballTextRenderer: BallTextRenderer
) {

    fun drawLogicalBalls(canvas: Canvas, state: OverlayState) {
        canvas.save()
        canvas.concat(state.pitchMatrix)

        if (state.screenState.isProtractorMode) {
            drawProtractorLogicalBalls(canvas, state)
        }

        if (state.screenState.showActualCueBall) {
            state.screenState.actualCueBall?.let {
                drawBall(canvas, it, paints.actualCueBallBasePaint, paints.actualCueBallCenterMarkPaint)
            }
        }
        canvas.restore()
    }

    fun drawScreenSpaceGhosts(canvas: Canvas, state: OverlayState) {
        if (state.screenState.isProtractorMode) {
            drawProtractorGhosts(canvas, state)
        }
        if (state.screenState.showActualCueBall) {
            state.screenState.actualCueBall?.let {
                val screenCenter = DrawingUtils.mapPoint(it.logicalPosition, state.pitchMatrix)
                val radiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(it, state)
                val visualY = screenCenter.y - radiusInfo.lift

                canvas.drawCircle(screenCenter.x, visualY, radiusInfo.radius, paints.actualCueBallGhostPaint)
                if (state.areHelpersVisible) {
                    ballTextRenderer.draw(canvas, paints.actualCueBallTextPaint, state.zoomSliderPosition, screenCenter.x, visualY, radiusInfo.radius, "Actual Cue Ball")
                }
            }
        }
    }

    private fun drawProtractorLogicalBalls(canvas: Canvas, state: OverlayState) {
        val protractorUnit = state.screenState.protractorUnit
        drawBall(canvas, protractorUnit.targetBall, paints.targetCirclePaint, paints.targetCenterMarkPaint)

        val ghostBall = getGhostBall(protractorUnit)
        drawBall(canvas, ghostBall, paints.ghostCueOutlinePaint, paints.cueCenterMarkPaint)
    }

    private fun drawProtractorGhosts(canvas: Canvas, state: OverlayState) {
        val protractorUnit = state.screenState.protractorUnit
        val targetBall = protractorUnit.targetBall
        val ghostBall = getGhostBall(protractorUnit)

        // Target Ghost
        val targetScreenCenter = DrawingUtils.mapPoint(targetBall.logicalPosition, state.pitchMatrix)
        val targetRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(targetBall, state)
        val targetVisualY = targetScreenCenter.y - targetRadiusInfo.lift
        canvas.drawCircle(targetScreenCenter.x, targetVisualY, targetRadiusInfo.radius, paints.targetGhostBallOutlinePaint)
        if (state.areHelpersVisible) {
            ballTextRenderer.draw(canvas, paints.targetBallTextPaint, state.zoomSliderPosition, targetScreenCenter.x, targetVisualY, targetRadiusInfo.radius, "Target Ball")
        }

        // Cue Ghost
        val ghostScreenCenter = DrawingUtils.mapPoint(ghostBall.logicalPosition, state.pitchMatrix)
        val ghostRadiusInfo = DrawingUtils.getPerspectiveRadiusAndLift(ghostBall, state)
        val ghostVisualY = ghostScreenCenter.y - ghostRadiusInfo.lift
        canvas.drawCircle(ghostScreenCenter.x, ghostVisualY, ghostRadiusInfo.radius, paints.ghostCueOutlinePaint)
        if (state.areHelpersVisible) {
            ballTextRenderer.draw(canvas, paints.ghostBallTextPaint, state.zoomSliderPosition, ghostScreenCenter.x, ghostVisualY, ghostRadiusInfo.radius, "Ghost Cue Ball")
        }
    }

    private fun getGhostBall(protractorUnit: ProtractorUnit): ILogicalBall {
        val angleRad = Math.toRadians(protractorUnit.aimingAngleDegrees.toDouble()).toFloat()
        val totalRadius = protractorUnit.targetBall.radius * 2
        val ghostBallX = protractorUnit.targetBall.logicalPosition.x - cos(angleRad) * totalRadius
        val ghostBallY = protractorUnit.targetBall.logicalPosition.y - sin(angleRad) * totalRadius
        return ProtractorUnit.LogicalBall(PointF(ghostBallX, ghostBallY), protractorUnit.targetBall.radius)
    }

    private fun drawBall(canvas: Canvas, ball: ILogicalBall, circlePaint: Paint, centerPaint: Paint) {
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius, circlePaint)
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius / 4, centerPaint)
    }
}