package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.protractorUnit.center.x == 0f) return

        // --- Draw all elements on the 3D logical plane ---
        canvas.save()
        canvas.concat(state.pitchMatrix)

        lineRenderer.drawLogicalLines(canvas, state, paints, typeface)
        ballRenderer.drawLogicalBalls(canvas, state, paints)

        canvas.restore()

        // --- Draw screen-space elements (ghosts) on top ---
        ballRenderer.drawScreenSpaceBalls(canvas, state, paints, typeface)
    }
}