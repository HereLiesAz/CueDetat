package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot

@Singleton
class ObstacleReducer @Inject constructor() {

    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.AddObstacleBall -> handleAddObstacleBall(currentState)
            else -> currentState
        }
    }

    private fun handleAddObstacleBall(currentState: OverlayState): OverlayState {
        val newBallRadius = currentState.onPlaneBall?.radius ?: currentState.protractorUnit.radius

        val placement = findNextAvailablePlacement(currentState)

        val newBall = OnPlaneBall(
            center = placement,
            radius = newBallRadius
        )
        val updatedList = currentState.obstacleBalls + newBall
        return currentState.copy(obstacleBalls = updatedList, valuesChangedSinceReset = true)
    }

    private fun findNextAvailablePlacement(currentState: OverlayState): PointF {
        val detectedBalls = currentState.visionData.genericBalls + currentState.visionData.customBalls
        val allLogicalBalls = (listOfNotNull(
            currentState.onPlaneBall,
            currentState.protractorUnit
        ) + currentState.obstacleBalls).map { it.center }

        // Find the first detected ball that doesn't already have a logical ball snapped to it.
        val nextAvailableBall = detectedBalls.firstOrNull { detected ->
            allLogicalBalls.none { logical ->
                hypot((logical.x - detected.x).toDouble(), (logical.y - detected.y).toDouble()) < (currentState.protractorUnit.radius * 2)
            }
        }

        // If an available detected ball is found, place the obstacle there.
        if (nextAvailableBall != null) {
            return nextAvailableBall
        }

        // Fallback to the old quadrant-based placement if no free detected balls are available.
        val numObstacles = currentState.obstacleBalls.size
        val quadrantIndex = numObstacles % 4

        val xFactor = if (quadrantIndex % 2 == 0) 0.3f else 0.7f
        val yFactor = if (quadrantIndex < 2) 0.35f else 0.65f

        return PointF(
            currentState.viewWidth * xFactor,
            currentState.viewHeight * yFactor
        )
    }
}