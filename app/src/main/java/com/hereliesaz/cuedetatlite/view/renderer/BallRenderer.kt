package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.IlogicalBall
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState

class BallRenderer(
    private val paints: PaintCache,
    private val ballTextRenderer: BallTextRenderer
) {

    fun draw(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        canvas.save()
        canvas.concat(overlayState.pitchMatrix)

        // Draw protractor balls
        if (screenState.isProtractorMode) {
            drawProtractorBalls(canvas, screenState, overlayState)
        }

        // Draw actual cue ball if available and enabled
        if (screenState.showActualCueBall) {
            screenState.actualCueBall?.let {
                drawBall(canvas, it, paints.actualCueBallBasePaint, paints.actualCueBallCenterMarkPaint)
                // FIX: Corrected arguments for ballTextRenderer.draw
                ballTextRenderer.draw(canvas, paints.actualCueBallTextPaint, overlayState.zoomSliderPosition, it.logicalPosition.x, it.logicalPosition.y, it.radius, "A")
            }
        }
        canvas.restore()
    }

    private fun drawProtractorBalls(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        val protractorUnit = screenState.protractorUnit
        // Draw Target Ball
        drawBall(canvas, protractorUnit.targetBall, paints.targetCirclePaint, paints.targetCenterMarkPaint)
        // FIX: Corrected arguments for ballTextRenderer.draw
        ballTextRenderer.draw(canvas, paints.targetBallTextPaint, overlayState.zoomSliderPosition, protractorUnit.targetBall.logicalPosition.x, protractorUnit.targetBall.logicalPosition.y, protractorUnit.targetBall.radius, "T")

        // Draw Cue Ball
        drawBall(canvas, protractorUnit.cueBall, paints.cueCirclePaint, paints.cueCenterMarkPaint)
        // FIX: Corrected arguments for ballTextRenderer.draw
        ballTextRenderer.draw(canvas, paints.cueBallTextPaint, overlayState.zoomSliderPosition, protractorUnit.cueBall.logicalPosition.x, protractorUnit.cueBall.logicalPosition.y, protractorUnit.cueBall.radius, "C")
    }

    private fun drawBall(canvas: Canvas, ball: IlogicalBall, circlePaint: Paint, centerPaint: Paint) {
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius, circlePaint)
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius / 4, centerPaint)
    }
}