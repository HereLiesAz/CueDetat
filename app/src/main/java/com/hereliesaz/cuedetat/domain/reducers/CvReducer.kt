package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class CvReducer @Inject constructor(
    private val snapReducer: SnapReducer
) {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.CvDataUpdated -> {
                val stateWithVision = state.copy(visionData = event.visionData)
                snapReducer.reduce(stateWithVision, event.visionData)
            }
            is MainScreenEvent.LockOrUnlockColor -> {
                if (state.lockedHsvColor == null) {
                    state.copy(lockedHsvColor = state.visionData.detectedHsvColor)
                } else {
                    state.copy(lockedHsvColor = null)
                }
            }
            else -> state
        }
    }
}
