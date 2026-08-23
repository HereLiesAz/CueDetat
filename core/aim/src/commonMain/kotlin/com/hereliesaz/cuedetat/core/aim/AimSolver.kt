package com.hereliesaz.cuedetat.core.aim

import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Segment
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.geometry.angleBetween
import com.hereliesaz.cuedetat.core.geometry.rayIntersectCircle
import com.hereliesaz.cuedetat.core.physics.Contact
import com.hereliesaz.cuedetat.core.physics.CueSpec
import com.hereliesaz.cuedetat.core.physics.Spin
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees
import kotlin.math.cos

/**
 * Solves "where do I actually have to aim?".
 *
 * The ghost-ball construction — put a phantom cue ball touching the object ball
 * on the line to the pocket, shoot at its centre — is where this starts, not
 * where it ends. Two physical effects move the real answer away from it, and the
 * previous app modelled neither in the aim:
 *
 *  - **Cut-induced throw** pushes the object ball off the line of centres by up
 *    to ~5°, always in the same direction for a given cut. The ghost ball must
 *    be placed to *pre-compensate*.
 *  - **Squirt** deflects the cue ball at the moment the tip strikes, so with
 *    english the cue ball does not travel down the line the cue is pointing.
 *    Aim and cue-ball path are different lines.
 *
 * Throw depends on the cut angle, and the cut angle depends on where you aim, so
 * the correction is a fixed point. Two or three iterations converge well inside
 * the precision anyone can shoot to.
 */
object AimSolver {

    /** How many correction passes to run. Convergence is geometric and fast. */
    private const val ITERATIONS = 4

    data class Solution(
        /** Where to place the phantom cue ball, throw already compensated. */
        val ghostBallCenter: Vec2,
        /** Direction the cue ball must physically travel to make the shot. */
        val cueBallPath: Vec2,
        /**
         * Direction to point the cue. Differs from [cueBallPath] by the squirt
         * the chosen english will produce.
         */
        val aimDirection: Vec2,
        /** Direction the object ball leaves along — by construction, the target line. */
        val objectBallPath: Vec2,
        /** Direction the cue ball leaves along after contact. */
        val cueBallDeparture: Vec2,
        val cutAngle: Angle,
        val throwAngle: Angle,
        val squirtAngle: Angle,
        /** False when the geometry cannot be made at all (cut beyond ~85°). */
        val isPossible: Boolean,
    ) {
        /** The tangent line at contact — where a stun cue ball goes. */
        fun tangentLine(from: Vec2, length: Length): Segment {
            val tangent = (ghostBallCenter - from).normalized().perpendicular
            return Segment(ghostBallCenter - tangent * length.meters, ghostBallCenter + tangent * length.meters)
        }
    }

    /**
     * @param cueBall centre of the cue ball.
     * @param objectBall centre of the ball being struck.
     * @param target the point the object ball should reach (usually a pocket mouth).
     */
    fun solve(
        cueBall: Vec2,
        objectBall: Vec2,
        target: Vec2,
        speed: Speed = Speed.MEDIUM,
        spin: Spin = Spin.NONE,
        cue: CueSpec = CueSpec.STANDARD,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Solution {
        val desiredObjectPath = (target - objectBall).normalized()
        val separation = ball.radius * 2.0

        // Seed: the naive ghost ball, uncorrected.
        var lineOfCenters = desiredObjectPath
        var ghost = objectBall - lineOfCenters * separation.meters
        var cutAngle = angleBetween((ghost - cueBall).normalized(), lineOfCenters)
        var throwAngle = Angle.ZERO

        // Fixed-point: the throw depends on the cut, the cut depends on where the
        // ghost ball sits, and the ghost ball sits where it must to pre-compensate
        // the throw. Iterate until it stops moving.
        repeat(ITERATIONS) {
            val approach = (ghost - cueBall).normalized()
            cutAngle = angleBetween(approach, lineOfCenters)

            // Throw the collision *would* produce if aimed along this line.
            val predicted = Contact.resolve(
                approach = approach,
                lineOfCenters = lineOfCenters,
                speed = speed,
                spin = spin,
                ball = ball,
            )
            throwAngle = predicted.throwAngle

            // Rotate the line of centres the other way by the same amount, so the
            // thrown object ball lands back on the line we actually want.
            lineOfCenters = desiredObjectPath.rotatedBy(-throwAngle)
            ghost = objectBall - lineOfCenters * separation.meters
        }

        val cueBallPath = (ghost - cueBall).normalized()
        cutAngle = angleBetween(cueBallPath, lineOfCenters)

        // Squirt happens at the tip, so the cue must point *back* by the squirt
        // angle for the cue ball to set off down cueBallPath.
        val squirt = cue.squirtAngle(spin)
        val aimDirection = cueBallPath.rotatedBy(-squirt)

        val final = Contact.resolve(cueBallPath, lineOfCenters, speed, spin, ball)

        return Solution(
            ghostBallCenter = ghost,
            cueBallPath = cueBallPath,
            aimDirection = aimDirection,
            objectBallPath = final.objectBallDirection,
            cueBallDeparture = final.cueBallDirection,
            cutAngle = cutAngle,
            throwAngle = throwAngle,
            squirtAngle = squirt,
            isPossible = cutAngle <= Contact.MAX_PRACTICAL_CUT,
        )
    }

    /**
     * The naive ghost-ball position, with no physics applied.
     *
     * Kept because it is what every aiming diagram in every instructional book
     * shows, and showing the player the difference between this and the corrected
     * position is genuinely useful teaching. It is not what the app aims with.
     */
    fun naiveGhostBall(
        objectBall: Vec2,
        target: Vec2,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Vec2 = objectBall - (target - objectBall).normalized() * (ball.radius.meters * 2.0)

    /**
     * True when [blocker] sits in the path of a ball of [ball] travelling from
     * [from] to [to]. Uses swept-circle clearance: the moving ball's centre must
     * stay at least one diameter from the blocker's centre.
     */
    fun isObstructed(
        from: Vec2,
        to: Vec2,
        blocker: Vec2,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Boolean {
        val path = Segment(from, to)
        val closest = path.closestPointTo(blocker)
        return closest.distanceTo(blocker) < ball.diameter
    }

    /**
     * The first ball in [others] that blocks the path, or `null`.
     */
    fun firstObstruction(
        from: Vec2,
        to: Vec2,
        others: List<Vec2>,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Vec2? = others
        .filter { isObstructed(from, to, it, ball) }
        .minByOrNull { it.distanceTo(from).meters }

    /**
     * Whether a shot is "straight" — within [tolerance] of a full-ball hit.
     * Straight shots have no throw and no tangent to speak of, and the UI should
     * stop drawing a cut indicator for them.
     */
    fun isStraight(
        cueBall: Vec2,
        objectBall: Vec2,
        target: Vec2,
        tolerance: Angle = 1.5.degrees,
    ): Boolean {
        val shot = (objectBall - cueBall).normalized()
        val onward = (target - objectBall).normalized()
        return angleBetween(shot, onward) <= tolerance
    }

    /**
     * Fraction of the object ball the cue ball actually overlaps, from 1.0 (full
     * ball, dead straight) down to 0.0 (a thin edge-clip at 90°).
     *
     * This is the "half-ball hit"/"quarter-ball hit" vocabulary players use, and
     * it is a more natural readout than a raw cut angle.
     */
    fun ballFraction(cutAngle: Angle): Double = cos(cutAngle.radians).coerceIn(0.0, 1.0)

    /**
     * Distance from the cue ball to first contact with the object ball, i.e. how
     * far the cue ball actually travels before the collision.
     */
    fun travelToContact(
        cueBall: Vec2,
        direction: Vec2,
        objectBall: Vec2,
        ball: BallSpec = BallSpec.AMERICAN_POOL,
    ): Length? = rayIntersectCircle(
        from = cueBall,
        direction = direction,
        center = objectBall,
        radius = ball.diameter,
    )?.distance
}
