package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Reducer function responsible for handling "Advanced Options" and Developer Mode events.
 *
 * This includes toggling the advanced options dialog, managing CV debug masks,
 * calibration modes, and updating Computer Vision algorithm parameters (Hough/Canny).
 *
 * @param state The current state of the application.
 * @param action The advanced option or debug event to process.
 * @return A new [CueDetatState] reflecting the configuration change.
 */
internal fun reduceAdvancedOptionsAction(
    state: CueDetatState,
    action: MainScreenEvent
): CueDetatState {
    // Switch on the specific type of MainScreenEvent provided.
    return when (action) {
        // Event: Toggle the visibility of the Advanced Options Dialog.
        is MainScreenEvent.ToggleAdvancedOptionsDialog -> {
            // Determine if we are opening or closing the dialog (toggle current state).
            val isOpening = !state.showAdvancedOptionsDialog

            // If we are closing the dialog:
            if (!isOpening) {
                // Close the dialog and also reset any related debug/calibration modes
                // that rely on the dialog or should be cleared when it closes.
                state.copy(
                    showAdvancedOptionsDialog = false, // Hide dialog.
                    showCvMask = false, // Hide CV debug mask.
                    isTestingCvMask = false, // Stop CV mask testing mode.
                    isCalibratingColor = false // Stop color calibration mode.
                )
            } else {
                // If we are opening the dialog, simply set the flag to true.
                state.copy(showAdvancedOptionsDialog = true)
            }
        }

        // Event: Toggle the CV debug mask visibility overlay.
        is MainScreenEvent.ToggleCvMask -> state.copy(showCvMask = !state.showCvMask)

        // Event: Enter the specific mode for testing CV masks (live adjustments).
        is MainScreenEvent.EnterCvMaskTestMode -> state.copy(
            isTestingCvMask = true, // Enable test mode logic.
            showCvMask = true, // Ensure the mask is visible so the user can see changes.
            showAdvancedOptionsDialog = false // Hide the main dialog to clear the view.
        )

        // Event: Exit the CV mask testing mode.
        is MainScreenEvent.ExitCvMaskTestMode -> state.copy(
            isTestingCvMask = false, // Disable test mode logic.
            showCvMask = true, // Keep the mask visible (or typically reset, logic implies keeping it).
            showAdvancedOptionsDialog = true // Re-open the main dialog.
        )

        // Event: Enter the Color Calibration mode.
        is MainScreenEvent.EnterCalibrationMode -> state.copy(
            isCalibratingColor = true, // Enable calibration logic (touch to sample).
            showAdvancedOptionsDialog = false, // Hide dialog to allow screen interaction.
            showCvMask = false // Hide mask to see the raw camera feed clearly.
        )

        // Event: The user has touched the screen to sample a color at a specific point.
        is MainScreenEvent.SampleColorAt -> state.copy(
            colorSamplePoint = action.screenPosition, // Store the touch coordinates.
            isCalibratingColor = false, // Calibration interaction is complete (one-shot).
            isTestingCvMask = true, // Switch to testing mode to verify the sampled color.
            showCvMask = true // Show the mask to visualize the result of the new color.
        )

        // Event: Cycle through available CV refinement methods (e.g., filtering algorithms).
        is MainScreenEvent.ToggleCvRefinementMethod -> state.copy(
            // Call .next() on the enum to get the next method in the sequence.
            cvRefinementMethod = state.cvRefinementMethod.next()
        )

        // Event: Update the first parameter of the Hough Circle Transform.
        is MainScreenEvent.UpdateHoughP1 -> state.copy(houghP1 = action.value)

        // Event: Update the second parameter of the Hough Circle Transform.
        is MainScreenEvent.UpdateHoughP2 -> state.copy(houghP2 = action.value)

        // Event: Update the accumulation threshold for the Hough Circle Transform.
        is MainScreenEvent.UpdateHoughThreshold -> state.copy(houghThreshold = action.value.toInt())

        // Event: Update the first threshold for the Canny Edge Detector.
        is MainScreenEvent.UpdateCannyT1 -> state.copy(cannyThreshold1 = action.value)

        // Event: Update the second threshold for the Canny Edge Detector.
        is MainScreenEvent.UpdateCannyT2 -> state.copy(cannyThreshold2 = action.value)

        // Fallback: If the event is not handled by this reducer, return state unchanged.
        else -> state
    }
}
