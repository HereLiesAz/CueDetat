// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import androidx.core.graphics.withMatrix
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.OverlayState

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()
    private val cvDebugRenderer = CvDebugRenderer()

    fun draw(canvas: Canvas, state: OverlayState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        // If the mask is to be shown (either in test or calibration), draw only it and nothing else.
        if (state.showCvMask) {
            cvDebugRenderer.draw(canvas, state)
            return
        }

        // If we are calibrating but not yet showing the mask, draw nothing but the camera feed.
        if (state.isCalibratingColor) {
            return
        }

        val pitchMatrix = state.pitchMatrix ?: return
        val railPitchMatrix = state.railPitchMatrix ?: pitchMatrix
        val logicalPlaneMatrix = state.logicalPlaneMatrix ?: pitchMatrix

        // In locked beginner mode, the 2D plane must be flat. Otherwise, it uses the full perspective.
        val matrixFor2DPlane =
            if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
                logicalPlaneMatrix
            } else {
                pitchMatrix
            }

        // Pass 1: Draw elements on the logical plane, using the correct matrix for the current mode.
        canvas.save()
        canvas.concat(matrixFor2DPlane)
        if (state.table.isVisible) {
            tableRenderer.drawSurface(canvas, state, paints)
            tableRenderer.drawPockets(canvas, state, paints)
        }
        lineRenderer.drawLogicalLines(canvas, state, paints, typeface)
        canvas.restore()


        // Pass 2: Draw the "lifted" table rails
        canvas.withMatrix(railPitchMatrix) {
            if (state.table.isVisible) {
                railRenderer.draw(this, state, paints, typeface)
                railRenderer.drawRailLabels(this, state, paints, typeface)
            }
        }


        // Pass 3: Draw all balls, which handle their own 2D/3D component rendering.
        ballRenderer.draw(canvas, state, paints, typeface)
    }
}