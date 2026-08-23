package com.hereliesaz.cuedetat.core.physics

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.geometry.angleBetween
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.radians
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/**
 * Ball-to-ball contact, including **cut-induced throw**.
 *
 * ## Why this exists
 *
 * The previous app aimed by pure ghost-ball geometry: place a phantom cue ball
 * touching the object ball on the line to the pocket, and shoot at its centre.
 * That construction ignores the single largest systematic error in real aiming.
 *
 * At contact the two balls are briefly sliding across one another. Friction acts
 * over that instant and pushes the object ball *off* the line of centres, by up
 * to about 5°. Ghost-ball aiming is therefore biased, always in the same
 * direction, and the bias is largest at exactly the medium cut angles that make
 * up most shots. Players compensate by feel; an app that claims to compute the
 * aim has no excuse not to compute this too.
 *
 * ## The model
 *
 * At impact the object ball receives a normal impulse along the line of centres
 * and a tangential impulse from friction. The tangential impulse is the smaller
 * of two limits:
 *
 *  - **friction-limited:** `mu(v_rel) * v_normal`, where `mu` falls off with
 *    relative surface speed (empirical fit below);
 *  - **rolling-limited:** `(2/7) * v_rel`, the most that can be transferred
 *    before the surfaces stop sliding and friction ceases to act.
 *
 * The throw angle is `atan(tangential / normal)`. This reproduces the shape
 * players know: throw peaks at slow speed and moderate cut, and washes out as
 * speed rises.
 *
 * Constants are from published measurements of ball-ball friction (the
 * exponential fit for `mu` against relative surface speed is the standard one
 * used in the billiards-physics literature). They are dimensional and stated in
 * SI, so unlike the old `K_THROW = 0.15f` "per logical unit" they mean something
 * and can be checked.
 */
object Contact {

    /**
     * Ball-ball coefficient of friction as a function of relative surface speed
     * in m/s. Falls from roughly 0.12 at rest to an asymptote near 0.01.
     */
    fun frictionCoefficient(relativeSurfaceSpeed: Speed): Double {
        val v = abs(relativeSurfaceSpeed.metersPerSecond)
        return 0.01 + 0.108 * exp(-1.088 * v)
    }

    /** The most tangential velocity that can be transferred before rolling. */
    private const val ROLLING_LIMIT_FRACTION = 2.0 / 7.0

    /**
     * Result of a cue-ball/object-ball collision.
     *
     * @param objectBallDirection true direction the object ball departs, i.e. the
     *   line of centres already corrected for throw.
     * @param cueBallDirection direction the cue ball departs.
     * @param throwAngle signed correction applied to the line of centres.
     * @param cutAngle angle between the cue ball's path and the line of centres.
     */
    data class Result(
        val objectBallDirection: Vec2,
        val cueBallDirection: Vec2,
        val throwAngle: Angle,
        val cutAngle: Angle,
    )

    /**
     * Resolves a collision.
     *
     * @param approach unit vector the cue ball is travelling along at contact.
     * @param lineOfCenters unit vector from cue-ball centre to object-ball centre.
     * @param speed cue-ball speed at contact.
     * @param spin cue-ball spin at contact.
     */
    fun resolve(
        approach: Vec2,
        lineOfCenters: Vec2,
        speed: Speed,
        spin: Spin = Spin.NONE,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Result {
        val a = approach.normalized()
        val loc = lineOfCenters.normalized()

        val cutAngle = angleBetween(a, loc)
        val cosCut = cos(cutAngle.radians)
        val sinCut = sin(cutAngle.radians)

        // Which side of the line of centres the cue ball is cutting from.
        // Positive means the object ball is thrown toward +perpendicular.
        val cutSide = if ((loc cross a) >= 0.0) 1.0 else -1.0

        val normalSpeed = speed.metersPerSecond * cosCut

        // Relative surface speed at the contact point has two contributions:
        // the tangential component of the cue ball's travel, and the surface
        // speed of any side spin. English can add to or cancel the sliding, which
        // is exactly why english changes throw — spin-induced throw.
        val tangentialFromCut = speed.metersPerSecond * sinCut * cutSide
        val tangentialFromSpin = spin.surfaceSpeed(speed, ball).metersPerSecond
        val relativeSurface = tangentialFromCut - tangentialFromSpin

        val mu = frictionCoefficient(Speed(abs(relativeSurface)))
        val frictionLimited = mu * normalSpeed
        val rollingLimited = ROLLING_LIMIT_FRACTION * abs(relativeSurface)
        val tangentialTransfer = min(frictionLimited, rollingLimited)

        val throwMagnitude =
            if (normalSpeed <= 1e-9) 0.0 else atan(tangentialTransfer / normalSpeed)

        // Friction opposes relative sliding, so the object ball is thrown
        // opposite the direction the cue-ball surface is moving.
        val throwSign = -sign(relativeSurface)
        val throwAngle = (throwMagnitude * throwSign).radians

        val objectBallDirection = loc.rotatedBy(throwAngle)

        // The cue ball leaves along the tangent line — perpendicular to the line
        // of centres — for a stun shot. Follow and draw rotate it back toward or
        // away from the original path immediately, not gradually: the separation
        // is a single impulse, and the old model's "start on the tangent then
        // curve toward the natural angle" got the region players care about most
        // (the first few inches after contact) systematically wrong.
        val cueBallDirection = cueBallDeparture(a, loc, cutAngle, spin, cutSide)

        return Result(
            objectBallDirection = objectBallDirection,
            cueBallDirection = cueBallDirection,
            throwAngle = throwAngle,
            cutAngle = cutAngle,
        )
    }

    /**
     * Cue-ball departure direction.
     *
     * For a stun shot the cue ball leaves exactly along the tangent line (the
     * 90° rule). Topspin or backspin surviving the collision adds a component
     * along the original path, rotating the departure off the tangent by the
     * *cue ball deflection angle*. With natural roll the classic result is a
     * departure roughly
     * `atan(tan(cut) * 5/7)` off the line of centres.
     */
    private fun cueBallDeparture(
        approach: Vec2,
        lineOfCenters: Vec2,
        cutAngle: Angle,
        spin: Spin,
        cutSide: Double,
    ): Vec2 {
        // Tangent line, oriented to the side the cue ball is heading.
        val tangent = lineOfCenters.perpendicular.normalized() * cutSide

        // Speed retained along the tangent after a stun contact.
        val tangentComponent = sin(cutAngle.radians)
        // Speed retained along the original direction from surviving roll/draw.
        // vertical = +1 full follow, -1 full draw. A rolling ball retains 5/7 of
        // its forward speed as the follow component after the collision.
        val forwardComponent = spin.vertical * (5.0 / 7.0) * cos(cutAngle.radians)

        val departure = tangent * tangentComponent + approach.normalized() * forwardComponent
        return if (departure.lengthSquared < 1e-18) tangent else departure.normalized()
    }

    /**
     * The angle by which throw shifts the *aim* for a given cut.
     *
     * This is the number the aim solver needs: to send the object ball along a
     * desired line, aim as if the target line were rotated the other way by this
     * much.
     */
    fun throwAngleFor(
        cutAngle: Angle,
        speed: Speed,
        spin: Spin = Spin.NONE,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Angle {
        val cosCut = cos(cutAngle.radians)
        val sinCut = sin(cutAngle.radians)
        val normalSpeed = speed.metersPerSecond * cosCut
        if (normalSpeed <= 1e-9) return Angle.ZERO

        val relativeSurface =
            speed.metersPerSecond * sinCut - spin.surfaceSpeed(speed, ball).metersPerSecond
        val mu = frictionCoefficient(Speed(abs(relativeSurface)))
        val transfer = min(mu * normalSpeed, ROLLING_LIMIT_FRACTION * abs(relativeSurface))
        return (atan(transfer / normalSpeed) * -sign(relativeSurface)).radians
    }

    /**
     * Cut angle needed for a ghost-ball construction, given the geometric angle
     * between the shot line and the object-ball-to-target line.
     */
    fun ghostBallCenter(
        objectBallCenter: Vec2,
        targetDirection: Vec2,
        ballRadius: Length,
    ): Vec2 = objectBallCenter - targetDirection.normalized() * (ballRadius.meters * 2.0)

    /** Largest cut that can still move a ball usefully; beyond this it is a miss. */
    val MAX_PRACTICAL_CUT: Angle = 85.0.degrees
}
