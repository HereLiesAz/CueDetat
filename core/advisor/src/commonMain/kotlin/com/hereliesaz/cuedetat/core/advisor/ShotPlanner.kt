package com.hereliesaz.cuedetat.core.advisor

import com.hereliesaz.cuedetat.core.aim.AimSolver
import com.hereliesaz.cuedetat.core.geometry.BallSpec
import com.hereliesaz.cuedetat.core.geometry.Pocket
import com.hereliesaz.cuedetat.core.geometry.Segment
import com.hereliesaz.cuedetat.core.geometry.Table
import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.geometry.rayIntersectSegment
import com.hereliesaz.cuedetat.core.physics.CueSpec
import com.hereliesaz.cuedetat.core.physics.Spin
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.degrees
import com.hereliesaz.cuedetat.core.units.meters
import com.hereliesaz.cuedetat.core.units.Speed

/** How the cue ball's energy reaches the ball that drops. */
enum class ShotKind {
    /** The cue strikes the ball that drops. */
    DIRECT,

    /** The cue strikes the ball that drops, which rebounds off one cushion into a pocket. */
    BANK,

    /** The cue rebounds off one cushion and then strikes the ball that drops, because the straight line is blocked. */
    KICK,

    /** The cue strikes one of the shooter's balls, which drives another of theirs into a pocket. */
    COMBO,
}

/**
 * A shot the planner would play: what to hit, where to aim, where the balls go, and how likely it is.
 * Points are table-plane metres ([Vec2]). The planner offers only legal shots, so [firstBall] is
 * always one the shooter may strike first (see [GroupRules]).
 *
 * @property firstBall the ball the cue strikes first.
 * @property pocketedBall the ball that drops. The same ball as [firstBall] unless [kind] is [ShotKind.COMBO].
 * @property pocketIndex index into the planner's `table.pockets`.
 * @property ghostBallCenter where the cue ball's centre is at first contact (throw compensated).
 * @property cuePath the cue ball's centre, waypoint to waypoint, up to contact: `[cue, ghost]`, or
 *   `[cue, cushion, ghost]` for a kick.
 * @property firstBallPath where the struck ball's centre goes: `[ball, pocket]` for a direct pot or a
 *   kick, `[ball, cushion, pocket]` for a bank, `[ball, where it meets the second ball]` for a combination.
 * @property secondBallPath combinations only: the second ball's centre, `[ball, pocket]`; empty otherwise.
 * @property cutAngle the cut at the cue's contact with the first ball.
 * @property secondCutAngle combinations only: the cut where the first ball meets the second.
 * @property makeProbability the model probability, from [PrecisionModel]; not a measured one.
 */
data class ShotPlan(
    val kind: ShotKind,
    val firstBall: TableBall,
    val pocketedBall: TableBall,
    val pocketIndex: Int,
    val ghostBallCenter: Vec2,
    val cuePath: List<Vec2>,
    val firstBallPath: List<Vec2>,
    val secondBallPath: List<Vec2> = emptyList(),
    val cutAngle: Angle,
    val secondCutAngle: Angle? = null,
    val makeProbability: Double,
)

/**
 * Plans the shots on a table: direct pots, one-cushion banks, one-cushion kicks and legal two-ball
 * combinations, for the balls the shooter may play, ranked by [ShotPlan.makeProbability].
 *
 * Built on the pieces the core already has: [AimSolver] for the contact geometry (throw and squirt
 * included), [Table] for pockets and cushions, [GroupRules] for what is legal, [PrecisionModel] for how
 * likely.
 *
 * Banks and kicks mirror across the cushion's *centre line* (the nose pushed in by a ball radius: it is
 * the ball's centre that bounces, not its edge) and use the pure mirror law, so they assume the usual
 * diamond-system conditions: no english on the rebound, a medium pace. Rebound speed loss and spin
 * (`Cushion.rebound`) are not used yet, so a real bank at a different pace or with side spin lands
 * somewhere else; the probability carries a flat price per cushion, not a model of that error.
 *
 * Not offered yet: three or more balls in a chain, two cushions or more, safeties, and position play
 * (where the cue ball ends). The core has no cue-ball path after contact to score them with.
 *
 * The ghost ball of every contact must lie wholly on the table, so no shot is offered that needs the
 * cue ball (or a striking ball) somewhere it cannot be.
 *
 * @param balls every ball but the cue, with groups; the cue is passed to [plan].
 */
class ShotPlanner(
    private val table: Table,
    private val ball: BallSpec = BallSpec.AMERICAN_POOL,
    private val cue: CueSpec = CueSpec.STANDARD,
    private val aimingSigma: Angle = 0.6.degrees,
) {

    private val advisor = ShotAdvisor(table, ball, cue, aimingSigma)

    /** Below this no shot is recommended at all. Uncalibrated, as [ShotAdvisor]'s. */
    var minimumProbability: Double = 0.10

    /** Below this a safety would be wiser even though a shot exists. Uncalibrated. */
    var safetyThreshold: Double = 0.35

    data class Result(
        /** The most likely shot, or null when none clears [minimumProbability]. */
        val best: ShotPlan?,
        val ranked: List<ShotPlan>,
        /** True when nothing clears [safetyThreshold]. The planner cannot offer a safety itself yet. */
        val playSafe: Boolean,
    )

    fun plan(
        cueBall: Vec2,
        balls: List<TableBall>,
        speed: Speed = Speed.MEDIUM,
        spin: Spin = Spin.NONE,
    ): Result {
        val pottable = GroupRules.pottable(balls)
        val plans = ArrayList<ShotPlan>()

        for (first in pottable) {
            val others = balls.filter { it.id != first.id }.map { it.position }
            val everyone = balls.map { it.position }
            for ((index, pocket) in table.pockets.withIndex()) {
                direct(cueBall, first, index, pocket, others, speed, spin)?.let { plans += it }
                banks(cueBall, first, index, pocket, others, speed, spin, plans)
                kicks(cueBall, first, index, pocket, others, everyone, speed, spin, plans)
            }
        }
        combinations(cueBall, balls, speed, spin, plans)

        val ranked = plans.sortedByDescending { it.makeProbability }
        val top = ranked.firstOrNull()
        return Result(
            best = top?.takeIf { it.makeProbability >= minimumProbability },
            ranked = ranked,
            playSafe = top == null || top.makeProbability < safetyThreshold,
        )
    }

    // ── Direct ──────────────────────────────────────────────────────────────

    private fun direct(
        cueBall: Vec2, first: TableBall, index: Int, pocket: Pocket,
        others: List<Vec2>, speed: Speed, spin: Spin,
    ): ShotPlan? {
        val candidate = advisor.evaluate(cueBall, first.position, pocket, others, speed, spin) ?: return null
        val ghost = candidate.solution.ghostBallCenter
        if (!onTable(ghost)) return null
        return ShotPlan(
            kind = ShotKind.DIRECT,
            firstBall = first,
            pocketedBall = first,
            pocketIndex = index,
            ghostBallCenter = ghost,
            cuePath = listOf(cueBall, ghost),
            firstBallPath = listOf(first.position, pocket.mouthCenter),
            cutAngle = candidate.cutAngle,
            makeProbability = candidate.makeProbability,
        )
    }

    // ── Banks: the struck ball rebounds off one cushion into the pocket ────

    private fun banks(
        cueBall: Vec2, first: TableBall, index: Int, pocket: Pocket,
        others: List<Vec2>, speed: Speed, spin: Spin, out: MutableList<ShotPlan>,
    ) {
        val target = pocket.mouthCenter
        for (cushion in table.cushions) {
            val line = cushion.centerLine(ball.radius)
            val normal = cushion.inwardNormal.normalized()
            // Ball and pocket must both be on the playing side of the cushion.
            if (((first.position - line.start) dot normal) <= 0.0 || ((target - line.start) dot normal) <= 0.0) continue

            // Unfold: the ball reaches the pocket by going straight to its mirror image.
            val bounce = crossing(first.position, reflectAcross(target, line), line) ?: continue

            val solution = AimSolver.solve(cueBall, first.position, bounce, speed, spin, cue, ball)
            if (!solution.isPossible) continue
            val ghost = solution.ghostBallCenter
            if (!onTable(ghost)) continue

            val approach = (target - bounce).normalized()
            if (!pocket.accepts(target, approach, ball.radius)) continue
            if (AimSolver.firstObstruction(cueBall, ghost, others, ball) != null) continue
            if (AimSolver.firstObstruction(first.position, bounce, others, ball) != null) continue
            if (AimSolver.firstObstruction(bounce, target, others, ball) != null) continue

            val unfolded = first.position.distanceTo(bounce).meters + bounce.distanceTo(target).meters
            val probability = PrecisionModel.makeProbability(
                ball = ball,
                aimingSigma = aimingSigma,
                pocketEffectiveWidth = pocket.effectiveWidth(approach),
                finalPathLength = unfolded.meters,
                legs = listOf(PrecisionModel.Leg(cueBall.distanceTo(first.position), solution.cutAngle)),
                cushions = 1,
            )
            out += ShotPlan(
                kind = ShotKind.BANK,
                firstBall = first,
                pocketedBall = first,
                pocketIndex = index,
                ghostBallCenter = ghost,
                cuePath = listOf(cueBall, ghost),
                firstBallPath = listOf(first.position, bounce, target),
                cutAngle = solution.cutAngle,
                makeProbability = probability,
            )
        }
    }

    // ── Kicks: the cue rebounds off one cushion to reach a blocked ghost ball ─

    private fun kicks(
        cueBall: Vec2, first: TableBall, index: Int, pocket: Pocket,
        others: List<Vec2>, everyone: List<Vec2>, speed: Speed, spin: Spin, out: MutableList<ShotPlan>,
    ) {
        // Only when the straight shot is not there: a blocked line, or a cut that cannot be made.
        val straight = advisor.evaluate(cueBall, first.position, pocket, others, speed, spin)
        if (straight != null && onTable(straight.solution.ghostBallCenter)) return

        for (cushion in table.cushions) {
            kick(cueBall, first, index, pocket, cushion, others, everyone, speed, spin)?.let { out += it }
        }
    }

    private fun kick(
        cueBall: Vec2, first: TableBall, index: Int, pocket: Pocket, cushion: com.hereliesaz.cuedetat.core.geometry.Cushion,
        others: List<Vec2>, everyone: List<Vec2>, speed: Speed, spin: Spin,
    ): ShotPlan? {
        val target = pocket.mouthCenter
        val line = cushion.centerLine(ball.radius)
        val normal = cushion.inwardNormal.normalized()
        if (((cueBall - line.start) dot normal) <= 0.0) return null

        // The ghost ball depends on the direction the cue arrives from (throw), and the cushion point
        // on the ghost: a fixed point, which converges in a few passes.
        var solution = AimSolver.solve(cueBall, first.position, target, speed, spin, cue, ball)
        var bounce: Vec2? = null
        repeat(KICK_PASSES) {
            if (!solution.isPossible) return null
            val point = crossing(cueBall, reflectAcross(solution.ghostBallCenter, line), line) ?: return null
            bounce = point
            solution = AimSolver.solve(point, first.position, target, speed, spin, cue, ball)
        }
        val rebound = bounce ?: return null
        if (!solution.isPossible) return null
        val ghost = solution.ghostBallCenter
        if (((ghost - line.start) dot normal) <= 0.0 || !onTable(ghost)) return null
        val settled = crossing(cueBall, reflectAcross(ghost, line), line) ?: return null
        if (settled.distanceTo(rebound).meters > KICK_TOLERANCE) return null

        val approach = (target - first.position).normalized()
        if (!pocket.accepts(target, approach, ball.radius)) return null
        if (AimSolver.firstObstruction(cueBall, rebound, everyone, ball) != null) return null
        if (AimSolver.firstObstruction(rebound, ghost, others, ball) != null) return null
        if (AimSolver.firstObstruction(first.position, target, others, ball) != null) return null

        val cueTravel = cueBall.distanceTo(rebound).meters + rebound.distanceTo(first.position).meters
        val probability = PrecisionModel.makeProbability(
            ball = ball,
            aimingSigma = aimingSigma,
            pocketEffectiveWidth = pocket.effectiveWidth(approach),
            finalPathLength = first.position.distanceTo(target),
            legs = listOf(PrecisionModel.Leg(cueTravel.meters, solution.cutAngle)),
            cushions = 1,
        )
        return ShotPlan(
            kind = ShotKind.KICK,
            firstBall = first,
            pocketedBall = first,
            pocketIndex = index,
            ghostBallCenter = ghost,
            cuePath = listOf(cueBall, rebound, ghost),
            firstBallPath = listOf(first.position, target),
            cutAngle = solution.cutAngle,
            makeProbability = probability,
        )
    }

    // ── Combinations: cue -> first ball -> second ball -> pocket ────────────

    private fun combinations(
        cueBall: Vec2, balls: List<TableBall>, speed: Speed, spin: Spin, out: MutableList<ShotPlan>,
    ) {
        val mine = balls.filter { it.group == BallGroup.MINE }
        if (mine.size < 2) return

        for (second in mine) {
            for (first in mine) {
                if (!GroupRules.isLegalCombination(first, second)) continue
                for ((index, pocket) in table.pockets.withIndex()) {
                    combination(cueBall, first, second, index, pocket, balls, speed, spin)?.let { out += it }
                }
            }
        }
    }

    private fun combination(
        cueBall: Vec2, first: TableBall, second: TableBall, index: Int, pocket: Pocket,
        balls: List<TableBall>, speed: Speed, spin: Spin,
    ): ShotPlan? {
        val target = pocket.mouthCenter
        // Work back from the pocket: where must the first ball be when it meets the second?
        val meeting = AimSolver.solve(first.position, second.position, target, speed, spin, cue, ball)
        if (!meeting.isPossible) return null
        val meetingPoint = meeting.ghostBallCenter
        if (!onTable(meetingPoint)) return null

        val approach = (target - second.position).normalized()
        if (!pocket.accepts(target, approach, ball.radius)) return null

        // ...and where must the cue be when it meets the first ball, to send it there?
        val strike = AimSolver.solve(cueBall, first.position, meetingPoint, speed, spin, cue, ball)
        if (!strike.isPossible) return null
        val ghost = strike.ghostBallCenter
        if (!onTable(ghost)) return null

        // A ball is out of the way once it has moved: the first ball leaves its place, so it does not
        // block its own run or the second ball's; the second leaves its place for the pocket.
        val exceptFirst = balls.filter { it.id != first.id }.map { it.position }
        val exceptBoth = balls.filter { it.id != first.id && it.id != second.id }.map { it.position }
        if (AimSolver.firstObstruction(cueBall, ghost, exceptFirst, ball) != null) return null
        if (AimSolver.firstObstruction(first.position, meetingPoint, exceptBoth, ball) != null) return null
        if (AimSolver.firstObstruction(second.position, target, exceptBoth, ball) != null) return null

        val probability = PrecisionModel.makeProbability(
            ball = ball,
            aimingSigma = aimingSigma,
            pocketEffectiveWidth = pocket.effectiveWidth(approach),
            finalPathLength = second.position.distanceTo(target),
            legs = listOf(
                PrecisionModel.Leg(cueBall.distanceTo(first.position), strike.cutAngle),
                PrecisionModel.Leg(first.position.distanceTo(second.position), meeting.cutAngle),
            ),
        )
        return ShotPlan(
            kind = ShotKind.COMBO,
            firstBall = first,
            pocketedBall = second,
            pocketIndex = index,
            ghostBallCenter = ghost,
            cuePath = listOf(cueBall, ghost),
            firstBallPath = listOf(first.position, meetingPoint),
            secondBallPath = listOf(second.position, target),
            cutAngle = strike.cutAngle,
            secondCutAngle = meeting.cutAngle,
            makeProbability = probability,
        )
    }

    // ── Geometry ────────────────────────────────────────────────────────────

    /** A ball of the table's size centred at [point] lies wholly on the table. */
    private fun onTable(point: Vec2): Boolean = table.isLegalBallPosition(point, ball.radius)

    /** [point] mirrored across the infinite line through [line]. */
    private fun reflectAcross(point: Vec2, line: Segment): Vec2 {
        val normal = line.direction.perpendicular
        val distance = (point - line.start) dot normal
        return point - normal * (2.0 * distance)
    }

    /**
     * Where the straight run [from] -> [to] crosses [line], strictly between the two ends; null if it
     * does not cross within the segment (the segments stop at the pocket jaws, so a run into a pocket
     * mouth does not).
     */
    private fun crossing(from: Vec2, to: Vec2, line: Segment): Vec2? {
        val run = to - from
        val length = run.length.meters
        val hit = rayIntersectSegment(from, run, line) ?: return null
        return if (hit.distance.meters > EDGE && hit.distance.meters < length - EDGE) hit.point else null
    }

    private companion object {
        /** Passes of the kick's ghost-ball / cushion-point fixed point. Geometric convergence. */
        const val KICK_PASSES = 4

        /** How far apart, in metres, the cushion point may still be after the passes (a millimetre). */
        const val KICK_TOLERANCE = 1e-3

        /** Closest a crossing may be to either end of a run, in metres. */
        const val EDGE = 1e-9
    }
}
