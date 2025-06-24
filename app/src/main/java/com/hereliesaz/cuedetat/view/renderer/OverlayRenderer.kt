package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // --- Draw Banking Mode elements if active ---
        if (state.isBankingMode) {
            // Draw base table with the standard matrix
            canvas.save()
            // Corrected: use pitchMatrix from state
            canvas.concat(state.pitchMatrix)
            tableRenderer.draw(canvas, state, paints)
            canvas.restore()

            // Draw rails with the special, lifted matrix
            canvas.save()
            // Corrected: use railPitchMatrix from state
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state, paints)
            canvas.restore()
        }

        // --- Draw all elements on the 3D logical plane ---
        canvas.save()
        // Corrected: use pitchMatrix from state
        canvas.concat(state.pitchMatrix)

        lineRenderer.drawLogicalLines(canvas, state, paints, typeface)
        ballRenderer.drawLogicalBalls(canvas, state, paints)

        canvas.restore()

        // --- Draw screen-space elements (ghosts) on top ---
        ballRenderer.drawScreenSpaceBalls(canvas, state, paints, typeface)
    }
}