package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // --- Pass 1: Draw On-Plane elements (Table, Rails, Actual/Banking Ball) ---
        canvas.save()
        if (state.showTable || state.isBankingMode) {
            // Pitched Table Surface
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.draw(canvas, state, paints)
            canvas.restore()

            // Lifted Rails
            canvas.save()
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state, paints)
            canvas.restore()
        }

        // Pitched On-Plane Ball (Actual Cue Ball / Banking Ball)
        canvas.save()
        canvas.concat(state.pitchMatrix)
        state.actualCueBall?.let {
            ballRenderer.drawOnPlaneBall(canvas, it, paints.actualCueBallPaint, paints)
        }
        canvas.restore()
        canvas.restore() // Corresponds to the top-level save


        // --- Pass 2: Draw Screen-Space Ghost/UI elements ---
        // This pass does not use the pitchMatrix, applying lift manually for a 3D effect.
        if (!state.isBankingMode) {
            lineRenderer.draw(canvas, state, paints, typeface) // Lines need both logical and screen space info
            ballRenderer.drawGhostedProtractor(canvas, state, paints, typeface)
        } else {
            // Draw banking lines in screen space as well if they need complex logic
            lineRenderer.draw(canvas, state, paints, typeface)
        }
    }
}