// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // Pass 1: Draw Table Surface
        if (state.table.isVisible) {
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.drawSurface(canvas, state, paints)
            canvas.restore()
        }

        // Pass 2: Draw all logical lines on the pitched canvas
        canvas.save()
        canvas.concat(state.pitchMatrix)
        lineRenderer.drawLogicalLines(canvas, state, paints, typeface)
        canvas.restore()

        // Pass 3: Draw Lifted Rails & Their Labels
        if (state.table.isVisible) {
            canvas.save()
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state, paints)
            lineRenderer.drawRailLabels(canvas, state, paints, typeface)
            canvas.restore()
        }

        // Pass 4: Draw Pockets (on top of lines)
        if (state.table.isVisible) {
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.drawPockets(canvas, state, paints)
            canvas.restore()
        }

        // Pass 5: Draw all balls
        ballRenderer.draw(canvas, state, paints, typeface)
    }
}