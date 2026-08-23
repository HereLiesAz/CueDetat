package com.hereliesaz.cuedetat.core.units

import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Physical units for Cue d'État.
 *
 * ## Why this module exists
 *
 * The previous architecture had no unit at all. Its world was a plane of bare
 * `Float`s scaled from a constant, `LOGICAL_BALL_RADIUS = 25f`, and real inches
 * were converted *into* that arbitrary frame rather than the other way round.
 * Everything downstream inherited the ambiguity:
 *
 *  - spin decay constants were specified "per logical unit", so their physical
 *    meaning silently changed if anyone edited the ball radius;
 *  - the on-screen distance readout was `1200f / screenRadiusInPixels` rendered
 *    as feet and inches, which meant moving the zoom slider changed the
 *    "physical" distance to the ball;
 *  - ARCore reports metres, OpenCV reports pixels, and neither could be
 *    reconciled with the third frame that was neither.
 *
 * Everything here is SI-backed. Metres, radians, metres per second. Imperial
 * values exist only as conversions at the edges, because pool tables are sold in
 * feet and balls are specified in inches.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Conversion constants
// ─────────────────────────────────────────────────────────────────────────────

const val METERS_PER_INCH: Double = 0.0254
const val INCHES_PER_FOOT: Double = 12.0

// ─────────────────────────────────────────────────────────────────────────────
// Length
// ─────────────────────────────────────────────────────────────────────────────

/** A distance, stored in metres. */
@JvmInline
value class Length(val meters: Double) : Comparable<Length> {

    val millimeters: Double get() = meters * 1_000.0
    val centimeters: Double get() = meters * 100.0
    val inches: Double get() = meters / METERS_PER_INCH
    val feet: Double get() = inches / INCHES_PER_FOOT

    operator fun plus(other: Length) = Length(meters + other.meters)
    operator fun minus(other: Length) = Length(meters - other.meters)
    operator fun times(scalar: Double) = Length(meters * scalar)
    operator fun div(scalar: Double) = Length(meters / scalar)

    /** Ratio of two lengths — dimensionless, so it is a plain [Double]. */
    operator fun div(other: Length): Double = meters / other.meters

    operator fun unaryMinus() = Length(-meters)
    override operator fun compareTo(other: Length): Int = meters.compareTo(other.meters)

    val absoluteValue: Length get() = Length(abs(meters))
    val isFinite: Boolean get() = meters.isFinite()

    /**
     * Renders as feet and inches, e.g. `4' 7"`. For user-facing readouts only —
     * never round-trip a formatted string back into a [Length].
     */
    fun formatImperial(): String {
        val totalInches = inches
        val ft = (totalInches / INCHES_PER_FOOT).toInt()
        val inch = (totalInches - ft * INCHES_PER_FOOT).roundToInt()
        // Carry a rounded 12" up into the next foot rather than printing 4' 12".
        return if (inch >= 12) "${ft + 1}' 0\"" else "$ft' $inch\""
    }

    /** Renders as centimetres, e.g. `139 cm`. */
    fun formatMetric(): String = "${centimeters.roundToInt()} cm"

    companion object {
        val ZERO = Length(0.0)
    }
}

val Double.meters: Length get() = Length(this)
val Double.centimeters: Length get() = Length(this / 100.0)
val Double.millimeters: Length get() = Length(this / 1_000.0)
val Double.inches: Length get() = Length(this * METERS_PER_INCH)
val Double.feet: Length get() = Length(this * INCHES_PER_FOOT * METERS_PER_INCH)

val Int.meters: Length get() = toDouble().meters
val Int.inches: Length get() = toDouble().inches
val Int.feet: Length get() = toDouble().feet

operator fun Double.times(length: Length): Length = Length(this * length.meters)

// ─────────────────────────────────────────────────────────────────────────────
// Angle
// ─────────────────────────────────────────────────────────────────────────────

/** A plane angle, stored in radians. */
@JvmInline
value class Angle(val radians: Double) : Comparable<Angle> {

    val degrees: Double get() = radians * 180.0 / PI

    operator fun plus(other: Angle) = Angle(radians + other.radians)
    operator fun minus(other: Angle) = Angle(radians - other.radians)
    operator fun times(scalar: Double) = Angle(radians * scalar)
    operator fun div(scalar: Double) = Angle(radians / scalar)
    operator fun div(other: Angle): Double = radians / other.radians
    operator fun unaryMinus() = Angle(-radians)
    override operator fun compareTo(other: Angle): Int = radians.compareTo(other.radians)

    val absoluteValue: Angle get() = Angle(abs(radians))

    /** Wrapped into `(-pi, pi]`. */
    fun normalized(): Angle {
        var r = radians
        val twoPi = 2.0 * PI
        r = r.mod(twoPi)
        if (r > PI) r -= twoPi
        return Angle(r)
    }

    companion object {
        val ZERO = Angle(0.0)
        val QUARTER_TURN = Angle(PI / 2.0)
        val HALF_TURN = Angle(PI)
        val FULL_TURN = Angle(2.0 * PI)
    }
}

val Double.radians: Angle get() = Angle(this)
val Double.degrees: Angle get() = Angle(this * PI / 180.0)
val Int.degrees: Angle get() = toDouble().degrees

// ─────────────────────────────────────────────────────────────────────────────
// Speed
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A scalar speed, stored in metres per second.
 *
 * Shot speed is a first-class quantity here because every interesting billiards
 * effect depends on it: draw and follow develop over a distance that scales with
 * speed, squirt and swerve trade off against it, and throw falls off as relative
 * surface speed rises. The previous code declared a `velocity` parameter and
 * never read it, which is why the shot advisor's "soft tap" safety shot
 * simulated exactly the same trajectory as a full-power break.
 */
@JvmInline
value class Speed(val metersPerSecond: Double) : Comparable<Speed> {

    val milesPerHour: Double get() = metersPerSecond * 2.236936
    val inchesPerSecond: Double get() = metersPerSecond / METERS_PER_INCH

    operator fun plus(other: Speed) = Speed(metersPerSecond + other.metersPerSecond)
    operator fun minus(other: Speed) = Speed(metersPerSecond - other.metersPerSecond)
    operator fun times(scalar: Double) = Speed(metersPerSecond * scalar)
    operator fun div(scalar: Double) = Speed(metersPerSecond / scalar)
    operator fun div(other: Speed): Double = metersPerSecond / other.metersPerSecond
    override operator fun compareTo(other: Speed): Int =
        metersPerSecond.compareTo(other.metersPerSecond)

    companion object {
        val ZERO = Speed(0.0)

        /**
         * Reference speeds, from published cue-ball speed measurements.
         * A lag shot is roughly 1 m/s; a normal pot 2–3 m/s; a hard break 8–11 m/s.
         */
        val SOFT = Speed(1.0)
        val MEDIUM = Speed(2.5)
        val FIRM = Speed(4.5)
        val BREAK = Speed(9.0)
    }
}

val Double.metersPerSecond: Speed get() = Speed(this)

// ─────────────────────────────────────────────────────────────────────────────
// Test / comparison helpers
// ─────────────────────────────────────────────────────────────────────────────

/** True when two lengths agree to within [tolerance]. */
fun Length.approximately(other: Length, tolerance: Length = 0.5.millimeters): Boolean =
    (this - other).absoluteValue <= tolerance

/** True when two angles agree to within [tolerance], accounting for wrap-around. */
fun Angle.approximately(other: Angle, tolerance: Angle = 0.01.degrees): Boolean =
    (this - other).normalized().absoluteValue <= tolerance
