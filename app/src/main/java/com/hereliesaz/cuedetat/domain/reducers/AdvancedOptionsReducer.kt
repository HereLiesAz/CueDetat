package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class AdvancedOptionsReducer @Inject constructor() {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleAdvancedOptionsDialog -> state.copy(isCvParamMenuVisible = !state.isCvParamMenuVisible)
            is MainScreenEvent.ToggleCvRefinementMethod -> state.copy(cvRefinement = if(state.cvRefinement == "HOUGH") "CONTOUR" else "HOUGH")
            is MainScreenEvent.ToggleCvModel -> state.copy(cvModel = if(state.cvModel == "Generic") "Custom" else "Generic")
            is MainScreenEvent.ToggleSnapping -> state.copy(isSnappingEnabled = !state.isSnappingEnabled)
            else -> state
        }
    }
}
