package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import kotlin.math.hypot
import kotlin.random.Random

internal fun reduceObstacleAction(
    state: CueDetatState,
    action: CueDetatAction,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when (action) {
        is CueDetatAction.AddObstacleBall -> handleAddObstacleBall(state, reducerUtils)
        else -> state
    }
}

private fun handleAddObstacleBall(state: CueDetatState, reducerUtils: ReducerUtils): CueDetatState {
    val newBallRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius
    val placement = findNextAvailablePlacement(state, reducerUtils)
    val newBall = OnPlaneBall(center = placement, radius = newBallRadius)
    val updatedList = state.obstacleBalls + newBall
    return state.copy(obstacleBalls = updatedList, valuesChangedSinceReset = true)
}

private fun findNextAvailablePlacement(state: CueDetatState, reducerUtils: ReducerUtils): PointF {
    val visionData = state.visionData ?: return reducerUtils.getDefaultTargetBallPosition()
    val detectedBalls = visionData.genericBalls + visionData.customBalls
    val allLogicalBalls = (listOfNotNull(
        state.onPlaneBall,
        state.protractorUnit
    ) + state.obstacleBalls).map { it.center }

    val nextAvailableBall = detectedBalls.firstOrNull { detected ->
        allLogicalBalls.none { logical ->
            hypot(
                (logical.x - detected.x).toDouble(),
                (logical.y - detected.y).toDouble()
            ) < (state.protractorUnit.radius * 2)
        }
    }

    if (nextAvailableBall != null) {
        return nextAvailableBall
    }

    if (state.table.isVisible) {
        val corners = state.table.corners
        if (corners.isEmpty()) return reducerUtils.getDefaultTargetBallPosition()
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }
        for (i in 0 until 10) {
            val randomX = Random.nextFloat() * (maxX - minX) + minX
            val randomY = Random.nextFloat() * (maxY - minY) + minY
            val candidatePoint = PointF(randomX, randomY)
            if (state.table.isPointInside(candidatePoint)) {
                val isOverlapping = allLogicalBalls.any {
                    hypot(
                        (it.x - candidatePoint.x).toDouble(),
                        (it.y - candidatePoint.y).toDouble()
                    ) < (state.protractorUnit.radius * 2.5)
                }
                if (!isOverlapping) return candidatePoint
            }
        }
        return reducerUtils.getDefaultTargetBallPosition()
    } else {
        val numObstacles = state.obstacleBalls.size
        val quadrantIndex = numObstacles % 4
        val radius = state.protractorUnit.radius
        val xOffset = if (quadrantIndex % 2 == 0) -radius * 3 else radius * 3
        val yOffset = if (quadrantIndex < 2) -radius * 6 else radius * 6
        return PointF(xOffset, yOffset)
    }
}