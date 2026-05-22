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
 *
 * Internally uses [Pt] rather than [android.graphics.PointF] so the algorithm
 * can be exercised in plain JVM unit tests without Robolectric.
 */
object TableGeometryFitter {

    /** Lightweight coordinate pair, usable in both Android and plain-JVM tests. */
    internal data class Pt(val x: Float, val y: Float)

    private const val ASPECT_RATIO = 2.0f
    private const val ASPECT_TOLERANCE = 0.25f  // ±25% tolerance on 2:1 ratio

    /** Max acceptable diagonal difference (in logical units) at which fit quality reaches 0. */
    private const val MAX_DIAGONAL_DIFF_LOGICAL = 50f

    /** Return type for [fitPtWithQuality] and [fitWithQuality]. */
    internal data class FitResult(
        val assignments: List<Pair<PocketId, Pt>>?,
        val quality: Float  // 0..1; 1 = perfect rectangle, 0 = unfit or too distorted
    )

    // ── Public surface ────────────────────────────────────────────────────────

    /**
     * Attempts to identify the 6 pockets from [android.graphics.PointF] inputs.
     *
     * @param points 6 unordered PointF positions in logical space
     * @return List of (PocketId, PointF) pairs if fit succeeded, null otherwise.
     */
    fun fit(points: List<PointF>): List<Pair<PocketId, PointF>>? {
        val pts = points.map { Pt(it.x, it.y) }
        return fitPt(pts)?.map { (id, pt) -> id to PointF(pt.x, pt.y) }
    }

    /**
     * Public Android-facing variant of [fitPtWithQuality].
     * Returns a pair of (pocket assignments or null, quality 0..1).
     */
    fun fitWithQuality(points: List<PointF>): Pair<List<Pair<PocketId, PointF>>?, Float> {
        val pts = points.map { Pt(it.x, it.y) }
        val result = fitPtWithQuality(pts)
        return result.assignments?.map { (id, pt) -> id to PointF(pt.x, pt.y) } to result.quality
    }

    /**
     * Variant that accepts [Pt] directly — used by unit tests to avoid
     * dependency on the Android framework mock.
     */
    internal fun fitPt(points: List<Pt>): List<Pair<PocketId, Pt>>? =
        fitPtWithQuality(points).assignments

    /**
     * Like [fitPt] but also returns a quality score (0..1).
     * Quality = 1 - (diagonal_difference / MAX_DIAGONAL_DIFF_LOGICAL), clamped to [0,1].
     * Quality is 0 when fit fails or fewer than 6 points are provided.
     */
    internal fun fitPtWithQuality(points: List<Pt>): FitResult {
        if (points.size != 6) return FitResult(null, 0f)

        var bestScore = Float.MAX_VALUE
        var bestResult: List<Pair<PocketId, Pt>>? = null

        val indices = points.indices.toList()
        for (i in 0 until 6) for (j in i + 1 until 6) for (k in j + 1 until 6) for (l in k + 1 until 6) {
            val cornerIdx = listOf(i, j, k, l)
            val corners = cornerIdx.map { points[it] }
            val sides = indices.filter { it !in cornerIdx }.map { points[it] }
            val result = tryFitCorners(corners, sides) ?: continue
            val score = rectResidual(result)
            if (score < bestScore) { bestScore = score; bestResult = result }
        }

        if (bestResult == null) return FitResult(null, 0f)

        val diagonalDiff = kotlin.math.sqrt(bestScore.toDouble()).toFloat()
        val quality = (1f - diagonalDiff / MAX_DIAGONAL_DIFF_LOGICAL).coerceIn(0f, 1f)
        return FitResult(bestResult, quality)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun tryFitCorners(corners: List<Pt>, sides: List<Pt>): List<Pair<PocketId, Pt>>? {
        val ordered = orderRectangle(corners) ?: return null

        // Check aspect ratio: long / short should be ~2:1.
        val width  = dist(ordered[0], ordered[1])
        val height = dist(ordered[0], ordered[3])
        val (longSide, shortSide) = if (width >= height) width to height else height to width
        if (shortSide < 1f) return null
        val ratio = longSide / shortSide
        if (abs(ratio - ASPECT_RATIO) > ASPECT_TOLERANCE) return null

        val tl = ordered[0]; val tr = ordered[1]
        val br = ordered[2]; val bl = ordered[3]

        val leftMid  = Pt((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
        val rightMid = Pt((tr.x + br.x) / 2f, (tr.y + br.y) / 2f)

        val (sl, sr) = assignSides(sides, leftMid, rightMid) ?: return null

        return listOf(
            PocketId.TL to tl, PocketId.TR to tr,
            PocketId.BL to bl, PocketId.BR to br,
            PocketId.SL to sl, PocketId.SR to sr
        )
    }

    /** Orders 4 points as [TL, TR, BR, BL] by projecting onto ±45° axes. */
    private fun orderRectangle(pts: List<Pt>): List<Pt>? {
        val cx = pts.sumOf { it.x.toDouble() }.toFloat() / 4
        val cy = pts.sumOf { it.y.toDouble() }.toFloat() / 4
        val tl = pts.minByOrNull {  (it.x - cx) + (it.y - cy) } ?: return null
        val br = pts.maxByOrNull {  (it.x - cx) + (it.y - cy) } ?: return null
        val tr = pts.minByOrNull { -(it.x - cx) + (it.y - cy) } ?: return null
        val bl = pts.maxByOrNull { -(it.x - cx) + (it.y - cy) } ?: return null
        if (setOf(tl, tr, br, bl).size != 4) return null
        return listOf(tl, tr, br, bl)
    }

    private fun assignSides(sides: List<Pt>, leftMid: Pt, rightMid: Pt): Pair<Pt, Pt>? {
        if (sides.size != 2) return null
        return if (dist(sides[0], leftMid) <= dist(sides[0], rightMid))
            sides[0] to sides[1] else sides[1] to sides[0]
    }

    /**
     * Tie-breaker score for ranking valid rectangle candidates.
     * A perfect rectangle has equal-length diagonals; score = squared diagonal difference.
     * Lower is better. Used only to pick among candidates that already passed aspect check.
     */
    private fun rectResidual(result: List<Pair<PocketId, Pt>>): Float {
        val m = result.associate { it.first to it.second }
        val d1 = dist(m[PocketId.TL]!!, m[PocketId.BR]!!)
        val d2 = dist(m[PocketId.TR]!!, m[PocketId.BL]!!)
        return (d1 - d2) * (d1 - d2)
    }

    private fun dist(a: Pt, b: Pt) =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
}
