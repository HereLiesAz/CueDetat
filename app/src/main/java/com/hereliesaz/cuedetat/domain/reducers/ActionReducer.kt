// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ActionReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class ActionReducer @Inject constructor(
    private val reducerUtils: ReducerUtils
) {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.Reset -> handleReset(state)
            is MainScreenEvent.AddObstacle -> handleAddObstacle(state)
            is MainScreenEvent.ClearObstacles -> state.copy(obstacleBalls = emptyList())
            is MainScreenEvent.SetTableSize -> state.copy(table = state.table.withSize(event.size))
            is MainScreenEvent.CycleTableSize -> state.copy(table = state.table.withSize(state.table.size.next()))
            is MainScreenEvent.AimBankShot -> state.copy(bankingAimTarget = event.logicalTarget)
            else -> state
        }
    }

    private fun handleReset(state: OverlayState): OverlayState {
        return state.copy(
            protractorUnit = ProtractorUnit(),
            onPlaneBall = null,
            table = state.table.withRotation(90f),
            obstacleBalls = emptyList()
        )
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