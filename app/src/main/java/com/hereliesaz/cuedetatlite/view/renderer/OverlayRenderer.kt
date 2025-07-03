package com.hereliesaz.cuedetatlite.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class OverlayRenderer(
    private val ballRenderer: BallRenderer,
    private val lineRenderer: LineRenderer,
    private val railRenderer: RailRenderer,
    private val tableRenderer: TableRenderer,
    private val paints: PaintCache
) {
    fun draw(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        if (state.screenState.isBankingMode) {
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.draw(canvas, state)
            canvas.restore()

            canvas.save()
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state)
            canvas.restore()
        }

        canvas.save()
        canvas.concat(state.pitchMatrix)
        lineRenderer.drawLogicalLines(canvas, state, typeface)
        ballRenderer.drawLogicalBalls(canvas, state)
        canvas.restore()

        ballRenderer.drawScreenSpaceBalls(canvas, state, typeface)
    }
}
