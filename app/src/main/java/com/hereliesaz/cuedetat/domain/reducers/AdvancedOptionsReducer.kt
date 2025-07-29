package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState

internal fun reduceAdvancedOptionsAction(
    state: CueDetatState,
    action: CueDetatAction
): CueDetatState {
    return when (action) {
        is CueDetatAction.ToggleAdvancedOptionsDialog -> {
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

        is CueDetatAction.ToggleCvMask -> state.copy(showCvMask = !state.showCvMask)
        is CueDetatAction.EnterCvMaskTestMode -> state.copy(
            isTestingCvMask = true,
            showCvMask = true,
            showAdvancedOptionsDialog = false
        )

        is CueDetatAction.ExitCvMaskTestMode -> state.copy(
            isTestingCvMask = false,
            showCvMask = true,
            showAdvancedOptionsDialog = true
        )

        is CueDetatAction.EnterCalibrationMode -> state.copy(
            isCalibratingColor = true,
            showAdvancedOptionsDialog = false,
            showCvMask = false
        )

        is CueDetatAction.SampleColorAt -> state.copy(
            colorSamplePoint = action.screenPosition,
            isCalibratingColor = false,
            isTestingCvMask = true,
            showCvMask = true
        )

        is CueDetatAction.ToggleCvRefinementMethod -> state.copy(cvRefinementMethod = state.cvRefinementMethod.next())
        is CueDetatAction.UpdateHoughP1 -> state.copy(houghP1 = action.value)
        is CueDetatAction.UpdateHoughP2 -> state.copy(houghP2 = action.value)
        is CueDetatAction.UpdateCannyT1 -> state.copy(cannyThreshold1 = action.value)
        is CueDetatAction.UpdateCannyT2 -> state.copy(cannyThreshold2 = action.value)
        else -> state
    }
}