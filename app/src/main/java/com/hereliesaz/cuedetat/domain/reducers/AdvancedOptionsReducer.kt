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
            // Closing the dialog also stops a running auto-tune: the loop must not keep
            // lowering thresholds behind a dialog the user has dismissed.
            state.copy(
                showAdvancedOptionsDialog = !state.showAdvancedOptionsDialog,
                isAutoCalibrating = if (state.showAdvancedOptionsDialog) false else state.isAutoCalibrating
            )
        }

        // Enable/Disable the CV mask overlay (seeing the world as the AI does).
        is MainScreenEvent.ToggleCvMask -> {
            state.copy(showCvMask = !state.showCvMask)
        }

        is MainScreenEvent.UpdateCannyT1 -> {
            state.copy(cannyThreshold1 = action.value)
        }

        is MainScreenEvent.UpdateCannyT2 -> {
            state.copy(cannyThreshold2 = action.value)
        }

        // Cycle through refinement algorithms (Hough vs Contour).

        else -> state
    }
}