package com.hereliesaz.cuedetat.domain.advisor

import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.TargetType
import kotlin.math.hypot

/**
 * Bridge from app state to the pure [AdvisorInput]. Returns null (no recommendation possible)
 * unless the table pose is calibrated — so detected balls are in logical coordinates
 * (`hasInverseMatrix`) — a cue ball is locatable, and the player's group has at least one ball.
 * This is the confidence gate: when CV detection is too thin, the advisor stays silent.
 */
fun CueDetatState.toAdvisorInput(): AdvisorInput? {
    if (!hasInverseMatrix) return null
    val detected = visionData?.balls ?: emptyList()

    val cue = detected.firstOrNull { it.type == BallType.CUE }?.position
        ?: onPlaneBall?.center
        ?: return null

    val groupType = if (targetType == TargetType.STRIPES) BallType.STRIPE else BallType.SOLID
    var targets = detected.filter { it.type == groupType }.map { it.position }
    if (targets.isEmpty()) {
        // Group cleared (or unclassified): fall back to the 8-ball if it's on the table.
        targets = detected.filter { it.type == BallType.EIGHT }.map { it.position }
    }
    if (targets.isEmpty()) return null

    val pockets = tableScanModel?.pockets?.map { it.logicalPosition } ?: table.pockets
    if (pockets.isEmpty()) return null

    return AdvisorInput(
        cue = cue,
        targetBalls = targets,
        allBalls = detected.map { it.position },
        pockets = pockets,
        ballRadius = LOGICAL_BALL_RADIUS,
        tableDiagonal = hypot(table.logicalWidth, table.logicalHeight).coerceAtLeast(1f),
        table = table,
    )
}
