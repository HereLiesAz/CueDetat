package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // --- Pass 1: Draw On-Plane elements (Table, Rails) ---
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

        // --- Pass 2: Draw all logical lines on the pitched canvas ---
        canvas.save()
        canvas.concat(state.pitchMatrix)
        // This draws Aiming, Shot, and Tangent lines. Angle guides are drawn later.
        lineRenderer.draw(canvas, state, paints, typeface)
        canvas.restore()


        // --- Pass 3: Draw all balls ---
        // The BallRenderer handles its own rendering passes internally for on-plane and ghosted elements.
        ballRenderer.draw(canvas, state, paints, typeface)

        // --- Pass 4: Draw Screen-Space UI (Protractor Guides) ---
        if (!state.isBankingMode) {
            val screenCenter = DrawingUtils.mapPoint(state.protractorUnit.center, state.pitchMatrix)
            lineRenderer.drawProtractorGuides(canvas, state, paints, screenCenter)
        }
    }
}