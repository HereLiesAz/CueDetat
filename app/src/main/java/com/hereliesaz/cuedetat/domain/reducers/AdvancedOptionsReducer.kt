package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class AdvancedOptionsReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleAdvancedOptions -> state.copy(isCvParamMenuVisible = !state.isCvParamMenuVisible)
            is MainScreenEvent.ToggleSnapping -> state.copy(isSnappingEnabled = !state.isSnappingEnabled)
            is MainScreenEvent.ToggleCvModel -> state.copy(cvModel = state.cvModel.next())
            is MainScreenEvent.ToggleCvRefinementMethod -> state.copy(cvRefinement = state.cvRefinement.next())
            else -> state
        }
    }
}