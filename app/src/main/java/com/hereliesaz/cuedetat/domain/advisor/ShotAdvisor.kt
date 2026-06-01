package com.hereliesaz.cuedetat.domain.advisor

import android.graphics.PointF
import javax.inject.Inject
import kotlin.math.acos
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

        var best: RecommendedShot? = null
        for (target in input.targetBalls) {
            for (pocket in input.pockets) {
                val candidate = evaluateDirect(input.cue, target, pocket, input, r) ?: continue
                if (best == null || candidate.makeProbability > best!!.makeProbability) {
                    best = candidate
                }
            }
        }
        return best?.takeIf { it.makeProbability >= MIN_MAKE_PROBABILITY }
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
    }
}
