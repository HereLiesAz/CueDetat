// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt
package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class OverlayRenderer @Inject constructor(
    private val ballRenderer: BallRenderer,
    private val lineRenderer: LineRenderer,
    private val tableRenderer: TableRenderer,
    private val railRenderer: RailRenderer
) {

    fun draw(canvas: Canvas, state: OverlayState, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        if (state.table.isVisible) {
            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.drawSurface(canvas, state, typeface)
            canvas.restore()

            canvas.save()
            canvas.concat(state.railPitchMatrix)
            railRenderer.draw(canvas, state, typeface)
            lineRenderer.drawRailLabels(canvas, state, typeface)
            canvas.restore()

            canvas.save()
            canvas.concat(state.pitchMatrix)
            tableRenderer.drawPockets(canvas, state)
            canvas.restore()
        }

        canvas.save()
        canvas.concat(state.pitchMatrix)
        lineRenderer.drawLogicalLines(canvas, state, typeface)
        canvas.restore()

        ballRenderer.draw(canvas, state, typeface)
    }
}