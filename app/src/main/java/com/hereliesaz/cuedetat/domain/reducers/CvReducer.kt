package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class CvReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when(event) {
            is MainScreenEvent.CvDataUpdated -> {
                state.copy(
                    visionData = event.visionData,
                    lockedHsvColor = if(state.lockedHsvColor == null) event.visionData.detectedHsvColor else state.lockedHsvColor
                )
            }
            is MainScreenEvent.LockOrUnlockColor -> state.copy(
                lockedHsvColor = if (state.lockedHsvColor == null) state.visionData.detectedHsvColor else null
            )
            else -> state
        }
    }
}