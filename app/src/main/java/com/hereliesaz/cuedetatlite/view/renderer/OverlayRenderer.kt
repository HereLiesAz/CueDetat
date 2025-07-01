// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/view/renderer/OverlayRenderer.kt
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
        // Clear canvas
        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        // Update paint colors based on the current state
        paintCache.updateColors(state, true) // Assuming dark mode for now

        if (state.isBankingMode) {
            tableRenderer.draw(canvas, state)
            railRenderer.draw(canvas, state)
        }

        lineRenderer.draw(canvas, state.screenState, state)
        ballRenderer.draw(canvas, state.screenState, state)
    }
}
