package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Reducer responsible for handling Computer Vision feedback and auto-calibration.
 */
internal fun reduceCvAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.AutoCalibrateCv -> {
            if (state.lockedHsvColor == null) return state
            state.copy(isAutoCalibrating = true)
        }

        is MainScreenEvent.CvDataUpdated -> {
            var nextState = state.copy(visionData = action.visionData)

            if (state.isAutoCalibrating) {
                // Low-light compensation loop:
                // If no bounding boxes are detected, we lower the standards for edge detection.
                val detectionCount = action.visionData.detectedBoundingBoxes.size

                if (detectionCount < 1) {
                    val newT1 = (state.cannyThreshold1 - 5f).coerceAtLeast(10f)
                    val newT2 = (state.cannyThreshold2 - 10f).coerceAtLeast(20f)
                    val newHough = (state.houghThreshold - 2).coerceAtLeast(15)

                    nextState = nextState.copy(
                        cannyThreshold1 = newT1,
                        cannyThreshold2 = newT2,
                        houghThreshold = newHough
                    )

                    // If we reach the floor and still see nothing, stop squinting.
                    if (newT1 <= 10f && newT2 <= 20f) {
                        nextState = nextState.copy(
                            isAutoCalibrating = false,
                            warningText = "Auto-calibration failed: Environment too dark."
                        )
                    }
                } else {
                    // Signal found.
                    nextState = nextState.copy(isAutoCalibrating = false)
                }
            }
            nextState
        }

        is MainScreenEvent.LockColor -> {
            state.copy(
                lockedHsvColor = action.hsvMean,
                lockedHsvStdDev = action.hsvStdDev
            )
        }

        is MainScreenEvent.LockOrUnlockColor -> {
            if (state.lockedHsvColor != null) {
                state.copy(lockedHsvColor = null, lockedHsvStdDev = null)
            } else {
                state
            }
        }

        is MainScreenEvent.ClearSamplePoint -> {
            state.copy(colorSamplePoint = null)
        }

        else -> state
    }
}