package com.hereliesaz.cuedetat.domain

import android.graphics.PointF

/**
 * Keeps every ball wholly on the table.
 *
 * Ball positions are written from many places: three drag modes and the gesture-start snap
 * (GestureReducer), tap-to-place (CvReducer), the follow of detected balls (SnapReducer), undo and
 * reset (ActionReducer). None of them checked the table, so a ball could be dragged off it, or
 * follow a detection that was. This is the one place that does, applied where state leaves
 * `stateReducer` and where `SnapReducer.reduce` returns, so a new writer cannot forget.
 *
 * A ball that would end up over a rail slides along it to the nearest point where it lies wholly on
 * the table ([com.hereliesaz.cuedetat.view.model.Table.clampBallCenter]); it is never reset or
 * removed. With no visible table (infinite space) there are no bounds and nothing changes.
 *
 * The ghost cue ball is not a stored position: it is derived from the target ball and the aim
 * ([com.hereliesaz.cuedetat.view.model.ProtractorUnit.ghostCueBallCenter]). A ghost ball over a
 * rail is an impossible shot, which is `isGeometricallyImpossible`'s to report, not a ball to move.
 *
 * Pure; allocates only when a ball actually has to move, since it runs on every reduction,
 * including 30-60 Hz sensor ticks.
 */
object BallBounds {

    /** [state] with the target ball, the cue ball and every obstacle ball confined to the table. */
    fun confine(state: CueDetatState): CueDetatState {
        val table = state.table
        if (!table.isVisible) return state

        val unit = state.protractorUnit
        val unitCenter = table.clampBallCenter(unit.center, unit.radius)
        val unitMoved = differs(unitCenter, unit.center)

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

        if (!unitMoved && !cueMoved && !obstaclesMoved) return state
        return state.copy(
            protractorUnit = if (unitMoved) unit.copy(center = unitCenter) else unit,
            onPlaneBall = if (cueMoved) cue!!.copy(center = cueCenter!!) else cue,
            obstacleBalls = if (obstaclesMoved) obstacles else state.obstacleBalls,
        )
    }

    private fun differs(a: PointF, b: PointF): Boolean = a.x != b.x || a.y != b.y
}
