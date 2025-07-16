package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.DistanceUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class ToggleReducer @Inject constructor() {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleTable -> state.copy(table = state.table.withVisibility(!state.table.isVisible))
            is MainScreenEvent.ToggleOnPlaneBall -> if (state.onPlaneBall != null) state.copy(onPlaneBall = null) else state.copy(onPlaneBall = state.protractorUnit.asOnPlaneBall())
            is MainScreenEvent.ToggleSpinControl -> state.copy(isSpinControlVisible = !state.isSpinControlVisible)
            is MainScreenEvent.ToggleCamera -> state.copy(isCameraVisible = !state.isCameraVisible)
            is MainScreenEvent.ToggleHelp -> state.copy(areHelpersVisible = !state.areHelpersVisible)
            is MainScreenEvent.ToggleLuminanceDialog -> state.copy(luminanceAdjustment = if (state.luminanceAdjustment != 0f) 0f else 0.5f)
            is MainScreenEvent.ToggleGlowStickDialog -> state.copy(glowStickValue = if (state.glowStickValue != 0f) 0f else 0.5f)
            is MainScreenEvent.ToggleTableSizeDialog -> state // Handled in UI layer
            is MainScreenEvent.ToggleAdvancedOptionsDialog -> state.copy(isCvParamMenuVisible = !state.isCvParamMenuVisible)
            is MainScreenEvent.ToggleForceTheme -> state.copy(isForceLightMode = when(state.isForceLightMode){
                null -> true
                true -> false
                false -> null
            })
            is MainScreenEvent.ToggleDistanceUnit -> state.copy(distanceUnit = if(state.distanceUnit == DistanceUnit.IMPERIAL) DistanceUnit.METRIC else DistanceUnit.IMPERIAL)
            is MainScreenEvent.ToggleBankingMode -> state.copy(isBankingMode = !state.isBankingMode)
            else -> state
        }
    }
}