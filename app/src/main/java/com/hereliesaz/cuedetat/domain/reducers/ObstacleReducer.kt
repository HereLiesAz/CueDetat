// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ObstacleReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class ObstacleReducer @Inject constructor(
    private val reducerUtils: ReducerUtils
) {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.AddObstacle -> handleAddObstacle(state)
            is MainScreenEvent.ClearObstacles -> state.copy(obstacleBalls = emptyList())
            else -> state
        }
    }

    private fun handleAddObstacle(state: OverlayState): OverlayState {
        if (!state.table.isVisible) return state

        val newObstacle = state.protractorUnit.asOnPlaneBall().copy(
            center = reducerUtils.getDefaultTargetBallPosition()
        )
        val newObstacles = state.obstacleBalls + newObstacle
        return state.copy(obstacleBalls = newObstacles)
    }
}