// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
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

    fun draw(canvas: Canvas, state: CueDetatState, paints: PaintCache, typeface: Typeface?, topDownProgress: Float = 0f) {
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
        
        // Calculate the Orthogonal Matrix to fit the table to screen.
        val orthoMatrix = Matrix().apply {
            val scale = (kotlin.math.min(
                state.viewWidth.toFloat() / state.table.logicalWidth,
                state.viewHeight.toFloat() / state.table.logicalHeight
            ) * 0.9f) // 90% fit
            postScale(scale, scale)
            postTranslate(state.viewWidth / 2f, state.viewHeight / 2f)
        }

        // Interpolate between Pitch and Ortho
        val interpolatedMatrix = if (topDownProgress <= 0f) {
            pitchMatrix
        } else if (topDownProgress >= 1f) {
            orthoMatrix
        } else {
            val pValues = FloatArray(9).apply { pitchMatrix.getValues(this) }
            val oValues = FloatArray(9).apply { orthoMatrix.getValues(this) }
            val iValues = FloatArray(9)
            for (i in 0..8) {
                iValues[i] = pValues[i] + (oValues[i] - pValues[i]) * topDownProgress
            }
            Matrix().apply { setValues(iValues) }
        }

        // Draw top-down snapshot if available and progress > 0
        if (topDownProgress > 0f && state.topDownBitmap != null) {
            canvas.withMatrix(interpolatedMatrix) {
                val halfW = state.table.logicalWidth / 2f
                val halfH = state.table.logicalHeight / 2f
                val destRect = android.graphics.RectF(-halfW, -halfH, halfW, halfH)
                val paint = paints.ballOverlayPaint.apply { alpha = (255 * topDownProgress).toInt() }
                this.drawBitmap(state.topDownBitmap, null, destRect, paint)
            }
        }

        val matrixFor2DPlane = interpolatedMatrix

        // Pass 1: Draw elements on the logical plane (Lines drawn UNDER balls)
        canvas.withMatrix(matrixFor2DPlane) {
            if (state.table.isVisible) {
                tableRenderer.drawSurface(this, state, paints)
                tableRenderer.drawPockets(this, state, paints)
            }
            lineRenderer.drawLogicalLines(this, state, paints, typeface, matrixFor2DPlane)
        }

        // Pass 2: Draw the "lifted" table rails
        // We only show rails when not in top-down view (or fade them out)
        if (topDownProgress < 0.5f) {
            canvas.withMatrix(railPitchMatrix) {
                if (state.table.isVisible) {
                    railRenderer.draw(this, state, paints, typeface)
                    railRenderer.drawRailLabels(this, state, paints, typeface)
                }
            }
        }

        // Pass 2.5: Beginner direction lines + triangles (below balls)
        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            lineRenderer.drawBeginnerLines(canvas, state, paints, matrixFor2DPlane)
        }

        // Pass 3: Draw all balls and their associated text
        ballRenderer.draw(canvas, state, paints, typeface)

        // Pass 4: Beginner text labels (above balls)
        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            lineRenderer.drawBeginnerLabels(canvas, state, paints, typeface, matrixFor2DPlane)
        }
    }
}