package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The first guess at which way the table lies, for placing the AR preview before the user (or a
 * confident felt fit) has said anything. Pure Kotlin; the ARCore side supplies the candidates.
 *
 * **What a heading is.** Degrees clockwise, seen from above, from the way the camera faces to the
 * table's long axis (its "away" axis: the long side runs away from the player). 0 is a table lying
 * straight ahead; +90 has its far end to the right of the camera. This is the same quantity as the
 * table rotation the app keeps (`worldRotationDegrees`, and what `TablePosePrior` remembers), so a
 * remembered rotation is a heading without conversion.
 *
 * **Three sources, trusted differently** (see [Source]):
 *  - [Source.MEMORY]: where this table, or one near here, or the last one, lay: it knows which end is
 *    which, so it is the only source that can say. Its confidence is the prior's own.
 *  - [Source.PLANE]: the long axis of the horizontal plane ARCore found under the table. Weak: a plane
 *    is as much floor and clutter as table, so it is capped at [PLANE_CONFIDENCE_CAP], and it cannot
 *    tell the two ends apart.
 *  - The **felt** is not a candidate here. It measures where the table is as well as which way, and
 *    the AR session applies it directly (`seedPreviewFromQuad`) when it is sure; what it cannot tell is
 *    which end is which, and this guess is what it is given to settle that with.
 *
 * The result is a starting point: the AR session still lets a run of sure felt fits correct it.
 */
object TableOrientationGuess {

    enum class Source { MEMORY, PLANE }

    /**
     * One source's view. [ambiguousEnds]: the heading is only known modulo a half-turn (a plane or a
     * felt outline looks the same turned the other way end for end).
     */
    data class Candidate(
        val source: Source,
        val headingDeg: Float,
        val ambiguousEnds: Boolean,
        val confidence: Float,
    )

    data class Guess(val headingDeg: Float, val confidence: Float, val sources: Set<Source>)

    /** Below this a candidate is noise and is dropped. */
    const val MIN_CONFIDENCE = 0.15f

    /** Candidates this close (degrees) are the same answer. */
    const val AGREE_DEG = 20f

    /** The AR session applies a guess at or above this; below it the camera-facing default stands. */
    const val APPLY_CONFIDENCE = 0.3f

    /** A plane never speaks louder than this, so it cannot outvote a remembered table. */
    const val PLANE_CONFIDENCE_CAP = 0.4f

    /**
     * The guess from [candidates], or null when none is worth having.
     *
     * 1. Candidates under [MIN_CONFIDENCE] go.
     * 2. An ambiguous one is turned to the end that agrees with the most confident unambiguous one
     *    (memory), or, with none, to the end nearer the camera-facing 0, which is how a table is
     *    normally played from.
     * 3. The most confident leads. Those within [AGREE_DEG] of it back it: their confidences combine
     *    as independent evidence (`1 - prod(1 - c)`), the heading is their confidence-weighted
     *    circular mean. A disagreeing source is not ignored: the combined confidence is cut by half
     *    the most confident dissenter's.
     */
    fun choose(candidates: List<Candidate>): Guess? {
        val live = candidates.filter { it.confidence >= MIN_CONFIDENCE && it.headingDeg.isFinite() }
        if (live.isEmpty()) return null

        val anchor = live.filter { !it.ambiguousEnds }.maxByOrNull { it.confidence }
        val resolved = live.map { c ->
            if (!c.ambiguousEnds) c.copy(headingDeg = wrap(c.headingDeg))
            else c.copy(headingDeg = wrap(endNearest(c.headingDeg, anchor?.headingDeg ?: 0f)), ambiguousEnds = false)
        }

        val lead = resolved.maxByOrNull { it.confidence } ?: return null
        val (backing, dissent) = resolved.partition { abs(wrap(it.headingDeg - lead.headingDeg)) <= AGREE_DEG }
        var miss = 1f
        backing.forEach { miss *= 1f - it.confidence }
        var confidence = 1f - miss
        dissent.maxOfOrNull { it.confidence }?.let { confidence *= 1f - 0.5f * it }

        return Guess(
            headingDeg = circularMean(backing),
            confidence = confidence.coerceIn(0f, 1f),
            sources = backing.map { it.source }.toSet(),
        )
    }

    /**
     * The heading, in clockwise degrees, of the horizontal direction [axis] against a camera facing
     * [away] with [right] to its right (the table preview's own basis: `right = (-away.z, away.x)`).
     * The vertical part of [axis] is ignored. 0 when [axis] runs the way the camera faces, +90 when it
     * runs to the camera's right.
     */
    fun headingOf(axis: Vec3, right: Vec3, away: Vec3): Float {
        val along = axis.x * away.x + axis.z * away.z
        val across = axis.x * right.x + axis.z * right.z
        return Math.toDegrees(atan2(across.toDouble(), along.toDouble())).toFloat()
    }

    /** Of [headingDeg] and its opposite end, the one nearer [reference]. */
    private fun endNearest(headingDeg: Float, reference: Float): Float {
        val other = headingDeg + 180f
        return if (abs(wrap(headingDeg - reference)) <= abs(wrap(other - reference))) headingDeg else other
    }

    private fun circularMean(group: List<Candidate>): Float {
        var x = 0.0
        var y = 0.0
        for (c in group) {
            val a = c.headingDeg * PI / 180.0
            x += c.confidence * cos(a)
            y += c.confidence * sin(a)
        }
        return wrap(Math.toDegrees(atan2(y, x)).toFloat())
    }

    /** [deg] in (-180, 180]. */
    internal fun wrap(deg: Float): Float {
        var a = deg % 360f
        if (a <= -180f) a += 360f
        if (a > 180f) a -= 360f
        return a
    }
}

/**
 * The shape of a detected horizontal plane's outline, to say which way a table might lie on it.
 * Pure Kotlin: the caller passes the polygon ARCore gives (x, z pairs in the plane's own frame).
 */
object PlaneShape {

    /**
     * The long axis of the smallest rectangle (by area) that holds the outline.
     *
     * @property angleDeg direction of the long side in the plane's frame, from its x axis toward its
     *   z axis, in [0, 180).
     * @property longSide the rectangle's long side, same unit as the polygon.
     * @property shortSide its short side.
     * @property rectangularity polygon area over the rectangle's area, 0 to 1: how much the outline
     *   fills its box (a table top is near 1; an L-shaped or round plane is not).
     */
    data class Box(val angleDeg: Float, val longSide: Float, val shortSide: Float, val rectangularity: Float)

    /** The minimum-area bounding box of [xz] (x0, z0, x1, z1, ...), or null for under 3 points or no area. */
    fun boundingBox(xz: FloatArray): Box? {
        val n = xz.size / 2
        if (n < 3 || xz.any { !it.isFinite() }) return null
        val area = polygonArea(xz, n)
        if (area <= 1e-9) return null

        var best: Box? = null
        var bestArea = Double.MAX_VALUE
        // The minimum-area rectangle has a side along a polygon edge; trying a one-degree sweep is
        // within a degree of that for any outline ARCore returns, and needs no hull.
        for (step in 0 until 180) {
            val a = Math.toRadians(step.toDouble())
            val c = cos(a)
            val s = sin(a)
            var minU = Double.MAX_VALUE
            var maxU = -Double.MAX_VALUE
            var minV = Double.MAX_VALUE
            var maxV = -Double.MAX_VALUE
            for (i in 0 until n) {
                val x = xz[2 * i].toDouble()
                val z = xz[2 * i + 1].toDouble()
                val u = x * c + z * s
                val v = -x * s + z * c
                if (u < minU) minU = u
                if (u > maxU) maxU = u
                if (v < minV) minV = v
                if (v > maxV) maxV = v
            }
            val du = maxU - minU
            val dv = maxV - minV
            val boxArea = du * dv
            if (boxArea < bestArea) {
                bestArea = boxArea
                // u runs along the sweep angle; the long side is whichever of u, v is longer.
                best = if (du >= dv) Box(step.toFloat(), du.toFloat(), dv.toFloat(), (area / boxArea).toFloat())
                else Box(((step + 90) % 180).toFloat(), dv.toFloat(), du.toFloat(), (area / boxArea).toFloat())
            }
        }
        return best
    }

    /**
     * How far to trust [box] as a table's orientation, 0 to [TableOrientationGuess.PLANE_CONFIDENCE_CAP]:
     * a table top is a rectangle (high [Box.rectangularity]) about twice as long as it is wide, of
     * about the table's real size. Anything else is a floor or a wall of clutter and says nothing.
     *
     * @param tableLong the table's long side in the polygon's unit.
     * @param tableShort its short side.
     */
    fun confidence(box: Box, tableLong: Float, tableShort: Float): Float {
        if (tableLong <= 0f || tableShort <= 0f || box.shortSide <= 0f) return 0f
        val rectangular = ((box.rectangularity - 0.6f) / 0.35f).coerceIn(0f, 1f)
        // How far the outline's aspect is from the table's, as a ratio of ratios.
        val aspectRatio = (box.longSide / box.shortSide) / (tableLong / tableShort)
        val aspect = (1f - abs(kotlin.math.ln(aspectRatio.toDouble())).toFloat() / 0.7f).coerceIn(0f, 1f)
        // Size: within about a third of the table's, either way. A plane much bigger is the floor.
        val sizeRatio = box.longSide / tableLong
        val size = (1f - abs(kotlin.math.ln(sizeRatio.toDouble())).toFloat() / 0.5f).coerceIn(0f, 1f)
        return TableOrientationGuess.PLANE_CONFIDENCE_CAP * rectangular * aspect * size
    }

    private fun polygonArea(xz: FloatArray, n: Int): Double {
        var sum = 0.0
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += xz[2 * i].toDouble() * xz[2 * j + 1] - xz[2 * j].toDouble() * xz[2 * i + 1]
        }
        return abs(sum) / 2.0
    }
}
