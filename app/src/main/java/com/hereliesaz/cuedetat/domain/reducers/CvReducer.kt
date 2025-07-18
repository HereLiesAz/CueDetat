// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt

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
                    currentState.copy(lockedHsvColor = currentState.visionData.detectedHsvColor)
                } else {
                    currentState.copy(lockedHsvColor = null)
                }
            }
            is MainScreenEvent.LockColor -> {
                currentState.copy(lockedHsvColor = event.hsv)
            }
            is MainScreenEvent.ClearSamplePoint -> {
                currentState.copy(colorSamplePoint = null)
            }
            else -> currentState
        }
    }
}