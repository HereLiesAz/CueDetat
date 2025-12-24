package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer

class OverlayRenderer {
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()
    private val lineRenderer = LineRenderer()
    private val ballRenderer = BallRenderer()

    fun draw(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface? = null
    ) {
        if (state.table.isVisible) {
            tableRenderer.drawSurface(canvas, state, paints)
            tableRenderer.drawPockets(canvas, state, paints)
            railRenderer.draw(canvas, state, paints, typeface)
        }

        lineRenderer.draw(canvas, state, paints, typeface)
        ballRenderer.draw(canvas, state, paints, typeface)
    }
}
