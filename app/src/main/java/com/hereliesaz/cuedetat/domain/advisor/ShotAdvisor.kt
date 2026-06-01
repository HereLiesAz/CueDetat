package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.SpinPhysicsCalculator
import com.hereliesaz.cuedetat.domain.Vector2
import com.hereliesaz.cuedetat.view.model.Table
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Pure, stateless shot evaluator. Phase 1: enumerate every (target ball × pocket) DIRECT pot,
 * reject the impossible/obstructed ones, score the rest by make-probability (cut angle dominant,
 * distance secondary), and return the best — or null when nothing clears the confidence floor.
 *
 * All geometry is in the logical plane (table centered at origin, inches). No app types, so the
 * whole thing is trivially unit-testable; a thin adapter extracts [AdvisorInput] from app state.
 */
class ShotAdvisor @Inject constructor() {

    fun recommend(input: AdvisorInput): RecommendedShot? {
        if (input.targetBalls.isEmpty() || input.pockets.isEmpty()) return null
        val r = input.ballRadius

        val candidates = ArrayList<RecommendedShot>()
        for (target in input.targetBalls) {
            for (pocket in input.pockets) {
                evaluateDirect(input.cue, target, pocket, input, r)?.let { candidates.add(it) }
            }
        }
        if (candidates.isEmpty()) return null

        val table = input.table
            ?: // Phase 1: no table → rank by make-probability only (stun).
            return candidates.maxByOrNull { it.makeProbability }
                ?.takeIf { it.makeProbability >= MIN_MAKE_PROBABILITY }

        // Phase 2: position-aware. For the strongest pots, choose the spin whose predicted cue
        // leave best sets up the next shot (avoiding scratches), then rank by a combined score.
        val topK = candidates.sortedByDescending { it.makeProbability }.take(POSITION_TOP_K)
        var best: RecommendedShot? = null
        var bestScore = -1f
        for (c in topK) {
            val refined = withPositionSpin(c, input, table, r)
            val score = refined.makeProbability * (1f + POSITION_WEIGHT * refined.positionScore)
            if (score > bestScore) {
                bestScore = score
                best = refined
            }
        }
        return best?.takeIf { it.makeProbability >= MIN_MAKE_PROBABILITY }
    }

    /** Pick the spin offset whose predicted cue leave best sets up the next shot; attach it. */
    private fun withPositionSpin(shot: RecommendedShot, input: AdvisorInput, table: Table, r: Float): RecommendedShot {
        val cueV = Vector2(input.cue.x, input.cue.y)
        val targetV = Vector2(shot.targetPos.x, shot.targetPos.y)
        val shotAngle = atan2(shot.ghostCuePos.y - input.cue.y, shot.ghostCuePos.x - input.cue.x)
        val scratchRadius = r * 1.8f

        var bestSpin = PointF(0f, 0f)
        var bestLeave = -1f
        var bestPath: List<PointF> = emptyList()

        for (spin in SPIN_GRID) {
            val pathV = SpinPhysicsCalculator.calculatePath(spin, cueV, targetV, shotAngle, table)
            if (pathV.isEmpty()) continue
            val resting = pathV.last()
            val scratched = pathV.any { pv -> input.pockets.any { hypot(pv.x - it.x, pv.y - it.y) < scratchRadius } }
            val leave = bestNextPot(PointF(resting.x, resting.y), shot.targetPos, input, r)
            val score = if (scratched) leave * SCRATCH_PENALTY else leave
            if (score > bestLeave) {
                bestLeave = score
                bestSpin = PointF(spin.x, spin.y)
                bestPath = pathV.map { PointF(it.x, it.y) }
            }
        }
        return shot.copy(
            spin = bestSpin,
            positionScore = bestLeave.coerceIn(0f, 1f),
            cueLeavePath = bestPath,
        )
    }

    /** Best make-probability of any direct pot on the remaining group from a hypothetical cue position. */
    private fun bestNextPot(cue: PointF, justPotted: PointF, input: AdvisorInput, r: Float): Float {
        var best = 0f
        for (target in input.targetBalls) {
            if (isSamePoint(target, justPotted)) continue
            for (pocket in input.pockets) {
                val c = evaluateDirect(cue, target, pocket, input, r) ?: continue
                if (c.makeProbability > best) best = c.makeProbability
            }
        }
        return best
    }

    private fun evaluateDirect(
        cue: PointF, target: PointF, pocket: PointF, input: AdvisorInput, r: Float,
    ): RecommendedShot? {
        // Object ball must travel target → pocket.
        val toPocketX = pocket.x - target.x
        val toPocketY = pocket.y - target.y
        val toPocketLen = hypot(toPocketX, toPocketY)
        if (toPocketLen < 1e-3f) return null
        val objDirX = toPocketX / toPocketLen
        val objDirY = toPocketY / toPocketLen

        // Ghost-ball center: one ball-diameter behind the target, away from the pocket.
        val ghost = PointF(target.x - objDirX * 2f * r, target.y - objDirY * 2f * r)

        // Cue must travel cue → ghost.
        val toGhostX = ghost.x - cue.x
        val toGhostY = ghost.y - cue.y
        val toGhostLen = hypot(toGhostX, toGhostY)
        if (toGhostLen < 1e-3f) return null
        val aimDirX = toGhostX / toGhostLen
        val aimDirY = toGhostY / toGhostLen

        // Cut angle = angle between the cue's incoming line and the object's outgoing line.
        val dot = (aimDirX * objDirX + aimDirY * objDirY).coerceIn(-1f, 1f)
        val cutRad = acos(dot)
        if (cutRad >= MAX_CUT_RAD) return null  // ≳83°: contact geometrically impossible/absurd

        // Both travel paths must be clear of other balls.
        if (isBlocked(cue, ghost, input.allBalls, listOf(cue, target), r)) return null
        if (isBlocked(target, pocket, input.allBalls, listOf(target), r)) return null

        // Score: cut angle dominates (cos², 0° → 1, 90° → 0), distance is a secondary penalty.
        val cutFactor = cos(cutRad).coerceAtLeast(0f).let { it * it }
        val distTotal = toGhostLen + toPocketLen
        val distFactor = (1f - distTotal / input.tableDiagonal).coerceIn(0.2f, 1f)
        val make = cutFactor * distFactor

        return RecommendedShot(
            type = ShotType.DIRECT,
            targetPos = target,
            pocketPos = pocket,
            ghostCuePos = ghost,
            cutAngleDeg = Math.toDegrees(cutRad.toDouble()).toFloat(),
            hardness = hardnessFor(distTotal, input.tableDiagonal),
            spin = PointF(0f, 0f), // Phase 2 computes position-aware spin
            makeProbability = make,
            confidence = make,
            shotPath = listOf(PointF(cue.x, cue.y), ghost, PointF(target.x, target.y), PointF(pocket.x, pocket.y)),
        )
    }

    /** True if any ball (excluding the shot's own endpoints) lies within ~a ball-diameter of the segment. */
    private fun isBlocked(a: PointF, b: PointF, balls: List<PointF>, exclude: List<PointF>, r: Float): Boolean {
        val clearance = 2f * r * 0.9f // slightly under a diameter so near-grazes aren't over-rejected
        return balls.any { ball ->
            if (exclude.any { isSamePoint(it, ball) }) false
            else distanceSegmentToPoint(a, b, ball) < clearance
        }
    }

    private fun distanceSegmentToPoint(a: PointF, b: PointF, p: PointF): Float {
        val abx = b.x - a.x
        val aby = b.y - a.y
        val l2 = abx * abx + aby * aby
        if (l2 < 1e-6f) return hypot(p.x - a.x, p.y - a.y)
        var t = ((p.x - a.x) * abx + (p.y - a.y) * aby) / l2
        t = t.coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * abx), p.y - (a.y + t * aby))
    }

    private fun isSamePoint(a: PointF, b: PointF): Boolean = hypot(a.x - b.x, a.y - b.y) < 1f

    private fun hardnessFor(distTotal: Float, diagonal: Float): Hardness {
        val f = distTotal / diagonal
        return when {
            f < 0.35f -> Hardness.SOFT
            f < 0.7f -> Hardness.MEDIUM
            f < 1.1f -> Hardness.FIRM
            else -> Hardness.BREAK
        }
    }

    companion object {
        // ~83°; beyond this a cut is effectively unmakeable.
        private val MAX_CUT_RAD = Math.toRadians(83.0).toFloat()
        private const val MIN_MAKE_PROBABILITY = 0.08f

        // Position-aware (Phase 2) tuning.
        private const val POSITION_TOP_K = 5      // refine spin/leave for the strongest pots only
        private const val POSITION_WEIGHT = 0.6f  // how much the leave influences ranking
        private const val SCRATCH_PENALTY = 0.1f  // multiplier when the cue scratches
        private val SPIN_GRID = listOf(
            Vector2(0f, 0f),    // stun
            Vector2(0f, 0.8f),  // follow
            Vector2(0f, -0.8f), // draw
            Vector2(0.7f, 0f),  // right english
            Vector2(-0.7f, 0f), // left english
            Vector2(0.5f, 0.6f), Vector2(-0.5f, 0.6f),
            Vector2(0.5f, -0.6f), Vector2(-0.5f, -0.6f),
        )
    }
}
