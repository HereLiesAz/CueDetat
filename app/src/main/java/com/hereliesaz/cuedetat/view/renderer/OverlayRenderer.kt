// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/OverlayRenderer.kt

package com.hereliesaz.cuedetat.view.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Typeface
import androidx.core.graphics.withMatrix
import com.hereliesaz.cuedetat.R
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
    private val recommendationRenderer = com.hereliesaz.cuedetat.view.renderer.line.RecommendationRenderer()

    // Reused across draw() calls to avoid GC pressure at 60 fps.
    private val orthoMatrix = Matrix()
    private val interpolatedMatrix = Matrix()
    private val pValues = FloatArray(9)
    private val oValues = FloatArray(9)
    private val iValues = FloatArray(9)

    fun draw(
        canvas: Canvas,
        state: CueDetatState,
        paints: PaintCache,
        typeface: Typeface?,
        context: Context,
        topDownProgress: Float = 0f
    ) {
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

        // Calculate the Orthogonal Matrix to fit the table to screen. Reused.
        orthoMatrix.reset()
        val scale = kotlin.math.min(
            state.viewWidth.toFloat() / state.table.logicalWidth,
            state.viewHeight.toFloat() / state.table.logicalHeight
        ) * 0.9f // 90% fit
        orthoMatrix.postScale(scale, scale)
        orthoMatrix.postTranslate(state.viewWidth / 2f, state.viewHeight / 2f)

        // Interpolate between Pitch and Ortho.
        val matrixForPlane: Matrix = when {
            topDownProgress <= 0f -> pitchMatrix
            topDownProgress >= 1f -> orthoMatrix
            else -> {
                pitchMatrix.getValues(pValues)
                orthoMatrix.getValues(oValues)
                for (i in 0..8) {
                    iValues[i] = pValues[i] + (oValues[i] - pValues[i]) * topDownProgress
                }
                interpolatedMatrix.setValues(iValues)
                interpolatedMatrix
            }
        }

        // Draw top-down snapshot if available and progress > 0
        if (topDownProgress > 0f && state.topDownBitmap != null) {
            canvas.withMatrix(matrixForPlane) {
                val halfW = state.table.logicalWidth / 2f
                val halfH = state.table.logicalHeight / 2f
                val destRect = android.graphics.RectF(-halfW, -halfH, halfW, halfH)
                val paint = paints.ballOverlayPaint.apply {
                    this.alpha = (255 * topDownProgress).toInt()
                }
                this.drawBitmap(state.topDownBitmap, null, destRect, paint)
            }
        }

        val matrixFor2DPlane = matrixForPlane
        val labels = mapOf(
            "bankingBall" to context.getString(R.string.label_banking_ball),
            "actualCueBall" to context.getString(R.string.label_actual_cue_ball),
            "targetBall" to context.getString(R.string.label_target_ball),
            "ghostCueInstruction" to context.getString(R.string.label_ghost_cue_ball_instruction),
            "ghostCueBall" to context.getString(R.string.label_ghost_cue_ball),
            "obstacle" to context.getString(R.string.label_obstacle),
            "aimPocket" to context.getString(R.string.label_aim_pocket),
            "tangentLine" to context.getString(R.string.label_tangent_line)
        )

        // Pass 1: Draw elements on the logical plane (Lines drawn UNDER balls)
        // Pass 1: Draw elements on the logical plane (Lines drawn UNDER balls)
        canvas.withMatrix(matrixFor2DPlane) {
            if (state.table.isVisible) {
                tableRenderer.drawSurface(this, state, paints)
                tableRenderer.drawPockets(this, state, paints)
            }
        }
        lineRenderer.drawLogicalLines(canvas, state, paints, typeface, matrixFor2DPlane)

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

        // Pass 2.5: Beginner Elements (Ordered Z-Index)
        if (state.experienceMode == ExperienceMode.BEGINNER && state.isBeginnerViewLocked) {
            // 1. Triangles (Bottommost)
            lineRenderer.drawBeginnerTriangles(canvas, state, paints, matrixFor2DPlane)
            
            // 2. Lines
            lineRenderer.drawBeginnerLines(canvas, state, paints, matrixFor2DPlane)
            
            // 3. Static Circles
            ballRenderer.drawBeginnerStaticCircles(canvas, state, paints)
            
            // 4. Bubble Centers and Circles
            ballRenderer.drawBeginnerBubbleElements(canvas, state, paints)
            
            // 5. Static Centers
            ballRenderer.drawBeginnerStaticCenters(canvas, state, paints)
            
            // 6. ALL Text (Topmost)
            ballRenderer.drawBeginnerLabels(canvas, state, paints, typeface, labels)
            lineRenderer.drawBeginnerLabels(canvas, state, paints, typeface, matrixFor2DPlane, labels["aimPocket"], labels["tangentLine"])

        } else {
            // Standard/Expert Mode Passes
            
            // Pass 3: Draw all balls and their associated text
            ballRenderer.draw(canvas, state, paints, typeface, labels)
        }

        // Pass 4 (topmost): the advisor's recommended shot, when enabled.
        recommendationRenderer.draw(canvas, state, matrixFor2DPlane)

        if (topDownProgress <= 0f) drawTableFitGhost(canvas, state, pitchMatrix)
    }

    private val ghostPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
        color = android.graphics.Color.WHITE
        alpha = 150
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(24f, 16f), 0f)
    }
    private val ghostPath = android.graphics.Path()
    private val ghostPts = FloatArray(8)

    /**
     * Where the app thinks the real table is (the felt fit), as a dashed outline in screen space:
     * a suggestion the user can accept with Lock. Hidden when unsure, when locked, or when the
     * table already sits on it.
     */
    private fun drawTableFitGhost(canvas: Canvas, state: CueDetatState, pitchMatrix: Matrix) {
        val fit = state.tableFit ?: return
        if (state.isArTableLocked || fit.iou < com.hereliesaz.cuedetat.domain.TableSnapPolicy.GHOST_MIN_IOU) return
        state.table.corners.forEachIndexed { i, c -> ghostPts[i * 2] = c.x; ghostPts[i * 2 + 1] = c.y }
        pitchMatrix.mapPoints(ghostPts)
        // Nearest-corner match: the fit may name the same outline's corners half a turn round.
        val drift = (0 until 4).sumOf { i ->
            fit.viewQuad.minOf { q ->
                kotlin.math.hypot((ghostPts[i * 2] - q.x).toDouble(), (ghostPts[i * 2 + 1] - q.y).toDouble())
            }
        } / 4.0
        if (drift < GHOST_MIN_DRIFT_PX) return
        ghostPath.reset()
        fit.viewQuad.forEachIndexed { i, p -> if (i == 0) ghostPath.moveTo(p.x, p.y) else ghostPath.lineTo(p.x, p.y) }
        ghostPath.close()
        canvas.drawPath(ghostPath, ghostPaint)
    }

    private companion object {
        /** Average corner gap (px) below which the table already sits on the fit. */
        const val GHOST_MIN_DRIFT_PX = 12.0
    }
}
