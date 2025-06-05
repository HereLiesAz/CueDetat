package com.hereliesaz.cuedetat.drawing.screen.elements

import android.graphics.Canvas
import com.hereliesaz.cuedetat.state.AppPaints
import com.hereliesaz.cuedetat.state.AppState // Import AppState
import com.hereliesaz.cuedetat.tracking.ball_detector.Ball

class DetectedBallOutlineDrawer {
    /**
     * Draws an outline around each ball in the provided list.
     * @param canvas The canvas to draw on.
     * @param appPaints The collection of paints.
     * @param balls The list of balls to draw outlines for.
     * @param zoomFactor The current zoom factor to apply to the ball radii.
     */
    fun draw(
        canvas: Canvas,
        appPaints: AppPaints,
        balls: List<Ball>,
        zoomFactor: Float // Added zoomFactor parameter
    ) {
        for (ball in balls) {
            // Apply zoom factor to the ball's radius before drawing
            val scaledRadius = ball.radius * zoomFactor
            if (scaledRadius > 0.01f) { // Only draw if radius is meaningful
                canvas.drawCircle(ball.x, ball.y, scaledRadius, appPaints.detectedBallOutlinePaint)
            }
        }
    }
}