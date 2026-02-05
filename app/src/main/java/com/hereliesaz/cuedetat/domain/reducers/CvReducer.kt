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
    return when (action) {
        is MainScreenEvent.CvDataUpdated -> {
            // New vision data arrived from the camera pipeline.
            // We simply update the state holder.
            // Note: The actual "Snapping" of the virtual ball to the detected ball
            // happens in [SnapReducer] or within the UpdateStateUseCase during the AIMING phase.
            // Here we just store the raw data.
            state.copy(visionData = action.visionData)
        }

        is MainScreenEvent.LockOrUnlockColor -> {
            // User clicked the "Lock" button.
            if (state.lockedHsvColor == null) {
                // LOCK: If currently unlocked, lock to the CURRENTLY DETECTED color.
                // We also set a default standard deviation tolerance (Hue=10, Sat=50, Val=50).
                state.copy(
                    lockedHsvColor = state.visionData?.detectedHsvColor,
                    lockedHsvStdDev = state.visionData?.detectedHsvColor?.let {
                        floatArrayOf(
                            10f,
                            50f,
                            50f
                        )
                    }
                )
            } else {
                // UNLOCK: If currently locked, clear the lock to allow auto-detection or new sampling.
                state.copy(lockedHsvColor = null, lockedHsvStdDev = null)
            }
        }

        is MainScreenEvent.LockColor -> {
            // Explicit lock command (e.g., from the Debug menu).
            state.copy(lockedHsvColor = action.hsvMean, lockedHsvStdDev = action.hsvStdDev)
        }

        is MainScreenEvent.ClearSamplePoint -> {
            // User stopped touching the screen for color sampling.
            state.copy(colorSamplePoint = null)
        }

        else -> state
    }
}
