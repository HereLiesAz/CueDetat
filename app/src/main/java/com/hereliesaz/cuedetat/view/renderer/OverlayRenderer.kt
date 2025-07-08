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

        if (state.isBankingMode) {
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.draw(canvas, state, paints)
            canvas.restore()

            canvas.save()
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state, paints)
            canvas.restore()
        }

        canvas.save()
        canvas.concat(state.pitchMatrix)

        lineRenderer.draw(canvas, state, paints, typeface)
        ballRenderer.draw(canvas, state, paints, typeface)

        canvas.restore()
    }
}