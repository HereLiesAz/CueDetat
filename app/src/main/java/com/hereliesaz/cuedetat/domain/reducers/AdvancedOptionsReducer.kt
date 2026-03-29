package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Reducer responsible for the 'Felt Sacrifice' and CV tuning.
 *
 * This governs the advanced configuration of the computer vision engine, allowing
 * the user to manually refine thresholds when the lighting conditions are
 * ontologically challenging.
 *
 * @param state The current application state.
 * @param action The specific CV tuning event.
 * @return A new state reflecting the adjusted vision parameters.
 */
internal fun reduceAdvancedOptionsAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        // Toggle the visibility of the deep configuration dialog.
        is MainScreenEvent.ToggleAdvancedOptionsDialog -> {
            state.copy(showAdvancedOptionsDialog = !state.showAdvancedOptionsDialog)
        }

        // Enable/Disable the CV mask overlay (seeing the world as the AI does).
        is MainScreenEvent.ToggleCvMask -> {
            state.copy(showCvMask = !state.showCvMask)
        }

        // Entering the void where only contours and thresholded pixels exist.
        is MainScreenEvent.EnterCvMaskTestMode -> {
            state.copy(isTestingCvMask = true)
        }

        is MainScreenEvent.ExitCvMaskTestMode -> {
            state.copy(isTestingCvMask = false)
        }

        // The 'Felt Sacrifice' phase: defining the color of the table.
        is MainScreenEvent.EnterCalibrationMode -> {
            state.copy(isCalibratingColor = true)
        }

        is MainScreenEvent.SampleColorAt -> {
            state.copy(
                colorSamplePoint = action.screenPosition,
                // Exit calibration mode once the sacrifice is made.
                isCalibratingColor = false
            )
        }



        is MainScreenEvent.UpdateCannyT1 -> {
            state.copy(cannyThreshold1 = action.value)
        }

        is MainScreenEvent.UpdateCannyT2 -> {
            state.copy(cannyThreshold2 = action.value)
        }

        // Cycle through refinement algorithms (Hough vs Contour).
        is MainScreenEvent.ToggleCvRefinementMethod -> {
            state.copy(cvRefinementMethod = state.cvRefinementMethod.next())
        }

        else -> state
    }
}