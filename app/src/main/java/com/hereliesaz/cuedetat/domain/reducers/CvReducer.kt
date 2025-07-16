package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CvReducer @Inject constructor() {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.CvDataUpdated -> {
                currentState.copy(visionData = event.data)
            }
            is MainScreenEvent.LockOrUnlockColor -> {
                if (currentState.lockedHsvColor == null) {
                    // Lock the current auto-detected color
                    currentState.copy(lockedHsvColor = currentState.visionData.detectedHsvColor)
                } else {
                    // Unlock and revert to auto-detection
                    currentState.copy(lockedHsvColor = null)
                }
            }
            else -> currentState
        }
    }
}