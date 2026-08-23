package com.hereliesaz.cuedetat.core.physics

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.radians
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Cue-tip contact point, as the fraction of the **miscue limit** in each axis.
 *
 * A spin dial in the UI is naturally read this way: `side = 1.0` is as far right
 * as you can strike without miscuing, not one full ball radius (which is
 * physically impossible to hit). The miscue limit is about half a ball radius —
 * beyond roughly `R/2` the tip slides off however much chalk is on it.
 *
 * @param side  −1 (maximum left english) … +1 (maximum right english)
 * @param vertical −1 (maximum draw) … +1 (maximum follow)
 */
data class Spin(val side: Double = 0.0, val vertical: Double = 0.0) {

    init {
        require(side.isFinite() && vertical.isFinite()) { "spin must be finite" }
    }

    /** Magnitude of the tip offset, clamped to the miscue circle. */
    val magnitude: Double get() = hypot(side, vertical)

    /** True when this tip offset lies outside the miscue limit. */
    val wouldMiscue: Boolean get() = magnitude > 1.0

    /** Clamped into the miscue circle, preserving direction. */
    fun clampedToMiscueLimit(): Spin {
        val m = magnitude
        return if (m <= 1.0) this else Spin(side / m, vertical / m)
    }

    /** Tip offset as a fraction of the ball radius. */
    val offsetFractionOfRadius: Double get() = side * MISCUE_LIMIT_FRACTION
    val verticalOffsetFractionOfRadius: Double get() = vertical * MISCUE_LIMIT_FRACTION

    /**
     * Equatorial surface speed produced by the side spin.
     *
     * Striking a ball at offset `b` from centre imparts `omega = 5·b·v / (2·R²)`,
     * so the surface speed at the equator is `omega·R = (5/2)·(b/R)·v`.
     */
    fun surfaceSpeed(ballSpeed: Speed, ball: BallSpec = BallSpec.AMERICAN_POOL): Speed =
        Speed(2.5 * offsetFractionOfRadius * ballSpeed.metersPerSecond)

    /** Spin about the vertical axis, in radians per second. */
    fun angularVelocity(ballSpeed: Speed, ball: BallSpec = BallSpec.AMERICAN_POOL): Double =
        2.5 * offsetFractionOfRadius * ballSpeed.metersPerSecond / ball.radius.meters

    companion object {
        val NONE = Spin(0.0, 0.0)

        /**
         * The miscue limit as a fraction of ball radius. Striking further than
         * about half a radius from centre slides the tip off the ball.
         */
        const val MISCUE_LIMIT_FRACTION = 0.5

        val MAX_RIGHT = Spin(side = 1.0)
        val MAX_LEFT = Spin(side = -1.0)
        val MAX_FOLLOW = Spin(vertical = 1.0)
        val MAX_DRAW = Spin(vertical = -1.0)
    }
}

/**
 * Properties of the cue that affect where the ball actually goes.
 *
 * [squirtAtFullRadius] is the cue-ball deflection ("squirt") that striking one
 * full ball radius off centre would produce — a hypothetical reference, since
 * the miscue limit is around half that. It is a property of the shaft's end
 * mass: a heavy maple shaft squirts about twice as much as a low-deflection one.
 */
data class CueSpec(
    val squirtAtFullRadius: Angle = 8.0.degrees,
    val label: String = "Standard",
) {
    /**
     * Cue-ball deflection at tip contact for the given [spin].
     *
     * **This is applied at the cue tip, before the ball travels anywhere.** The
     * previous implementation applied its squirt term at the *object ball*, after
     * contact, which is backwards: squirt happens when the tip strikes, so it
     * changes the direction the cue ball sets off in — and therefore changes
     * where you must aim. Applying it post-collision meant english altered the
     * predicted cue-ball path but never the aim line the app told you to shoot
     * along, which is the opposite of what a player has to do.
     *
     * Sign convention: right english (`side > 0`) deflects the ball to the left.
     */
    fun squirtAngle(spin: Spin): Angle =
        Angle(-squirtAtFullRadius.radians * spin.offsetFractionOfRadius)

    companion object {
        val STANDARD = CueSpec(8.0.degrees, "Standard maple")
        val LOW_DEFLECTION = CueSpec(4.0.degrees, "Low deflection")
        val BREAK = CueSpec(10.0.degrees, "Break cue")
    }
}

/**
 * Cloth and cushion parameters. All SI, all measurable.
 */
data class ClothSpec(
    /** Rolling resistance deceleration, m/s². Typical worsted cloth ≈ 0.2–0.4 g. */
    val rollingDeceleration: Double = 0.25 * GRAVITY,
    /** Sliding friction coefficient between ball and cloth. */
    val slidingFriction: Double = 0.2,
    /** Cushion coefficient of restitution at moderate speed. */
    val cushionRestitution: Double = 0.80,
    val label: String = "Worsted",
) {
    companion object {
        val WORSTED = ClothSpec()
        val NAPPED = ClothSpec(
            rollingDeceleration = 0.4 * GRAVITY,
            slidingFriction = 0.25,
            cushionRestitution = 0.75,
            label = "Napped",
        )
    }
}

const val GRAVITY: Double = 9.80665

/**
 * Cushion rebound.
 *
 * A real cushion is not a mirror. Two effects dominate and both are modelled:
 *
 *  - **Restitution** removes speed, more at higher speed.
 *  - **Rail throw** — side spin rubbing against the cushion shifts the rebound
 *    angle, and the cushion also imparts spin. This is what "running english"
 *    and "reverse english" mean at the rail.
 *
 * The rebound angle also *shortens* relative to the incident angle as speed
 * rises, because the cushion compresses further and the ball leaves with more
 * forward roll. That is captured by scaling the tangential component.
 */
object Cushion {

    data class Rebound(
        val direction: com.hereliesaz.cuedetat.core.geometry.Vec2,
        val speed: Speed,
        val spin: Spin,
    )

    fun rebound(
        incoming: com.hereliesaz.cuedetat.core.geometry.Vec2,
        inwardNormal: com.hereliesaz.cuedetat.core.geometry.Vec2,
        speed: Speed,
        spin: Spin,
        cloth: ClothSpec = ClothSpec.WORSTED,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Rebound {
        val dir = incoming.normalized()
        val n = inwardNormal.normalized()
        val t = n.perpendicular

        val vNormal = dir dot n            // negative: approaching
        val vTangent = dir dot t

        // Restitution falls with impact speed; the fit is normalised so the
        // nominal value applies around 2 m/s.
        val impactSpeed = abs(vNormal) * speed.metersPerSecond
        val e = (cloth.cushionRestitution - 0.02 * (impactSpeed - 2.0)).coerceIn(0.6, 0.95)

        // Side spin at the cushion adds a tangential impulse: running english
        // speeds the ball along the rail, reverse english drags it back.
        val surfaceSpeed = spin.surfaceSpeed(speed, ball).metersPerSecond
        val railThrow = RAIL_THROW_COEFFICIENT * surfaceSpeed / speed.metersPerSecond.coerceAtLeast(1e-6)

        val outNormal = -vNormal * e
        val outTangent = vTangent * TANGENT_RETENTION + railThrow

        val outDir = (n * outNormal + t * outTangent).normalized()
        val outSpeed = Speed(
            speed.metersPerSecond * sqrt(outNormal * outNormal + outTangent * outTangent)
        )

        // The cushion reverses part of the side spin.
        val outSpin = spin.copy(side = spin.side * SPIN_RETENTION)

        return Rebound(outDir, outSpeed, outSpin)
    }

    /**
     * Fraction of tangential velocity retained through a cushion contact. Below
     * one because the cushion rubs; this is why banks "shorten" slightly.
     */
    private const val TANGENT_RETENTION = 0.92

    /** How strongly equatorial surface speed shifts the rebound along the rail. */
    private const val RAIL_THROW_COEFFICIENT = 0.10

    /** Side spin surviving a cushion contact. */
    private const val SPIN_RETENTION = 0.55
}
