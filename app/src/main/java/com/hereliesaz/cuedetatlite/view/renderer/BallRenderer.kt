// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/BallRenderer.kt
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
                ballTextRenderer.draw(canvas, "A", it.logicalPosition, paints.actualCueBallTextPaint, overlayState.zoomSliderPosition)
            }
        }
        canvas.restore()
    }

    private fun drawProtractorBalls(canvas: Canvas, screenState: ScreenState, overlayState: OverlayState) {
        val protractorUnit = screenState.protractorUnit
        // Draw Target Ball
        drawBall(canvas, protractorUnit.targetBall, paints.targetCirclePaint, paints.targetCenterMarkPaint)
        ballTextRenderer.draw(canvas, "T", protractorUnit.targetBall.logicalPosition, paints.targetBallTextPaint, overlayState.zoomSliderPosition)

        // Draw Cue Ball
        drawBall(canvas, protractorUnit.cueBall, paints.cueCirclePaint, paints.cueCenterMarkPaint)
        ballTextRenderer.draw(canvas, "C", protractorUnit.cueBall.logicalPosition, paints.cueBallTextPaint, overlayState.zoomSliderPosition)
    }

    private fun drawBall(canvas: Canvas, ball: IlogicalBall, circlePaint: Paint, centerPaint: Paint) {
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius, circlePaint)
        canvas.drawCircle(ball.logicalPosition.x, ball.logicalPosition.y, ball.radius / 4, centerPaint)
    }
}
