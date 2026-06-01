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

        val table = input.table
            ?: // Phase 1: no table → rank by make-probability only (stun). Null when no pot exists.
            return candidates.maxByOrNull { it.makeProbability }
                ?.takeIf { it.makeProbability >= MIN_MAKE_PROBABILITY }

        // Phase 3: add bank / kick / combo candidates (each carries a lower per-type confidence,
        // so they only win when no good direct pot exists).
        addBankCandidates(candidates, input, table, r)
        addKickCandidates(candidates, input, table, r)
        addComboCandidates(candidates, input, r)

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

        // Phase 4: if the best pot isn't confident enough, recommend a safety instead.
        best?.takeIf { it.makeProbability >= SAFETY_POT_FLOOR }?.let { return it }
        bestSafety(input, table, r)?.let { return it }
        return best?.takeIf { it.makeProbability >= MIN_MAKE_PROBABILITY }
    }

    /** When no pot is confident, leave the cue where the opponent's group has no good shot. */
    private fun bestSafety(input: AdvisorInput, table: Table, r: Float): RecommendedShot? {
        if (input.targetBalls.isEmpty()) return null
        var best: RecommendedShot? = null
        var bestScore = 0f
        for (target in input.targetBalls) {
            val toT = sub(target, input.cue)
            val dToT = len(toT)
            if (dToT < 1e-2f) continue
            val dir = PointF(toT.x / dToT, toT.y / dToT)
            val ghost = back(target, dir, 2f * r) // legal soft contact on our own group ball
            if (isBlocked(input.cue, ghost, input.allBalls, listOf(input.cue, target), r)) continue
            val shotAngle = atan2(dir.y, dir.x)

            var localBest = -1f
            var localSpin = PointF(0f, 0f)
            var localPath: List<PointF> = emptyList()
            for (spin in SPIN_GRID) {
                val pathV = SpinPhysicsCalculator.calculatePath(
                    spin, Vector2(input.cue.x, input.cue.y), Vector2(target.x, target.y),
                    shotAngle, table, velocity = SAFETY_VELOCITY,
                )
                if (pathV.isEmpty()) continue
                val resting = pathV.last()
                val scratched = pathV.any { pv -> input.pockets.any { hypot(pv.x - it.x, pv.y - it.y) < r * 1.8f } }
                val oppBest = opponentBestPot(PointF(resting.x, resting.y), input, r)
                val score = (1f - oppBest) * (if (scratched) SCRATCH_PENALTY else 1f)
                if (score > localBest) {
                    localBest = score
                    localSpin = PointF(spin.x, spin.y)
                    localPath = pathV.map { PointF(it.x, it.y) }
                }
            }
            if (localBest > bestScore) {
                bestScore = localBest
                best = RecommendedShot(
                    type = ShotType.SAFETY, targetPos = target, pocketPos = null, ghostCuePos = ghost,
                    cutAngleDeg = 0f, hardness = Hardness.SOFT, spin = localSpin,
                    makeProbability = 0f, positionScore = localBest.coerceIn(0f, 1f),
                    confidence = localBest.coerceIn(0f, 1f),
                    shotPath = listOf(PointF(input.cue.x, input.cue.y), ghost, PointF(target.x, target.y)),
                    cueLeavePath = localPath,
                )
            }
        }
        return best?.takeIf { bestScore > 0f }
    }

    /** Best make-probability of any direct pot the opponent has from a hypothetical cue position. */
    private fun opponentBestPot(cue: PointF, input: AdvisorInput, r: Float): Float {
        var best = 0f
        for (target in input.opponentBalls) {
            for (pocket in input.pockets) {
                val c = evaluateDirect(cue, target, pocket, input, r) ?: continue
                if (c.makeProbability > best) best = c.makeProbability
            }
        }
        return best
    }

    /** Pick the spin offset whose predicted cue leave best sets up the next shot; attach it. */
    private fun withPositionSpin(shot: RecommendedShot, input: AdvisorInput, table: Table, r: Float): RecommendedShot {
        // The leave simulation models the cue contacting the target directly; for combos the cue
        // strikes a different ball, so skip spin refinement there.
        if (shot.type == ShotType.COMBO) return shot
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

    // --- Phase 3 generators: banks, kicks, combos (mirror method) -----------------------------

    private data class RailLine(val vertical: Boolean, val coord: Float, val lo: Float, val hi: Float)

    private fun railsOf(table: Table): List<RailLine> {
        val hw = table.logicalWidth / 2f
        val hh = table.logicalHeight / 2f
        return listOf(
            RailLine(vertical = true, coord = -hw, lo = -hh, hi = hh),   // left
            RailLine(vertical = true, coord = hw, lo = -hh, hi = hh),    // right
            RailLine(vertical = false, coord = -hh, lo = -hw, hi = hw),  // top
            RailLine(vertical = false, coord = hh, lo = -hw, hi = hw),   // bottom
        )
    }

    private fun reflectAcross(p: PointF, rail: RailLine): PointF =
        if (rail.vertical) PointF(2f * rail.coord - p.x, p.y) else PointF(p.x, 2f * rail.coord - p.y)

    /** Intersection of segment a→b with the rail line, within the segment and the rail's extent; else null. */
    private fun segmentRailHit(a: PointF, b: PointF, rail: RailLine): PointF? {
        if (rail.vertical) {
            val dx = b.x - a.x
            if (kotlin.math.abs(dx) < 1e-4f) return null
            val t = (rail.coord - a.x) / dx
            if (t <= 1e-3f || t >= 1f - 1e-3f) return null
            val y = a.y + t * (b.y - a.y)
            if (y < rail.lo || y > rail.hi) return null
            return PointF(rail.coord, y)
        } else {
            val dy = b.y - a.y
            if (kotlin.math.abs(dy) < 1e-4f) return null
            val t = (rail.coord - a.y) / dy
            if (t <= 1e-3f || t >= 1f - 1e-3f) return null
            val x = a.x + t * (b.x - a.x)
            if (x < rail.lo || x > rail.hi) return null
            return PointF(x, rail.coord)
        }
    }

    private fun sub(a: PointF, b: PointF) = PointF(a.x - b.x, a.y - b.y)
    private fun len(a: PointF) = hypot(a.x, a.y)
    private fun unit(a: PointF): PointF { val l = len(a); return if (l > 1e-4f) PointF(a.x / l, a.y / l) else PointF(0f, 0f) }
    private fun dot(a: PointF, b: PointF) = a.x * b.x + a.y * b.y
    private fun back(from: PointF, dir: PointF, d: Float) = PointF(from.x - dir.x * d, from.y - dir.y * d)

    /**
     * Build a pot where the cue contacts [target] directly at [ghost] and the object then travels
     * through [objectPath] (ending at the pocket; multiple points = a bank). DIRECT and BANK only.
     */
    private fun makePot(
        type: ShotType, cue: PointF, target: PointF, ghost: PointF,
        objectPath: List<PointF>, pocket: PointF, input: AdvisorInput, r: Float, conf: Float,
    ): RecommendedShot? {
        val toGhost = sub(ghost, cue)
        val toGhostLen = len(toGhost)
        if (toGhostLen < 1e-2f) return null
        val aim = PointF(toGhost.x / toGhostLen, toGhost.y / toGhostLen)
        val objDir = unit(sub(objectPath.first(), target))
        val cutRad = acos(dot(aim, objDir).coerceIn(-1f, 1f))
        if (cutRad >= MAX_CUT_RAD) return null

        if (isBlocked(cue, ghost, input.allBalls, listOf(cue, target), r)) return null
        var prev = target
        objectPath.forEach { pt ->
            val exclude = if (prev === target) listOf(target) else emptyList()
            if (isBlocked(prev, pt, input.allBalls, exclude, r)) return null
            prev = pt
        }

        val cutFactor = cos(cutRad).coerceAtLeast(0f).let { it * it }
        var dist = toGhostLen
        prev = target
        objectPath.forEach { dist += len(sub(it, prev)); prev = it }
        val distFactor = (1f - dist / input.tableDiagonal).coerceIn(0.15f, 1f)

        val sp = ArrayList<PointF>(objectPath.size + 3)
        sp.add(cue); sp.add(ghost); sp.add(target); sp.addAll(objectPath)
        return RecommendedShot(
            type = type, targetPos = target, pocketPos = pocket, ghostCuePos = ghost,
            cutAngleDeg = Math.toDegrees(cutRad.toDouble()).toFloat(),
            hardness = hardnessFor(dist, input.tableDiagonal), spin = PointF(0f, 0f),
            makeProbability = cutFactor * distFactor * conf, confidence = cutFactor * distFactor * conf,
            shotPath = sp,
        )
    }

    /** One-rail banks of the object ball into a pocket (mirror the pocket across each rail). */
    private fun addBankCandidates(out: MutableList<RecommendedShot>, input: AdvisorInput, table: Table, r: Float) {
        val rails = railsOf(table)
        for (target in input.targetBalls) for (pocket in input.pockets) for (rail in rails) {
            val railPt = segmentRailHit(target, reflectAcross(pocket, rail), rail) ?: continue
            if (len(sub(railPt, target)) < 1e-2f) continue
            val ghost = back(target, unit(sub(railPt, target)), 2f * r)
            makePot(ShotType.BANK, input.cue, target, ghost, listOf(railPt, pocket), pocket, input, r, BANK_CONF)
                ?.let { out.add(it) }
        }
    }

    /** Kicks: cue banks off a rail to reach the ghost when the direct cue→ghost path is blocked. */
    private fun addKickCandidates(out: MutableList<RecommendedShot>, input: AdvisorInput, table: Table, r: Float) {
        val rails = railsOf(table)
        val cue = input.cue
        for (target in input.targetBalls) for (pocket in input.pockets) {
            if (len(sub(pocket, target)) < 1e-3f) continue
            val objDir = unit(sub(pocket, target))
            val ghost = back(target, objDir, 2f * r)
            if (!isBlocked(cue, ghost, input.allBalls, listOf(cue, target), r)) continue // direct works; no kick needed
            for (rail in rails) {
                val railPt = segmentRailHit(cue, reflectAcross(ghost, rail), rail) ?: continue
                val aim = unit(sub(ghost, railPt))
                val cutRad = acos(dot(aim, objDir).coerceIn(-1f, 1f))
                if (cutRad >= MAX_CUT_RAD) continue
                if (isBlocked(cue, railPt, input.allBalls, listOf(cue), r)) continue
                if (isBlocked(railPt, ghost, input.allBalls, listOf(target), r)) continue
                if (isBlocked(target, pocket, input.allBalls, listOf(target), r)) continue
                val cutFactor = cos(cutRad).coerceAtLeast(0f).let { it * it }
                val dist = len(sub(railPt, cue)) + len(sub(ghost, railPt)) + len(sub(pocket, target))
                val distFactor = (1f - dist / input.tableDiagonal).coerceIn(0.15f, 1f)
                out.add(
                    RecommendedShot(
                        ShotType.KICK, target, pocket, ghost,
                        Math.toDegrees(cutRad.toDouble()).toFloat(),
                        hardnessFor(dist, input.tableDiagonal), PointF(0f, 0f),
                        cutFactor * distFactor * KICK_CONF, confidence = cutFactor * distFactor * KICK_CONF,
                        shotPath = listOf(cue, railPt, ghost, target, pocket),
                    )
                )
            }
        }
    }

    /** Two-ball combos: cue strikes ball A, A drives the group ball B into a pocket. */
    private fun addComboCandidates(out: MutableList<RecommendedShot>, input: AdvisorInput, r: Float) {
        val cue = input.cue
        for (b in input.targetBalls) for (pocket in input.pockets) {
            if (len(sub(pocket, b)) < 1e-3f) continue
            val ghostB = back(b, unit(sub(pocket, b)), 2f * r)
            for (a in input.allBalls) {
                if (isSamePoint(a, b) || isSamePoint(a, cue)) continue
                if (len(sub(ghostB, a)) < 1e-2f) continue
                val ghostA = back(a, unit(sub(ghostB, a)), 2f * r)
                val toGhostA = sub(ghostA, cue)
                if (len(toGhostA) < 1e-2f) continue
                val aim = unit(toGhostA)
                val cutRad = acos(dot(aim, unit(sub(ghostB, a))).coerceIn(-1f, 1f))
                if (cutRad >= MAX_CUT_RAD) continue
                if (isBlocked(cue, ghostA, input.allBalls, listOf(cue, a), r)) continue
                if (isBlocked(a, ghostB, input.allBalls, listOf(a, b), r)) continue
                if (isBlocked(b, pocket, input.allBalls, listOf(b), r)) continue
                val cutFactor = cos(cutRad).coerceAtLeast(0f).let { it * it }
                val dist = len(toGhostA) + len(sub(ghostB, a)) + len(sub(pocket, b))
                val distFactor = (1f - dist / input.tableDiagonal).coerceIn(0.1f, 1f)
                out.add(
                    RecommendedShot(
                        ShotType.COMBO, b, pocket, ghostA,
                        Math.toDegrees(cutRad.toDouble()).toFloat(),
                        hardnessFor(dist, input.tableDiagonal), PointF(0f, 0f),
                        cutFactor * distFactor * COMBO_CONF, confidence = cutFactor * distFactor * COMBO_CONF,
                        shotPath = listOf(cue, ghostA, a, b, pocket),
                    )
                )
            }
        }
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

        // Phase 3 per-type confidence: directs (1.0) win unless geometry strongly favors these.
        private const val BANK_CONF = 0.55f
        private const val KICK_CONF = 0.4f
        private const val COMBO_CONF = 0.4f

        // Phase 4 safety: below this best-pot probability, recommend a defensive shot instead.
        private const val SAFETY_POT_FLOOR = 0.25f
        private const val SAFETY_VELOCITY = 0.5f // soft tap, so the cue stays controllable
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
