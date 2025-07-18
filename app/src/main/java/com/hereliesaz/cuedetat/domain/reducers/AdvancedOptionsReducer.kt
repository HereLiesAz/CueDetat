package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvancedOptionsReducer @Inject constructor() {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.ToggleAdvancedOptionsDialog -> {
                val isOpening = !currentState.showAdvancedOptionsDialog
                if (!isOpening) {
                    currentState.copy(
                        showAdvancedOptionsDialog = false,
                        showCvMask = false,
                        isTestingCvMask = false,
                        isCalibratingColor = false
                    )
                } else {
                    currentState.copy(showAdvancedOptionsDialog = true)
                }
            }
            is MainScreenEvent.ToggleCvMask -> currentState.copy(showCvMask = !currentState.showCvMask)
            is MainScreenEvent.EnterCvMaskTestMode -> currentState.copy(
                isTestingCvMask = true,
                showCvMask = true,
                showAdvancedOptionsDialog = false
            )
            is MainScreenEvent.ExitCvMaskTestMode -> currentState.copy(
                isTestingCvMask = false,
                showCvMask = true, // Keep mask on, but return to dialog
                showAdvancedOptionsDialog = true
            )
            is MainScreenEvent.EnterCalibrationMode -> currentState.copy(
                isCalibratingColor = true,
                showAdvancedOptionsDialog = false,
                showCvMask = false
            )
            is MainScreenEvent.SampleColorAt -> currentState.copy(
                colorSamplePoint = event.screenPosition,
                isCalibratingColor = false,
                isTestingCvMask = true, // Immediately go into test mode after sampling
                showCvMask = true
            )
            is MainScreenEvent.ToggleCvRefinementMethod -> currentState.copy(cvRefinementMethod = currentState.cvRefinementMethod.next())
            is MainScreenEvent.UpdateHoughP1 -> currentState.copy(houghP1 = event.value)
            is MainScreenEvent.UpdateHoughP2 -> currentState.copy(houghP2 = event.value)
            is MainScreenEvent.UpdateCannyT1 -> currentState.copy(cannyThreshold1 = event.value)
            is MainScreenEvent.UpdateCannyT2 -> currentState.copy(cannyThreshold2 = event.value)
            is MainScreenEvent.UpdateHsvMultiplier -> currentState.copy(cvHsvRangeMultiplier = event.value)
            else -> currentState
        }
    }
}