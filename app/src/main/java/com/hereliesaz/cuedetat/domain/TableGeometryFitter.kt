// app/src/main/java/com/hereliesaz/cuedetat/domain/TableGeometryFitter.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Fits 6 unordered logical-space pocket positions to a billiards table model
 * and assigns TL/TR/BL/BR/SL/SR identities.
 *
 * A billiards table has 4 corner pockets forming a 2:1 rectangle, plus 2 side
 * pockets at the midpoints of the long sides. All standard sizes (7ft, 8ft, 9ft)
 * are exactly 2:1.
 */
object TableGeometryFitter {

    private const val ASPECT_RATIO = 2.0f
    private const val ASPECT_TOLERANCE = 0.25f  // ±25% tolerance on 2:1 ratio

    /**
     * Attempts to identify the 6 pockets.
     *
     * @param points 6 unordered PointF positions in logical space
     * @return List of (PocketId, PointF) pairs if fit succeeded, null otherwise.
     *         Requires exactly 6 input points.
     */
    fun fit(points: List<PointF>): List<Pair<PocketId, PointF>>? {
        if (points.size != 6) return null

        var bestScore = Float.MAX_VALUE
        var bestResult: List<Pair<PocketId, PointF>>? = null

        // Try all C(6,4) = 15 combinations of 4 points as corner candidates.
        val indices = points.indices.toList()
        for (i in 0 until 6) for (j in i+1 until 6) for (k in j+1 until 6) for (l in k+1 until 6) {
            val corners = listOf(points[i], points[j], points[k], points[l])
            val sideIndices = indices.filter { it !in listOf(i, j, k, l) }
            val sides = sideIndices.map { points[it] }

            val result = tryFitCorners(corners, sides) ?: continue
            val score = rectResidual(result)
            if (score < bestScore) {
                bestScore = score
                bestResult = result
            }
        }
        return bestResult
    }

    /**
     * Tries to interpret 4 points as rectangle corners and 2 points as side pockets.
     * Returns null if the 4 points don't form a valid 2:1 rectangle.
     */
    private fun tryFitCorners(
        corners: List<PointF>,
        sides: List<PointF>
    ): List<Pair<PocketId, PointF>>? {
        // Order the 4 corners as a convex hull (clockwise: TL, TR, BR, BL).
        val ordered = orderRectangle(corners) ?: return null

        // Check aspect ratio: long / short should be ~2:1.
        val width = hypot(
            (ordered[1].x - ordered[0].x).toDouble(),
            (ordered[1].y - ordered[0].y).toDouble()
        ).toFloat()
        val height = hypot(
            (ordered[3].x - ordered[0].x).toDouble(),
            (ordered[3].y - ordered[0].y).toDouble()
        ).toFloat()
        val (longSide, shortSide) = if (width >= height) Pair(width, height) else Pair(height, width)
        if (shortSide < 1f) return null
        val ratio = longSide / shortSide
        if (abs(ratio - ASPECT_RATIO) > ASPECT_TOLERANCE) return null

        // Assign TL/TR/BR/BL based on convex-hull ordering (topmost-leftmost = TL).
        // `orderRectangle` guarantees: [0]=TL, [1]=TR, [2]=BR, [3]=BL.
        val tl = ordered[0]; val tr = ordered[1]
        val br = ordered[2]; val bl = ordered[3]

        // Assign side pockets: each must be near a long-side midpoint.
        val leftMid  = PointF((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
        val rightMid = PointF((tr.x + br.x) / 2f, (tr.y + br.y) / 2f)

        val (sl, sr) = assignSides(sides, leftMid, rightMid) ?: return null

        return listOf(
            PocketId.TL to tl, PocketId.TR to tr,
            PocketId.BL to bl, PocketId.BR to br,
            PocketId.SL to sl, PocketId.SR to sr
        )
    }

    /**
     * Orders 4 points as a clockwise rectangle [TL, TR, BR, BL].
     * Returns null if the points don't form a roughly convex quadrilateral.
     */
    private fun orderRectangle(pts: List<PointF>): List<PointF>? {
        // Sort by y then x to find TL candidate.
        val centroid = PointF(pts.sumOf { it.x.toDouble() }.toFloat() / 4,
                              pts.sumOf { it.y.toDouble() }.toFloat() / 4)
        // Classify by quadrant relative to centroid.
        val tl = pts.minByOrNull {  (it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        val br = pts.maxByOrNull {  (it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        val tr = pts.minByOrNull { -(it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        val bl = pts.maxByOrNull { -(it.x - centroid.x) + (it.y - centroid.y) } ?: return null
        if (setOf(tl, tr, br, bl).size != 4) return null
        return listOf(tl, tr, br, bl)
    }

    /** Assigns the two side points to SL (nearest leftMid) and SR (nearest rightMid). */
    private fun assignSides(
        sides: List<PointF>,
        leftMid: PointF,
        rightMid: PointF
    ): Pair<PointF, PointF>? {
        if (sides.size != 2) return null
        val d0toLeft  = dist(sides[0], leftMid)
        val d0toRight = dist(sides[0], rightMid)
        return if (d0toLeft <= d0toRight) Pair(sides[0], sides[1])
               else Pair(sides[1], sides[0])
    }

    /** Sum of squared deviations from ideal rectangle for the 4 corners. */
    private fun rectResidual(result: List<Pair<PocketId, PointF>>): Float {
        val byId = result.associate { it.first to it.second }
        val tl = byId[PocketId.TL]!!; val tr = byId[PocketId.TR]!!
        val bl = byId[PocketId.BL]!!; val br = byId[PocketId.BR]!!
        // Diagonals should be equal length for a rectangle.
        val d1 = dist(tl, br); val d2 = dist(tr, bl)
        return (d1 - d2) * (d1 - d2)
    }

    private fun dist(a: PointF, b: PointF) = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
}
