package com.hereliesaz.cuedetat.domain

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The judgement calls of ball finding, kept free of OpenCV and Android so they run in plain
 * JUnit.
 *
 * A ball, seen from above a pool table, is a hole in the felt: a patch of non-felt colour with
 * felt all round it. [judge] decides what such a patch (an "island") is, given its pixel area,
 * bounding box and the ball radius expected at that spot. [classify] names the ball from how
 * its pixels split into white, dark and coloured.
 *
 * A sphere projects to a near-circle from any viewpoint, so roundness is tested as-is; only the
 * size changes with distance, which is what the expected radius accounts for.
 */
object BallIslandRules {

    /** An island's area may be this fraction of a full ball's disk and still count as one. */
    const val MIN_AREA_RATIO = 0.35f

    /** ...and at most this. Above it the island is too big for one ball. */
    const val MAX_AREA_RATIO = 1.8f

    /** Longest side over shortest side of the bounding box for a single ball. */
    const val MAX_ASPECT = 1.5f

    /** Island area over bounding-box area. A disk fills pi/4 (0.785) of its box. */
    const val MIN_FILL = 0.5f

    /**
     * Two touching balls: area range. No aspect test, because a pair lying diagonally has a
     * near-square bounding box; [splitPair] finds the axis from the island's own shape.
     */
    const val PAIR_MIN_AREA_RATIO = 1.5f
    const val PAIR_MAX_AREA_RATIO = 2.6f

    /** Below this radius (pixels) colour statistics are noise; the ball is left UNKNOWN. */
    const val MIN_CLASSIFY_RADIUS_PX = 3f

    /**
     * Ball radius as a fraction of the square root of the visible playing-surface area, used
     * when no table pose gives a better estimate. A 2.25" ball on a 2:1 table of short side W:
     * r = 1.125, area = 2·W², so r / sqrt(area) = 1.125 / (W·sqrt 2). For W = 44" (8 ft) that is
     * 1.125 / 62.23 = 0.0181; 7 ft (W = 39") gives 0.0204, 9 ft (W = 50") gives 0.0159.
     */
    const val RADIUS_PER_SQRT_SURFACE_AREA = 0.0181f

    /** Expected ball radius in pixels from the visible playing-surface area in pixels. */
    fun fallbackRadius(surfaceAreaPx: Double): Float =
        (sqrt(surfaceAreaPx) * RADIUS_PER_SQRT_SURFACE_AREA).toFloat()

    sealed interface Verdict {
        /** Not a ball. */
        data object Reject : Verdict
        /** One ball, centred on the island's centroid. */
        data object Single : Verdict
        /** Two touching balls; see [splitPair]. */
        data object Pair : Verdict
    }

    /**
     * @param area island area in pixels
     * @param width bounding-box width in pixels
     * @param height bounding-box height in pixels
     * @param expectedRadius ball radius expected at the island, in pixels
     */
    fun judge(area: Int, width: Int, height: Int, expectedRadius: Float): Verdict {
        if (expectedRadius <= 0f || width <= 0 || height <= 0 || area <= 0) return Verdict.Reject
        val disk = PI * expectedRadius * expectedRadius
        val ratio = area / disk
        val aspect = max(width, height).toFloat() / min(width, height).toFloat()
        val fill = area.toFloat() / (width * height).toFloat()
        if (fill < MIN_FILL) return Verdict.Reject

        if (ratio in MIN_AREA_RATIO.toDouble()..MAX_AREA_RATIO.toDouble() && aspect <= MAX_ASPECT &&
            max(width, height) <= expectedRadius * 2.6f
        ) return Verdict.Single

        if (ratio in PAIR_MIN_AREA_RATIO.toDouble()..PAIR_MAX_AREA_RATIO.toDouble()) return Verdict.Pair

        return Verdict.Reject
    }

    /**
     * Centres of two touching balls: one ball radius either side of the island's centroid, along
     * its principal axis (from the central second moments mu20, mu02, mu11). Works at any angle,
     * which the bounding box does not.
     *
     * @return x1, y1, x2, y2
     */
    fun splitPair(cx: Float, cy: Float, mu20: Double, mu02: Double, mu11: Double, radius: Float): FloatArray {
        val theta = 0.5 * kotlin.math.atan2(2.0 * mu11, mu20 - mu02)
        val dx = (kotlin.math.cos(theta) * radius).toFloat()
        val dy = (kotlin.math.sin(theta) * radius).toFloat()
        return floatArrayOf(cx - dx, cy - dy, cx + dx, cy + dy)
    }

    /**
     * Names a ball from its pixel make-up (fractions of the pixels inside the ball's disk).
     * Rotation-invariant on purpose: a stripe's band lies at any angle, so "white at both
     * poles" tests are wrong about half the time.
     *
     * - CUE: mostly white.
     * - EIGHT: mostly dark and little white (the number disc).
     * - STRIPE: a real share of white (the caps) beside colour.
     * - SOLID: everything else.
     *
     * @param white fraction with low saturation and high value
     * @param dark fraction with low value
     */
    fun classify(white: Float, dark: Float, radiusPx: Float): BallKind {
        if (radiusPx < MIN_CLASSIFY_RADIUS_PX) return BallKind.UNKNOWN
        return when {
            white >= 0.6f -> BallKind.CUE
            dark >= 0.5f && white < 0.2f -> BallKind.EIGHT
            white >= 0.2f -> BallKind.STRIPE
            else -> BallKind.SOLID
        }
    }

    /** Mirrors data.BallType without depending on the data layer. */
    enum class BallKind { UNKNOWN, SOLID, STRIPE, CUE, EIGHT }
}
