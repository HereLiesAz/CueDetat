package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * Keeps every ball wholly on the table.
 *
 * Ball positions are written from many places: three drag modes and the gesture-start snap
 * (GestureReducer), tap-to-place (CvReducer), the follow of detected balls (SnapReducer), undo and
 * reset (ActionReducer). None of them checked the table, so a ball could be dragged off it, or
 * follow a detection that was. This is the one place that does, applied where state leaves
 * `stateReducer` and where `SnapReducer.reduce` returns, so a new writer cannot forget.
 *
 * - **Target, cue and obstacle balls** that would end up over a rail slide along it to the nearest
 *   point where they lie wholly on the table ([com.hereliesaz.cuedetat.view.model.Table.clampBallCenter]);
 *   they are never reset or removed.
 * - **The ghost cue ball** is not a stored position: it is the target ball's centre plus
 *   `2 * radius` at the aim angle ([com.hereliesaz.cuedetat.view.model.ProtractorUnit.ghostCueBallCenter]).
 *   So it is kept on the table by moving the aim, to the nearest angle at which the ghost fits
 *   ([aimWithGhostOnTable]). Dragging the aim into a rail, or the target towards one, makes the
 *   ghost slide along the rail; it never leaves the table. Only where the ghost is the protractor's:
 *   not in banking mode (no ghost) and not in massé mode (the ghost is the massé simulation's).
 *
 * `isGeometricallyImpossible` does not cover any of this: it is about the shot line, ghost and
 * target (see `UpdateStateUseCase`), and never looks at the table.
 *
 * With no visible table (infinite space) there are no bounds and nothing changes.
 *
 * Pure; allocates only when something actually has to move, since it runs on every reduction,
 * including 30-60 Hz sensor ticks.
 */
object BallBounds {

    /** Slack, in logical units, for a ghost that sits exactly on its limit after floating-point work. */
    private const val TOLERANCE = 1e-3

    /**
     * [state] with the target, cue and obstacle balls confined to the table, and the aim moved if
     * that is what it takes to keep the ghost cue ball on it.
     */
    fun confine(state: CueDetatState): CueDetatState {
        val table = state.table
        if (!table.isVisible) return state

        val unit = state.protractorUnit
        val unitCenter = table.clampBallCenter(unit.center, unit.radius)
        val unitMoved = differs(unitCenter, unit.center)

        // The ghost is judged from where the target ends up, so a target moved in is not judged from
        // where it was.
        val aimRotation = if (!state.isBankingMode && !state.isMasseModeActive) {
            val limits = table.ballCenterLimits(unit.radius)
            aimWithGhostOnTable(
                unitCenter.x, unitCenter.y, unit.radius, limits.x, limits.y, unit.rotationDegrees,
            )
        } else {
            unit.rotationDegrees
        }
        val aimMoved = aimRotation != unit.rotationDegrees

        val cue = state.onPlaneBall
        val cueCenter = cue?.let { table.clampBallCenter(it.center, it.radius) }
        val cueMoved = cue != null && cueCenter != null && differs(cueCenter, cue.center)

        var obstaclesMoved = false
        val obstacles = state.obstacleBalls.map { ball ->
            val center = table.clampBallCenter(ball.center, ball.radius)
            if (differs(center, ball.center)) {
                obstaclesMoved = true
                ball.copy(center = center)
            } else {
                ball
            }
        }

        if (!unitMoved && !aimMoved && !cueMoved && !obstaclesMoved) return state
        return state.copy(
            protractorUnit = if (unitMoved || aimMoved) {
                unit.copy(
                    center = if (unitMoved) unitCenter else unit.center,
                    rotationDegrees = aimRotation,
                )
            } else {
                unit
            },
            onPlaneBall = if (cueMoved) cue!!.copy(center = cueCenter!!) else cue,
            obstacleBalls = if (obstaclesMoved) obstacles else state.obstacleBalls,
        )
    }

    /**
     * The aim angle (degrees) nearest to [rotationDegrees] at which the ghost cue ball, a ball of
     * [radius] `2 * radius` from the target ball at ([targetX], [targetY]), lies wholly on the table.
     * [limitX] and [limitY] are how far from the table's centre a ball's centre may be
     * ([com.hereliesaz.cuedetat.view.model.Table.ballCenterLimits]).
     *
     * The ghost is `target + d * (-sin a, cos a)`, so as the angle turns it travels a circle around
     * the target. Where that circle leaves the table it does so across a rail line, so the nearest
     * allowed angle is the current one, if it is allowed, or one of the points where the circle
     * crosses a rail line. The result keeps the caller's winding (it is [rotationDegrees] plus a
     * small turn, not wrapped), because the aim gesture accumulates unwrapped degrees.
     *
     * Returns [rotationDegrees] unchanged when it is already allowed, and also when no angle is (a
     * table smaller than the ghost's circle, which has nothing sensible to offer).
     */
    internal fun aimWithGhostOnTable(
        targetX: Float,
        targetY: Float,
        radius: Float,
        limitX: Float,
        limitY: Float,
        rotationDegrees: Float,
    ): Float {
        val d = 2.0 * radius
        if (d <= 0.0) return rotationDegrees
        val cx = targetX.toDouble()
        val cy = targetY.toDouble()
        val lx = limitX.toDouble()
        val ly = limitY.toDouble()

        // Polar angle of the ghost's offset from the target: (-sin a, cos a) = (cos p, sin p), p = a + 90 deg.
        val current = Math.toRadians(rotationDegrees.toDouble()) + PI / 2.0

        fun onTable(p: Double): Boolean =
            abs(cx + d * cos(p)) <= lx + TOLERANCE && abs(cy + d * sin(p)) <= ly + TOLERANCE

        if (onTable(current)) return rotationDegrees

        var bestTurn: Double? = null
        var bestSize = Double.MAX_VALUE
        fun consider(p: Double) {
            if (!onTable(p)) return
            val turn = wrapToPi(p - current)
            if (abs(turn) < bestSize) {
                bestSize = abs(turn)
                bestTurn = turn
            }
        }
        // Crossings of the vertical rail lines x = -lx and x = lx: cos p = (x - cx) / d.
        for (x in doubleArrayOf(-lx, lx)) {
            val c = (x - cx) / d
            if (abs(c) <= 1.0) {
                val a = acos(c)
                consider(a)
                consider(-a)
            }
        }
        // Crossings of the horizontal rail lines y = -ly and y = ly: sin p = (y - cy) / d.
        for (y in doubleArrayOf(-ly, ly)) {
            val s = (y - cy) / d
            if (abs(s) <= 1.0) {
                val a = asin(s)
                consider(a)
                consider(PI - a)
            }
        }

        val turn = bestTurn ?: return rotationDegrees
        return (rotationDegrees + Math.toDegrees(turn)).toFloat()
    }

    /** [angle] in radians wrapped to (-pi, pi]. */
    private fun wrapToPi(angle: Double): Double {
        var a = angle % (2.0 * PI)
        if (a > PI) a -= 2.0 * PI
        if (a <= -PI) a += 2.0 * PI
        return a
    }

    private fun differs(a: PointF, b: PointF): Boolean = a.x != b.x || a.y != b.y
}
