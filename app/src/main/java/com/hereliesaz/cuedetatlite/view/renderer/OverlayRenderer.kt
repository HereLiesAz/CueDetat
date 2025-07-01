package com.hereliesaz.cuedetatlite.view.renderer

import android.content.Context
import android.graphics.Canvas
import com.hereliesaz.cuedetatlite.view.PaintCache
import com.hereliesaz.cuedetatlite.view.renderer.text.BallTextRenderer
import com.hereliesaz.cuedetatlite.view.renderer.text.LineTextRenderer
import com.hereliesaz.cuedetatlite.view.state.OverlayState

class OverlayRenderer(context: Context) {

    private val paintCache = PaintCache()

    // Text Renderers
    private val ballTextRenderer = BallTextRenderer(paintCache)
    private val lineTextRenderer = LineTextRenderer(paintCache)

    // Component Renderers
    private val ballRenderer = BallRenderer(paintCache, ballTextRenderer)
    private val lineRenderer = LineRenderer(paintCache, lineTextRenderer)
    private val tableRenderer = TableRenderer(paintCache)
    private val railRenderer = RailRenderer(paintCache)


    fun draw(canvas: Canvas, state: OverlayState) {
        if (state.isDarkMode) {
            canvas.drawColor(android.graphics.Color.BLACK)
        } else {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }

        if (state.isProtractorMode) {
            ballRenderer.draw(canvas, state)
            lineRenderer.draw(canvas, state)
        } else { // Banking Mode
            state.tableModel?.let {
                tableRenderer.draw(canvas, state)
                railRenderer.draw(canvas, state)
                ballRenderer.draw(canvas, state) // Draw ball on top of table
                lineRenderer.draw(canvas, state)
            }
        }
    }
}