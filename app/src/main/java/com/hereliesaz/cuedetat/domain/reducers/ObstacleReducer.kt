package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

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

        val safePlacement = findSafePlacement(currentState)

        val newBall = OnPlaneBall(
            center = safePlacement,
            radius = newBallRadius
        )
        val updatedList = currentState.obstacleBalls + newBall
        return currentState.copy(obstacleBalls = updatedList, valuesChangedSinceReset = true)
    }

    /**
     * Finds a relatively safe and predictable place to spawn a new obstacle ball.
     * It cycles through four screen quadrants to avoid immediate overlap.
     */
    private fun findSafePlacement(currentState: OverlayState): PointF {
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