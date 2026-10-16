package com.hereliesaz.cuedetat.view.renderer.line

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.renderer.renderLensTps
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.renderer.util.LensArrayCache
import com.hereliesaz.cuedetat.view.renderer.warpedBy
import kotlin.math.hypot

/**
 * Draws the advisor's recommended shot on top of everything: the cue → ghost → … → pocket
 * aim line, a ring around the recommended object ball, and a faint ghost-ball ring.
 *
 * Projection follows the same path as LineRenderer so the advisor line lands on the balls the
 * other renderers draw: points are TPS-warped via [renderLensTps], each segment is clipped to
 * the near plane ([DrawingUtils.clipToNearPlane]) so nothing behind the camera maps through
 * negative w, and segments are built with [DrawingUtils.buildDistortedLinePath] using the same
 * [LensArrayCache] distortion gate. Drawn in screen space so stroke widths aren't scaled by the
 * perspective matrix. Paints/Paths are allocated once as fields.
 */
class RecommendationRenderer {

    private val path = Path()
    private val segmentPath = Path()
    private val lensArrays = LensArrayCache()
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
    private val leavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
        alpha = 120
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    fun draw(canvas: Canvas, state: CueDetatState, matrix: Matrix) {
        if (!state.isAdvisorEnabled) return
        val shot = state.recommendedShot ?: return
        val pts = shot.shotPath
        if (pts.size < 2) return
        val tps = renderLensTps(state)
        val (camArray, distArray) = lensArrays.resolve(state)

        // Aim line through the whole shot path.
        buildPolyline(pts, tps, matrix, camArray, distArray)
        canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, linePaint)

        // Predicted cue-ball leave (dashed white), so the position recommendation is visible.
        val leave = shot.cueLeavePath
        if (leave.size >= 2) {
            buildPolyline(leave, tps, matrix, camArray, distArray)
            canvas.drawPath(path, leavePaint)
        }

        // Ring around the recommended object ball, then a faint ghost-ball ring.
        drawRing(canvas, shot.targetPos.warpedBy(tps), LOGICAL_BALL_RADIUS * 1.4f, ringPaint, matrix)
        drawRing(canvas, shot.ghostCuePos.warpedBy(tps), LOGICAL_BALL_RADIUS, ghostPaint, matrix)
    }

    /** Fills [path] with the visible, warped, distorted projection of [logical]. */
    private fun buildPolyline(
        logical: List<PointF>,
        tps: com.hereliesaz.cuedetat.domain.TpsWarpData?,
        matrix: Matrix,
        camArray: DoubleArray?,
        distArray: DoubleArray?
    ) {
        path.reset()
        for (i in 0 until logical.size - 1) {
            val a = logical[i].warpedBy(tps)
            val b = logical[i + 1].warpedBy(tps)
            val (s, e) = DrawingUtils.clipToNearPlane(a, b, matrix) ?: continue
            DrawingUtils.buildDistortedLinePath(s, e, matrix, camArray, distArray, out = segmentPath)
            path.addPath(segmentPath)
        }
    }

    /** Draw a circle at a logical center, sizing its radius by mapping a logical edge point. */
    private fun drawRing(canvas: Canvas, centerLogical: PointF, logicalRadius: Float, paint: Paint, matrix: Matrix) {
        if (DrawingUtils.clipToNearPlane(centerLogical, centerLogical, matrix) == null) return
        val center = DrawingUtils.mapPoint(centerLogical, matrix)
        val edge = DrawingUtils.mapPoint(PointF(centerLogical.x + logicalRadius, centerLogical.y), matrix)
        val r = hypot(edge.x - center.x, edge.y - center.y)
        if (r > 0.5f) canvas.drawCircle(center.x, center.y, r, paint)
    }
}
