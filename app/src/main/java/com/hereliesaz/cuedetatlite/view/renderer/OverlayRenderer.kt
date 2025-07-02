package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class OverlayRenderer(
    private val ballRenderer: BallRenderer,
    private val lineRenderer: LineRenderer,
    private val railRenderer: RailRenderer,
    private val tableRenderer: TableRenderer,
    private val paintCache: PaintCache
) {
    fun draw(canvas: Canvas, state: OverlayState) {
        // Clear canvas for transparency
        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        if (state.isBankingMode) {
            // Draw table surface with the standard matrix
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.draw(canvas, state.screenState, state)
            canvas.restore()

            // Draw rails with the special, lifted matrix
            canvas.save()
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state.screenState, state)
            canvas.restore()
        }

        // Draw all logical lines and 2D balls on the transformed plane
        lineRenderer.draw(canvas, state.screenState, state)
        ballRenderer.drawLogicalBalls(canvas, state, paintCache)

        // Draw the 3D "ghost" ball effects on top, in screen space
        ballRenderer.drawScreenSpaceGhosts(canvas, state)
    }
}