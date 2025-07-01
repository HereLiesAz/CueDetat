package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.PointF
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.model.ILogicalBall
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class BallRenderer(
    private val paintCache: PaintCache,
    private val ballTextRenderer: BallTextRenderer
) {

    fun draw(canvas: Canvas, state: OverlayState) {
        if (state.isProtractorMode) {
            drawProtractorBalls(canvas, state)
        }
        if (state.showActualCueBall) {
            drawActualCueBall(canvas, state)
        }
    }

    private fun drawProtractorBalls(canvas: Canvas, state: OverlayState) {
        canvas.save()
        canvas.concat(state.pitchMatrix)

        // Draw Target Ball
        drawLogicalBall(canvas, state.protractorUnit)
        ballTextRenderer.draw(canvas, "Target", state.protractorUnit.center)

        // Draw Ghost Cue Ball
        if (state.showProtractorCueBall) {
            canvas.drawCircle(state.protractorUnit.protractorCueBallCenter.x, state.protractorUnit.protractorCueBallCenter.y, state.protractorUnit.radius, paintCache.whitePaint)
            ballTextRenderer.draw(canvas, "Ghost", state.protractorUnit.protractorCueBallCenter)
        }

        canvas.restore()
    }

    private fun drawActualCueBall(canvas: Canvas, state: OverlayState) {
        canvas.save()
        canvas.concat(state.pitchMatrix)

        drawLogicalBall(canvas, state.actualCueBall)
        ballTextRenderer.draw(canvas, "Cue", state.actualCueBall.center)

        canvas.restore()
    }

    private fun drawLogicalBall(canvas: Canvas, ball: ILogicalBall) {
        canvas.drawCircle(ball.center.x, ball.center.y, ball.radius, paintCache.whitePaint)
    }
}