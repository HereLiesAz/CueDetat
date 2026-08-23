package com.hereliesaz.cuedetat.core.geometry

import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.meters
import com.hereliesaz.cuedetat.core.units.radians
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A point or vector on the table plane.
 *
 * **Components are metres.** The table plane is a right-handed 2D frame whose
 * origin is the centre of the playing surface: `+x` runs along the long axis
 * (foot rail to head rail), `+y` runs across the short axis. This never changes
 * with zoom, device tilt, or which camera is active — it is the physical table,
 * not a view of it.
 */
data class Vec2(val x: Double, val y: Double) {

    val length: Length get() = hypot(x, y).meters
    val lengthSquared: Double get() = x * x + y * y

    /** Direction from the origin, measured counter-clockwise from `+x`. */
    val direction: Angle get() = atan2(y, x).radians

    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Double) = Vec2(x / scalar, y / scalar)
    operator fun unaryMinus() = Vec2(-x, -y)

    infix fun dot(other: Vec2): Double = x * other.x + y * other.y

    /** 2D cross product (the z component of the 3D cross). Sign gives turn direction. */
    infix fun cross(other: Vec2): Double = x * other.y - y * other.x

    /** Rotated 90° counter-clockwise. */
    val perpendicular: Vec2 get() = Vec2(-y, x)

    fun normalized(): Vec2 {
        val len = hypot(x, y)
        return if (len < EPSILON) ZERO else Vec2(x / len, y / len)
    }

    fun distanceTo(other: Vec2): Length = (this - other).length

    fun rotatedBy(angle: Angle): Vec2 {
        val c = cos(angle.radians)
        val s = sin(angle.radians)
        return Vec2(x * c - y * s, x * s + y * c)
    }

    /** Scales this vector to exactly [length]. */
    fun withLength(length: Length): Vec2 = normalized() * length.meters

    val isFinite: Boolean get() = x.isFinite() && y.isFinite()

    companion object {
        val ZERO = Vec2(0.0, 0.0)
        const val EPSILON = 1e-12

        /** Unit vector pointing at [angle] from `+x`. */
        fun polar(angle: Angle, radius: Length = 1.0.meters): Vec2 =
            Vec2(cos(angle.radians) * radius.meters, sin(angle.radians) * radius.meters)
    }
}

fun Vec2(x: Length, y: Length) = Vec2(x.meters, y.meters)

/** The unsigned angle between two directions, wrapped into `[0, pi]`. */
fun angleBetween(a: Vec2, b: Vec2): Angle {
    val na = a.normalized()
    val nb = b.normalized()
    // atan2 of the cross/dot pair is numerically stable at both 0 and pi, unlike acos(dot).
    return atan2(kotlin.math.abs(na cross nb), na dot nb).radians
}
