package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

internal fun reduceAdvancedOptionsAction(
    state: CueDetatState,
    action: MainScreenEvent
): CueDetatState {
    return when (action) {
        is MainScreenEvent.ToggleAdvancedOptionsDialog -> {
            val isOpening = !state.showAdvancedOptionsDialog
            if (!isOpening) {
                state.copy(
                    showAdvancedOptionsDialog = false,
                    showCvMask = false,
                    isTestingCvMask = false,
                    isCalibratingColor = false
                )
            } else {
                state.copy(showAdvancedOptionsDialog = true)
            }
        }

        is MainScreenEvent.ToggleCvMask -> state.copy(showCvMask = !state.showCvMask)
        is MainScreenEvent.EnterCvMaskTestMode -> state.copy(
            isTestingCvMask = true,
            showCvMask = true,
            showAdvancedOptionsDialog = false
        )

        is MainScreenEvent.ExitCvMaskTestMode -> state.copy(
            isTestingCvMask = false,
            showCvMask = true,
            showAdvancedOptionsDialog = true
        )

        is MainScreenEvent.EnterCalibrationMode -> state.copy(
            isCalibratingColor = true,
            showAdvancedOptionsDialog = false,
            showCvMask = false
        )

        is MainScreenEvent.SampleColorAt -> state.copy(
            colorSamplePoint = action.screenPosition,
            isCalibratingColor = false,
            isTestingCvMask = true,
            showCvMask = true
        )

        is MainScreenEvent.LockOrUnlockColor.ToggleCvRefinementMethod -> state.copy(
            cvRefinementMethod = state.cvRefinementMethod.next()
        )

        is MainScreenEvent.UpdateHoughP1 -> state.copy(houghP1 = action.value)
        is MainScreenEvent.UpdateHoughP2 -> state.copy(houghP2 = action.value)
        is MainScreenEvent.UpdateHoughThreshold -> state.copy(houghThreshold = action.value)
        is MainScreenEvent.UpdateCannyT1 -> state.copy(cannyThreshold1 = action.value)
        is MainScreenEvent.UpdateCannyT2 -> state.copy(cannyThreshold2 = action.value)
        else -> state
    }
}