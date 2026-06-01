package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import kotlin.math.hypot

/**
 * Draws the advisor's recommended shot on top of everything: the cue → ghost → … → pocket
 * aim line, a ring around the recommended object ball, and a faint ghost-ball ring.
 *
 * Everything is mapped logical → screen via [DrawingUtils.mapPoint] and drawn in screen space
 * (so stroke widths aren't scaled by the perspective matrix), matching LineRenderer's approach.
 * Paints/Path are reused per the renderer allocation conventions.
 */
class RecommendationRenderer {

    private val path = Path()
    private val gold = Color.parseColor("#FFB74D")

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = gold
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        color = gold
        alpha = 90
        maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = gold
    }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = gold
        alpha = 170
    }

    fun draw(canvas: Canvas, state: CueDetatState, matrix: Matrix) {
        if (!state.isAdvisorEnabled) return
        val shot = state.recommendedShot ?: return
        val pts = shot.shotPath
        if (pts.size < 2) return

        // Aim line through the whole shot path.
        path.reset()
        pts.forEachIndexed { i, lp ->
            val sp = DrawingUtils.mapPoint(lp, matrix)
            if (i == 0) path.moveTo(sp.x, sp.y) else path.lineTo(sp.x, sp.y)
        }
        canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, linePaint)

        // Ring around the recommended object ball, then a faint ghost-ball ring.
        drawRing(canvas, shot.targetPos, LOGICAL_BALL_RADIUS * 1.4f, ringPaint, matrix)
        drawRing(canvas, shot.ghostCuePos, LOGICAL_BALL_RADIUS, ghostPaint, matrix)
    }

    /** Draw a circle at a logical center, sizing its radius by mapping a logical edge point. */
    private fun drawRing(canvas: Canvas, centerLogical: PointF, logicalRadius: Float, paint: Paint, matrix: Matrix) {
        val center = DrawingUtils.mapPoint(centerLogical, matrix)
        val edge = DrawingUtils.mapPoint(PointF(centerLogical.x + logicalRadius, centerLogical.y), matrix)
        val r = hypot(edge.x - center.x, edge.y - center.y)
        if (r > 0.5f) canvas.drawCircle(center.x, center.y, r, paint)
    }
}
