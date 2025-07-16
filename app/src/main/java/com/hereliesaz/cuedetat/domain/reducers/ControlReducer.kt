package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class ControlReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.TableRotationChanged -> state.copy(table = state.table.copy(rotationDegrees = event.degrees))
            is MainScreenEvent.ZoomChanged -> state.copy(zoomSliderPosition = event.position)
            is MainScreenEvent.UpdateHoughP1 -> state.copy(houghP1 = event.value)
            is MainScreenEvent.UpdateHoughP2 -> state.copy(houghP2 = event.value)
            is MainScreenEvent.UpdateCannyT1 -> state.copy(cannyThreshold1 = event.value)
            is MainScreenEvent.UpdateCannyT2 -> state.copy(cannyThreshold2 = event.value)
            is MainScreenEvent.AdjustLuminance -> state.copy(luminanceAdjustment = event.value)
            is MainScreenEvent.AdjustGlow -> state.copy(glowStickValue = event.value)
            else -> state
        }
    }
}