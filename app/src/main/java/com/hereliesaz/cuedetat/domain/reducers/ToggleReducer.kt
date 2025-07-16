package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class ToggleReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleCamera -> state.copy(isCameraVisible = !state.isCameraVisible)
            is MainScreenEvent.ToggleTable -> state.copy(table = state.table.copy(isVisible = !state.table.isVisible))
            is MainScreenEvent.ToggleOnPlaneBall -> state.copy(onPlaneBall = if (state.onPlaneBall == null) state.onPlaneBall else null)
            is MainScreenEvent.ToggleBankingMode -> state.copy(isBankingMode = !state.isBankingMode)
            is MainScreenEvent.ToggleHelp -> state.copy(isTutorialVisible = !state.isTutorialVisible)
            is MainScreenEvent.ToggleCvParamMenu -> state.copy(isCvParamMenuVisible = !state.isCvParamMenuVisible)
            is MainScreenEvent.ToggleForceTheme -> state.copy(
                isForceLightMode = when (state.isForceLightMode) {
                    true -> false
                    false -> null
                    null -> true
                }
            )
            is MainScreenEvent.ToggleDistanceUnit -> state.copy(distanceUnit = state.distanceUnit.next())
            else -> state
        }
    }
}