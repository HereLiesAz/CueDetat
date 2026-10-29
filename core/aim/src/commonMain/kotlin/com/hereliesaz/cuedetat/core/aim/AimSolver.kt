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
import kotlin.math.sin

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
 *
 * Angles follow the right-handed table frame of [Contact] (positive =
 * counter-clockwise seen from above). Cut-induced throw pulls the object ball
 * toward the cue ball's side, so the corrected aim is *over*cut relative to the
 * naive ghost ball; right english squirts the cue ball left, so the cue is
 * pointed right of [Solution.cueBallPath].
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
        /**
         * Direction the object ball leaves along, as predicted by [Contact.resolve]
         * for the final aim. Equals the target line to within the fixed-point
         * residual after [ITERATIONS] passes (far below shootable precision).
         */
        val objectBallPath: Vec2,
        /**
         * Direction the cue ball leaves along after contact, or [Vec2.ZERO] when it
         * stops dead (straight stun shot) or the shot is degenerate.
         */
        val cueBallDeparture: Vec2,
        val cutAngle: Angle,
        /** Throw the final aim produces; positive = counter-clockwise. */
        val throwAngle: Angle,
        /** Squirt the chosen english produces; positive (left) for right english. */
        val squirtAngle: Angle,
        /**
         * False when the geometry cannot be made at all: cut beyond ~85°, or a
         * degenerate input (target on the object ball, cue ball already at the
         * ghost-ball position), in which case the direction vectors are [Vec2.ZERO].
         */
        val isPossible: Boolean,
    ) {
        /**
         * The tangent line at contact — where a stun cue ball goes: through
         * [ghostBallCenter], perpendicular to the line of centres from the ghost
         * ball to [objectBall], extending [length] each way.
         */
        fun tangentLine(objectBall: Vec2, length: Length): Segment {
            val tangent = (objectBall - ghostBallCenter).normalized().perpendicular
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

        // Degenerate: no target direction, or the cue ball already sits where the
        // ghost ball must go (no approach direction). Report impossible rather
        // than returning zero vectors flagged as a makeable shot.
        val naive = objectBall - desiredObjectPath * separation.meters
        if (desiredObjectPath == Vec2.ZERO || (naive - cueBall).normalized() == Vec2.ZERO) {
            return Solution(
                ghostBallCenter = naive,
                cueBallPath = Vec2.ZERO,
                aimDirection = Vec2.ZERO,
                objectBallPath = desiredObjectPath,
                cueBallDeparture = Vec2.ZERO,
                cutAngle = Angle.ZERO,
                throwAngle = Angle.ZERO,
                squirtAngle = cue.squirtAngle(spin),
                isPossible = false,
            )
        }

        // Seed: the naive ghost ball, uncorrected.
        var lineOfCenters = desiredObjectPath
        var ghost = objectBall - lineOfCenters * separation.meters

        // Fixed-point: the throw depends on the cut, the cut depends on where the
        // ghost ball sits, and the ghost ball sits where it must to pre-compensate
        // the throw. Iterate until it stops moving.
        repeat(ITERATIONS) {
            val approach = (ghost - cueBall).normalized()

            // Throw the collision *would* produce if aimed along this line.
            val predicted = Contact.resolve(
                approach = approach,
                lineOfCenters = lineOfCenters,
                speed = speed,
                spin = spin,
                ball = ball,
            )
            // Rotate the line of centres the other way by the same amount, so the
            // thrown object ball lands back on the line we actually want.
            lineOfCenters = desiredObjectPath.rotatedBy(-predicted.throwAngle)
            ghost = objectBall - lineOfCenters * separation.meters
        }

        val cueBallPath = (ghost - cueBall).normalized()
        val cutAngle = angleBetween(cueBallPath, lineOfCenters)

        // Squirt happens at the tip, so the cue must point *back* by the squirt
        // angle for the cue ball to set off down cueBallPath. Right english squirts
        // left (positive), so the cue points right (clockwise) of the path.
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
            throwAngle = final.throwAngle,
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
     * it is a more natural readout than a raw cut angle. At contact the cue ball's
     * centre line passes `2R·sin(cut)` from the object ball's centre, across the
     * `2R` width the two balls share when straight, so the overlap is
     * `1 − sin(cut)`: a half-ball hit is a 30° cut, a quarter-ball about 49°.
     */
    fun ballFraction(cutAngle: Angle): Double = (1.0 - sin(cutAngle.radians)).coerceIn(0.0, 1.0)

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
