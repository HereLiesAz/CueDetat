package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.data.DetectedBall
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import kotlin.math.hypot

/**
 * Where a tapped ball goes (the cue / target pick, [BallSelectionPhase]).
 *
 * The tap lands on the table plane at [resolve]'s `tapped` point. If a detected ball is within
 * [SNAP_RADIUS] of it, the virtual ball takes that ball's position (and the caller anchors it so
 * it follows that detection); otherwise it goes exactly where the finger landed. Never a far-away
 * detection: a tap on bare felt must not teleport the ball across the table.
 */
object BallPlacement {

    /** Logical units: three ball radii. A finger is wider than a ball, but not by more. */
    const val SNAP_RADIUS = LOGICAL_BALL_RADIUS * 3f

    /** [snapped] is true when [position] is a detected ball's. */
    data class Result(val position: PointF, val snapped: Boolean)

    /**
     * @param tapped The tap in logical table coordinates.
     * @param phase Which ball is being picked; the target pick skips detections of the other suit.
     * @param candidates Detections that have held still long enough to trust (confirmed only).
     * @param balls The latest raw detections.
     */
    fun resolve(
        tapped: PointF,
        phase: BallSelectionPhase,
        targetType: TargetType,
        candidates: List<SnapCandidate>?,
        balls: List<DetectedBall>?,
    ): Result {
        fun matches(type: BallType): Boolean = when (phase) {
            BallSelectionPhase.AWAITING_TARGET -> type == BallType.UNKNOWN ||
                (targetType == TargetType.STRIPES && type == BallType.STRIPE) ||
                (targetType == TargetType.SOLIDS && type == BallType.SOLID)
            else -> true
        }
        val points = buildList {
            candidates?.filter { it.isConfirmed && matches(it.ballType) }?.forEach { add(it.detectedPoint) }
            balls?.filter { matches(it.type) }?.forEach { add(it.position) }
        }
        val nearest = points.minByOrNull { hypot((it.x - tapped.x).toDouble(), (it.y - tapped.y).toDouble()) }
        val dist = nearest?.let { hypot((it.x - tapped.x).toDouble(), (it.y - tapped.y).toDouble()) }
        return if (nearest != null && dist != null && dist <= SNAP_RADIUS) Result(nearest, true)
        else Result(tapped, false)
    }
}
