// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/CvReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Handles state updates related to Computer Vision data flow.
 *
 * This includes:
 * 1. Integrating new frames of data (ball positions) from the [VisionRepository].
 * 2. Managing the "Color Lock" feature (saving a specific HSV value to track).
 *
 * @param state The current state.
 * @param action The CV-related event.
 * @return The new state.
 */
internal fun reduceCvAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    // Process the incoming action based on its type.
    return when (action) {
        // Case: New vision data has been received from the vision pipeline.
        is MainScreenEvent.CvDataUpdated -> {
            // New vision data arrived from the camera pipeline.
            // We simply update the state holder with the new data object.
            // Note: The actual "Snapping" of the virtual ball to the detected ball
            // happens in [SnapReducer] or within the UpdateStateUseCase during the AIMING phase.
            // Here we just store the raw data for later processing/rendering.
            state.copy(visionData = action.visionData)
        }

        // Case: User requests to Lock or Unlock the ball tracking color.
        is MainScreenEvent.LockOrUnlockColor -> {
            // User clicked the "Lock" button.
            // Check if we are currently unlocked (null).
            if (state.lockedHsvColor == null) {
                // LOCK ACTION:
                // If currently unlocked, lock to the CURRENTLY DETECTED color from the vision data.
                // We also set a default standard deviation tolerance for the lock.
                state.copy(
                    // Set the locked color to the mean HSV detected in the current frame.
                    lockedHsvColor = state.visionData?.detectedHsvColor,
                    // Initialize the standard deviation (tolerance) for the lock.
                    // We default to Hue=10, Sat=50, Val=50 if a color is detected.
                    lockedHsvStdDev = state.visionData?.detectedHsvColor?.let {
                        floatArrayOf(
                            10f, // Hue tolerance
                            50f, // Saturation tolerance
                            50f  // Value tolerance
                        )
                    }
                )
            } else {
                // UNLOCK ACTION:
                // If currently locked, clear the lock variables.
                // This allows auto-detection logic or new sampling to occur.
                state.copy(
                    lockedHsvColor = null, // Clear locked mean color.
                    lockedHsvStdDev = null // Clear locked tolerance.
                )
            }
        }

        // Case: Explicitly lock to a specific color (e.g., from Debug menu or preset).
        is MainScreenEvent.LockColor -> {
            // Update the state with the provided mean and standard deviation.
            state.copy(
                lockedHsvColor = action.hsvMean, // Set the specific mean color.
                lockedHsvStdDev = action.hsvStdDev // Set the specific tolerance.
            )
        }

        // Case: Clear the point on the screen used for manual color sampling.
        is MainScreenEvent.ClearSamplePoint -> {
            // User stopped touching the screen for color sampling (ACTION_UP).
            // Reset the sample point to null so sampling stops.
            state.copy(colorSamplePoint = null)
        }

        // Default: If the action is not a CV-related action handled here, return state as-is.
        else -> state
    }
}
