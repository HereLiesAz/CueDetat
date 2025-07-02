package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.ILogicalBall
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import kotlin.math.cos
import kotlin.math.sin

class BallRenderer(
    private val paints: PaintCache,
    private val ballTextRenderer: BallTextRenderer
) {

    fun draw(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        canvas.save()
        canvas.concat(overlayState.pitchMatrix)

        if (screenState.isProtractorMode) {
            drawProtractorBalls(canvas, screenState, overlayState)
        }

        if (screenState.showActualCueBall) {
            screenState.actualCueBall?.let {
                drawBall(canvas, it, paints.actualCueBallBasePaint, paints.actualCueBallCenterMarkPaint)
                ballTextRenderer.draw(canvas, paints.actualCueBallTextPaint, overlayState.zoomSliderPosition, it.logicalPosition.x, it.logicalPosition.y, it.radius, "A")
            }
        }
        canvas.restore()
    }

    private fun drawProtractorBalls(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        val protractorUnit = screenState.protractorUnit
        // Draw Target Ball
        drawBall(canvas, protractorUnit.targetBall, paints.targetCirclePaint, paints.targetCenterMarkPaint)
        ballTextRenderer.draw(canvas, paints.targetBallTextPaint, overlayState.zoomSliderPosition, protractorUnit.targetBall.logicalPosition.x, protractorUnit.targetBall.logicalPosition.y, protractorUnit.targetBall.radius, "T")

        // --- Calculate and draw Ghost Ball ---
        val targetBall = protractorUnit.targetBall
        val angleRad = Math.toRadians(protractorUnit.aimingAngleDegrees.toDouble()).toFloat()
        // The ghost ball is placed tangent to the target ball, opposite the aiming angle
        val totalRadius = targetBall.radius * 2
        val ghostBallX = targetBall.logicalPosition.x - cos(angleRad) * totalRadius
        val ghostBallY = targetBall.logicalPosition.y - sin(angleRad) * totalRadius

        val ghostBall = ProtractorUnit.LogicalBall(PointF(ghostBallX, ghostBallY), targetBall.radius)
        drawBall(canvas, ghostBall, paints.ghostCueOutlinePaint, paints.cueCenterMarkPaint)
        ballTextRenderer.draw(canvas, paints.ghostBallTextPaint, overlayState.zoomSliderPosition, ghostBallX, ghostBallY, ghostBall.radius, "G")
    }

    private fun drawBall(canvas: Canvas, ball: ILogicalBall, circlePaint: Paint, centerPaint: Paint) {
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius, circlePaint)
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius / 4, centerPaint)
    }
}