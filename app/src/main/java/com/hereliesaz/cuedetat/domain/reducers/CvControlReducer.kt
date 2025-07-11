package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CvControlReducer @Inject constructor() {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleCvTuningDialog -> currentState.copy(showCvTuningDialog = !currentState.showCvTuningDialog)
            is MainScreenEvent.UpdateHoughP1 -> currentState.copy(houghP1 = event.value)
            is MainScreenEvent.UpdateHoughP2 -> currentState.copy(houghP2 = event.value)
            is MainScreenEvent.UpdateCannyT1 -> currentState.copy(cannyThreshold1 = event.value)
            is MainScreenEvent.UpdateCannyT2 -> currentState.copy(cannyThreshold2 = event.value)
            else -> currentState
        }
    }
}