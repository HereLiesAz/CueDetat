package com.hereliesaz.cuedetat.core.geometry

import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.inches
import com.hereliesaz.cuedetat.core.units.meters
import com.hereliesaz.cuedetat.core.units.millimeters
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The physical table.
 *
 * ## What changed, and why it matters
 *
 * The previous model was an axis-aligned rectangle whose "pockets" were six bare
 * points, with rails that reflected perfectly off the rectangle's own edges —
 * edges that ran *through* the pocket points. Two consequences capped the whole
 * product's accuracy no matter how good the vision pipeline got:
 *
 *  - **Pocketing was a distance threshold.** Whether a ball "went in" was decided
 *    by proximity to a point, so the model could not express the single most
 *    important fact about real pockets: a ball approaching along the rail is
 *    rejected by the jaw even when it passes directly over the pocket centre.
 *  - **Cushions were in the wrong place.** Real cushion noses are inset from the
 *    pocket jaws; banking off the rectangle's corner geometry put every rail
 *    contact point slightly wrong, and the error grew toward the pockets.
 *
 * Here, cushions are nose segments that stop at the jaws, and pockets are
 * apertures with a real mouth width and a facing direction. Acceptance uses the
 * standard *effective pocket size* relation — a mouth of width `m` presents only
 * `m·cos(θ)` to a ball approaching `θ` off the pocket's axis — so shallow
 * approaches are rejected by geometry rather than by a tuned constant.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Balls
// ─────────────────────────────────────────────────────────────────────────────

/** Physical properties of a ball. Defaults are WPA-spec American pool. */
data class BallSpec(
    val radius: Length = 28.575.millimeters,
    /** Kilograms. WPA spec is 5½–6 oz; 163 g is mid-range. */
    val massKg: Double = 0.163,
) {
    val diameter: Length get() = radius * 2.0

    companion object {
        /** American pool: 2¼ in diameter, 5½–6 oz. */
        val AMERICAN_POOL = BallSpec()

        /** Blackball / English pool: 2 in diameter. */
        val ENGLISH_POOL = BallSpec(radius = 25.4.millimeters, massKg = 0.142)

        /** Carom / three-cushion: 61.5 mm diameter. */
        val CAROM = BallSpec(radius = 30.75.millimeters, massKg = 0.210)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pockets
// ─────────────────────────────────────────────────────────────────────────────

enum class PocketType { CORNER, SIDE }

/**
 * A pocket aperture.
 *
 * @param mouthCenter midpoint of the line joining the two jaw tips.
 * @param mouthWidth  jaw-tip to jaw-tip distance.
 * @param facing      unit vector pointing from the table into the pocket.
 */
data class Pocket(
    val mouthCenter: Vec2,
    val mouthWidth: Length,
    val facing: Vec2,
    val type: PocketType,
) {
    /** The line the ball centre must cross to drop, expressed as a segment. */
    val mouthLine: Segment
        get() {
            val half = facing.perpendicular.normalized() * (mouthWidth.meters / 2.0)
            return Segment(mouthCenter - half, mouthCenter + half)
        }

    /**
     * The width this pocket actually presents to a ball arriving along
     * [approach]. Standard pool geometry: a mouth of width `m` entered at `θ`
     * off the pocket axis presents an effective width of `m·cos(θ)`.
     *
     * Returns [Length.ZERO] for approaches at or beyond 90°, i.e. moving away.
     */
    fun effectiveWidth(approach: Vec2): Length {
        val cosTheta = approach.normalized() dot facing.normalized()
        return if (cosTheta <= 0.0) Length.ZERO else mouthWidth * cosTheta
    }

    /**
     * True when a ball of [ballRadius] whose centre is at [ballCenter], moving
     * along [approach], drops.
     *
     * Two independent conditions, both physical:
     *  1. the ball must *fit* through the width the pocket presents at this
     *     angle — this is what rejects a ball rolling along the rail;
     *  2. its centre must be laterally inside the remaining clearance.
     */
    fun accepts(ballCenter: Vec2, approach: Vec2, ballRadius: Length): Boolean {
        val effective = effectiveWidth(approach)
        val clearance = effective.meters / 2.0 - ballRadius.meters
        if (clearance <= 0.0) return false

        val lateralAxis = facing.perpendicular.normalized()
        val lateralOffset = abs((ballCenter - mouthCenter) dot lateralAxis)
        return lateralOffset <= clearance
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cushions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A cushion nose segment.
 *
 * [inwardNormal] is the unit normal pointing into the playing area, so a ball
 * approaching the cushion always has `direction · inwardNormal < 0`.
 */
data class Cushion(
    val segment: Segment,
    val inwardNormal: Vec2,
) {
    /**
     * The segment the *centre* of a ball of [ballRadius] travels along when it
     * is in contact with this cushion — the nose line pushed inward by one ball
     * radius. Collision detection runs against this, not the nose itself.
     */
    fun centerLine(ballRadius: Length): Segment {
        val offset = inwardNormal.normalized() * ballRadius.meters
        return Segment(segment.start + offset, segment.end + offset)
    }
}

/** A cushion contact: where the ball centre was, and the normal it bounced off. */
data class CushionHit(
    val cushion: Cushion,
    val contactCenter: Vec2,
    val distance: Length,
)

// ─────────────────────────────────────────────────────────────────────────────
// Table specification
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Table dimensions. [playLength] and [playWidth] are the *playing surface*
 * (cushion nose to cushion nose), not the slate or the cabinet.
 */
data class TableSpec(
    val playLength: Length,
    val playWidth: Length,
    val cornerMouth: Length = 4.5.inches,
    val sideMouth: Length = 5.0.inches,
    val ball: BallSpec = BallSpec.AMERICAN_POOL,
    val label: String,
) {
    init {
        require(playLength > playWidth) { "playLength must exceed playWidth" }
        require(cornerMouth > ball.diameter) { "corner pocket narrower than a ball" }
        require(sideMouth > ball.diameter) { "side pocket narrower than a ball" }
    }

    companion object {
        /** WPA regulation 9-foot: 100 × 50 in playing surface. */
        val NINE_FOOT = TableSpec(100.0.inches, 50.0.inches, label = "9 ft")

        /** 8-foot: 88 × 44 in. */
        val EIGHT_FOOT = TableSpec(88.0.inches, 44.0.inches, label = "8 ft")

        /** 7-foot bar box: 78 × 39 in. Bar tables commonly have tighter corners. */
        val SEVEN_FOOT = TableSpec(78.0.inches, 39.0.inches, label = "7 ft")

        val ALL = listOf(SEVEN_FOOT, EIGHT_FOOT, NINE_FOOT)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Table
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A table laid out in the table plane: centred on the origin, long axis along
 * `+x`, short axis along `+y`.
 *
 * Cushions and pockets are derived once in [spec] order and are immutable. The
 * old model recomputed rotated coordinates inside the data class *and* applied a
 * world rotation outside it, which is how the table used to rotate twice; here
 * the table simply has no opinion about how it is viewed.
 */
class Table(val spec: TableSpec) {

    val halfLength: Length = spec.playLength / 2.0
    val halfWidth: Length = spec.playWidth / 2.0

    /**
     * Distance from a corner point, measured along each rail, to that rail's jaw
     * tip. The corner mouth spans the corner at 45°, so a mouth of width `m` has
     * jaw tips `m/√2` back along each of the two rails it separates.
     */
    private val cornerJaw: Length = spec.cornerMouth / sqrt(2.0)

    private val hl = halfLength.meters
    private val hw = halfWidth.meters
    private val cj = cornerJaw.meters
    private val sideHalf = (spec.sideMouth / 2.0).meters

    val pockets: List<Pocket> = buildList {
        // Corner pockets. Mouth centre is the midpoint of the two jaw tips, which
        // sits inside the nominal corner by cornerJaw/2 on each axis.
        for (sx in listOf(-1.0, 1.0)) {
            for (sy in listOf(-1.0, 1.0)) {
                add(
                    Pocket(
                        mouthCenter = Vec2(sx * (hl - cj / 2.0), sy * (hw - cj / 2.0)),
                        mouthWidth = spec.cornerMouth,
                        facing = Vec2(sx, sy).normalized(),
                        type = PocketType.CORNER,
                    )
                )
            }
        }
        // Side pockets, at the midpoint of each long rail.
        for (sy in listOf(-1.0, 1.0)) {
            add(
                Pocket(
                    mouthCenter = Vec2(0.0, sy * hw),
                    mouthWidth = spec.sideMouth,
                    facing = Vec2(0.0, sy),
                    type = PocketType.SIDE,
                )
            )
        }
    }

    val cushions: List<Cushion> = buildList {
        // Long rails (y = ±halfWidth), each split in two by its side pocket.
        for (sy in listOf(-1.0, 1.0)) {
            val y = sy * hw
            val normal = Vec2(0.0, -sy)
            add(Cushion(Segment(Vec2(-hl + cj, y), Vec2(-sideHalf, y)), normal))
            add(Cushion(Segment(Vec2(sideHalf, y), Vec2(hl - cj, y)), normal))
        }
        // Short rails (x = ±halfLength), uninterrupted between the corner jaws.
        for (sx in listOf(-1.0, 1.0)) {
            val x = sx * hl
            val normal = Vec2(-sx, 0.0)
            add(Cushion(Segment(Vec2(x, -hw + cj), Vec2(x, hw - cj)), normal))
        }
    }

    /** True when a ball centre lies within the playing surface. */
    fun containsCenter(point: Vec2): Boolean =
        abs(point.x) <= hl && abs(point.y) <= hw

    /**
     * True when a ball of [radius] centred at [point] is fully on the table and
     * clear of the cushions — the legal region for placing a ball.
     */
    fun isLegalBallPosition(point: Vec2, radius: Length = spec.ball.radius): Boolean {
        val r = radius.meters
        return abs(point.x) <= hl - r && abs(point.y) <= hw - r
    }

    /**
     * The first cushion a ball of [ballRadius] starting at [from] and travelling
     * along [direction] would contact, or `null` if it reaches no cushion (which
     * means it is heading for a pocket mouth or is already off-table).
     */
    fun firstCushionHit(
        from: Vec2,
        direction: Vec2,
        ballRadius: Length = spec.ball.radius,
        maxDistance: Length = spec.playLength * 4.0,
    ): CushionHit? {
        val dir = direction.normalized()
        if (dir == Vec2.ZERO) return null

        var best: CushionHit? = null
        for (cushion in cushions) {
            // Only cushions we are approaching can be hit.
            if (dir dot cushion.inwardNormal >= 0.0) continue
            val line = cushion.centerLine(ballRadius)
            val hit = rayIntersectSegment(from, dir, line) ?: continue
            if (hit.distance.meters <= Vec2.EPSILON) continue
            if (hit.distance > maxDistance) continue
            if (best == null || hit.distance < best.distance) {
                best = CushionHit(cushion, hit.point, hit.distance)
            }
        }
        return best
    }

    /**
     * The pocket that accepts a ball of [ballRadius] travelling from [from]
     * along [direction], if any, considered only within [maxDistance].
     */
    fun pocketAlong(
        from: Vec2,
        direction: Vec2,
        ballRadius: Length = spec.ball.radius,
        maxDistance: Length = spec.playLength * 4.0,
    ): Pocket? {
        val dir = direction.normalized()
        var best: Pocket? = null
        var bestDistance = Double.MAX_VALUE
        for (pocket in pockets) {
            val hit = rayIntersectSegment(from, dir, pocket.mouthLine) ?: continue
            if (hit.distance.meters <= Vec2.EPSILON || hit.distance > maxDistance) continue
            if (!pocket.accepts(hit.point, dir, ballRadius)) continue
            if (hit.distance.meters < bestDistance) {
                bestDistance = hit.distance.meters
                best = pocket
            }
        }
        return best
    }

    /**
     * Diamond count along a rail, for the diamond systems players actually use.
     * Rails are divided into eight intervals by seven diamonds; the corners count
     * as 0 and 8. Returns a fractional position for [point] projected onto the
     * long axis.
     */
    fun diamondsAlongLength(point: Vec2): Double = (point.x + hl) / (2.0 * hl) * 8.0

    /** As [diamondsAlongLength], across the short axis (four intervals). */
    fun diamondsAcrossWidth(point: Vec2): Double = (point.y + hw) / (2.0 * hw) * 4.0
}

// ─────────────────────────────────────────────────────────────────────────────
// Segment / ray primitives
// ─────────────────────────────────────────────────────────────────────────────

data class Segment(val start: Vec2, val end: Vec2) {
    val delta: Vec2 get() = end - start
    val length: Length get() = delta.length
    val direction: Vec2 get() = delta.normalized()

    /** The point at parameter [t], where 0 is [start] and 1 is [end]. */
    fun pointAt(t: Double): Vec2 = start + delta * t

    /** Closest point on this segment to [point]. */
    fun closestPointTo(point: Vec2): Vec2 {
        val d = delta
        val lenSq = d.lengthSquared
        if (lenSq < Vec2.EPSILON) return start
        val t = ((point - start) dot d) / lenSq
        return pointAt(t.coerceIn(0.0, 1.0))
    }
}

data class RayHit(val point: Vec2, val distance: Length, val t: Double)

/**
 * Intersects the ray `from + t·direction` (t > 0) with [segment].
 *
 * Returns `null` when they are parallel or the crossing lies outside the
 * segment. [direction] need not be normalized; [RayHit.distance] is measured in
 * real units regardless.
 */
fun rayIntersectSegment(from: Vec2, direction: Vec2, segment: Segment): RayHit? {
    val dir = direction.normalized()
    val seg = segment.delta
    val denominator = dir cross seg
    if (abs(denominator) < 1e-12) return null

    val offset = segment.start - from
    val tRay = (offset cross seg) / denominator
    val tSeg = (offset cross dir) / denominator

    if (tRay < 0.0) return null
    if (tSeg < 0.0 || tSeg > 1.0) return null

    return RayHit(from + dir * tRay, tRay.meters, tSeg)
}

/**
 * Intersects the ray `from + t·direction` with the circle of [radius] at
 * [center], returning the nearer forward hit.
 */
fun rayIntersectCircle(
    from: Vec2,
    direction: Vec2,
    center: Vec2,
    radius: Length,
): RayHit? {
    val dir = direction.normalized()
    val toCenter = from - center
    val b = 2.0 * (toCenter dot dir)
    val c = toCenter.lengthSquared - radius.meters * radius.meters
    val discriminant = b * b - 4.0 * c
    if (discriminant < 0.0) return null

    val root = sqrt(discriminant)
    val t1 = (-b - root) / 2.0
    val t2 = (-b + root) / 2.0
    val t = when {
        t1 > Vec2.EPSILON -> t1
        t2 > Vec2.EPSILON -> t2
        else -> return null
    }
    return RayHit(from + dir * t, t.meters, t)
}

/** Reflects [direction] about a surface with unit normal [normal]. */
fun reflect(direction: Vec2, normal: Vec2): Vec2 {
    val n = normal.normalized()
    return direction - n * (2.0 * (direction dot n))
}

/** Angle of incidence measured from the surface normal, in `[0, pi/2]`. */
fun incidenceAngle(direction: Vec2, normal: Vec2): Angle =
    angleBetween(-direction.normalized(), normal.normalized())

/** Shortest distance from [point] to the infinite line through [segment]. */
fun distanceToLine(point: Vec2, segment: Segment): Length {
    val d = segment.direction
    val toPoint = point - segment.start
    return abs(toPoint cross d).meters
}
