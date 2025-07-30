// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Typeface
import androidx.core.graphics.withMatrix
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.view.PaintCache
import com.hereliesaz.cuedetat.view.renderer.ball.BallRenderer
import com.hereliesaz.cuedetat.view.renderer.line.LineRenderer
import com.hereliesaz.cuedetat.view.renderer.table.RailRenderer
import com.hereliesaz.cuedetat.view.renderer.table.TableRenderer

class OverlayRenderer {

    private val ballRenderer = BallRenderer()
    private val lineRenderer = LineRenderer()
    private val tableRenderer = TableRenderer()
    private val railRenderer = RailRenderer()
    private val cvDebugRenderer = CvDebugRenderer()

    fun draw(canvas: Canvas, state: CueDetatState, paints: PaintCache, typeface: Typeface?) {
        if (state.viewWidth == 0 || state.viewHeight == 0) return

        if (state.showCvMask) {
            cvDebugRenderer.draw(canvas, state)
            return
        }

        if (state.isCalibratingColor) {
            return
        }

        val pitchMatrix = state.pitchMatrix ?: return
        val railPitchMatrix = state.railPitchMatrix ?: pitchMatrix
        val logicalPlaneMatrix = state.logicalPlaneMatrix ?: pitchMatrix

        val matrixFor2DPlane =
            if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
                logicalPlaneMatrix
            } else {
                pitchMatrix
            }

        // Pass 1: Draw elements on the logical plane
        canvas.withMatrix(matrixFor2DPlane) {
            if (state.table.isVisible) {
                tableRenderer.drawSurface(this, state, paints)
                tableRenderer.drawPockets(this, state, paints)
            }
            lineRenderer.drawLogicalLines(this, state, paints, typeface)
        }


        // Pass 2: Draw the "lifted" table rails
        canvas.withMatrix(railPitchMatrix) {
            if (state.table.isVisible) {
                railRenderer.draw(this, state, paints, typeface)
                railRenderer.drawRailLabels(this, state, paints, typeface)
            }
        }


        // Pass 3: Draw all balls
        ballRenderer.draw(canvas, state, paints, typeface)
    }
}