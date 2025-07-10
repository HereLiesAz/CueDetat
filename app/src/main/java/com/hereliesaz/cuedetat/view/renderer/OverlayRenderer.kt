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
        lineRenderer.draw(canvas, state, paints, typeface) // Draws main aiming lines

        // Draw protractor guides on the same pitched plane
        if (!state.isBankingMode) {
            lineRenderer.drawProtractorGuides(
                canvas = canvas,
                state = state,
                paints = paints,
                center = state.protractorUnit.ghostCueBallCenter, // Use logical center
                referencePoint = state.protractorUnit.center // Use logical reference
            )
        }
        canvas.restore()

        // --- Pass 3: Draw banking labels on the lifted rail plane ---
        if (state.isBankingMode) {
            canvas.save()
            canvas.concat(state.railPitchMatrix)
            lineRenderer.drawBankingLabels(canvas, state, paints, typeface)
            canvas.restore()
        }

        // --- Pass 4: Draw all balls ---
        // The BallRenderer handles its own rendering passes internally for on-plane and ghosted elements.
        ballRenderer.draw(canvas, state, paints, typeface)

        // --- Pass 5: (Empty) Screen-space elements drawn by Composables ---
        // Protractor guides have been moved to Pass 2.
    }
}