package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.aim.AimSolver
import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Pocket
import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.physics.Contact
import com.hereliesaz.cuedetat.core.physics.CueSpec
import com.hereliesaz.cuedetat.core.physics.Spin
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.Speed
import com.hereliesaz.cuedetat.core.units.degrees
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Ranks the available shots.
 *
 * ## What changed
 *
 * The previous advisor scored shots with a set of hand-tuned constants —
 * `BANK_CONF = 0.55f`, `KICK_CONF = 0.4f`, `MIN_MAKE_PROBABILITY = 0.08f` — and
 * called the result a "make probability" without anything ever having been
 * calibrated against an outcome. It also passed `velocity = SAFETY_VELOCITY`
 * into a simulator that never read the parameter, so its "soft tap, so the cue
 * stays controllable" safety shot predicted precisely the same trajectory as a
 * full-power break.
 *
 * Here the difficulty of a pot falls out of geometry that is already modelled:
 *
 *  1. A pocket of effective width `w` at distance `d` from the object ball
 *     accepts a spread of object-ball directions of about `(w - ball) / d`.
 *  2. An aiming error at the cue ball is amplified on its way to the object
 *     ball: shifting the contact point by `e` swings the object ball by
 *     `e / (2R)`, and `e` grows with the cue-to-object distance.
 *  3. Together those give the angular precision the shot demands. Feeding that
 *     through a player's aiming spread — one tunable, [aimingSigma], with an
 *     obvious physical meaning — gives a number that behaves like a probability
 *     because it is one.
 */
class ShotAdvisor(
    private val table: Table,
    private val ball: BallSpec = BallSpec.AMERICAN_POOL,
    private val cue: CueSpec = CueSpec.STANDARD,
    /** One standard deviation of the player's aiming error. */
    private val aimingSigma: Angle = 0.6.degrees,
) {

    data class Candidate(
        val objectBall: Vec2,
        val pocket: Pocket,
        val solution: AimSolver.Solution,
        val makeProbability: Double,
        val cutAngle: Angle,
        val objectToPocket: Length,
        val cueToObject: Length,
    )

    data class Recommendation(
        val best: Candidate?,
        val ranked: List<Candidate>,
        /** True when nothing cleared [minimumProbability] and a safety is wiser. */
        val playSafe: Boolean,
    )

    /**
     * Evaluates every (ball × pocket) pair and ranks the legal ones.
     *
     * @param cueBall current cue-ball position.
     * @param targets the balls this player is on.
     * @param others every other ball on the table, which can obstruct.
     */
    fun recommend(
        cueBall: Vec2,
        targets: List<Vec2>,
        others: List<Vec2> = emptyList(),
        speed: Speed = Speed.MEDIUM,
        spin: Spin = Spin.NONE,
    ): Recommendation {
        val candidates = ArrayList<Candidate>()

        for (target in targets) {
            val blockers = (targets - target) + others
            for (pocket in table.pockets) {
                evaluate(cueBall, target, pocket, blockers, speed, spin)?.let { candidates += it }
            }
        }

        val ranked = candidates.sortedByDescending { it.makeProbability }
        val best = ranked.firstOrNull()
        return Recommendation(
            best = best?.takeIf { it.makeProbability >= minimumProbability },
            ranked = ranked,
            playSafe = best == null || best.makeProbability < safetyThreshold,
        )
    }

    private fun evaluate(
        cueBall: Vec2,
        objectBall: Vec2,
        pocket: Pocket,
        blockers: List<Vec2>,
        speed: Speed,
        spin: Spin,
    ): Candidate? {
        val target = pocket.mouthCenter
        val objectToPocket = objectBall.distanceTo(target)
        val cueToObject = cueBall.distanceTo(objectBall)
        if (objectToPocket.meters < 1e-6 || cueToObject.meters < 1e-6) return null

        val solution = AimSolver.solve(cueBall, objectBall, target, speed, spin, cue, ball)
        if (!solution.isPossible) return null

        // The object ball must actually be able to enter this pocket at the
        // angle the geometry forces on it.
        val approach = (target - objectBall).normalized()
        if (!pocket.accepts(target, approach, ball.radius)) return null

        // The cue ball must be able to reach the contact point, and the object
        // ball must be able to reach the pocket.
        if (AimSolver.firstObstruction(cueBall, solution.ghostBallCenter, blockers, ball) != null) {
            return null
        }
        if (AimSolver.firstObstruction(objectBall, target, blockers, ball) != null) return null

        val probability = makeProbability(
            pocket = pocket,
            approach = approach,
            objectToPocket = objectToPocket,
            cueToObject = cueToObject,
            cutAngle = solution.cutAngle,
        )

        return Candidate(
            objectBall = objectBall,
            pocket = pocket,
            solution = solution,
            makeProbability = probability,
            cutAngle = solution.cutAngle,
            objectToPocket = objectToPocket,
            cueToObject = cueToObject,
        )
    }

    /**
     * Probability that a shot of this geometry is made.
     *
     * Derived, not tuned: the pocket's angular acceptance divided by the aiming
     * error the geometry amplifies, run through a normal distribution.
     */
    internal fun makeProbability(
        pocket: Pocket,
        approach: Vec2,
        objectToPocket: Length,
        cueToObject: Length,
        cutAngle: Angle,
    ): Double {
        val effective = pocket.effectiveWidth(approach)
        val clearance = effective.meters - ball.diameter.meters
        if (clearance <= 0.0) return 0.0

        // Angular spread of object-ball directions the pocket still accepts.
        val acceptance = clearance / (2.0 * objectToPocket.meters)

        // A lateral error `e` at the contact point swings the object ball by
        // e/(2R). The cue ball's aim error becomes a lateral error proportional
        // to how far it has to travel, so precision demand grows with distance.
        val amplification = cueToObject.meters / (2.0 * ball.radius.meters)

        // Thin cuts also need more precision, because the contact point sweeps
        // faster across the object ball's face as the cut angle opens.
        val thinCutPenalty = 1.0 / kotlin.math.cos(cutAngle.radians).coerceAtLeast(0.08)

        val requiredPrecision = acceptance / (amplification * thinCutPenalty)
        return normalCdfTwoSided(requiredPrecision / aimingSigma.radians)
    }

    /** `P(|Z| < z)` for a standard normal — the fraction of attempts that land. */
    private fun normalCdfTwoSided(z: Double): Double {
        if (z <= 0.0) return 0.0
        return erf(z / sqrt(2.0)).coerceIn(0.0, 1.0)
    }

    /** Abramowitz & Stegun 7.1.26. Max absolute error 1.5e-7. */
    private fun erf(x: Double): Double {
        val sign = if (x < 0) -1.0 else 1.0
        val a = abs(x)
        val t = 1.0 / (1.0 + 0.3275911 * a)
        val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-a * a)
        return sign * y
    }

    /** Below this the advisor declines to recommend anything at all. */
    var minimumProbability: Double = 0.10

    /** Below this a safety is the better play even though a pot exists. */
    var safetyThreshold: Double = 0.35

    companion object {
        /** Cut angles beyond this are not worth evaluating. */
        val MAX_CUT: Angle = Contact.MAX_PRACTICAL_CUT
    }
}
