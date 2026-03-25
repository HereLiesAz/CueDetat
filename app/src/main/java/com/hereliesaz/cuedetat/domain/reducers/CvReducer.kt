package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

internal fun reduceCvAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.AutoCalibrateCv -> {
            if (state.lockedHsvColor == null) return state
            state.copy(isAutoCalibrating = true)
        }

        is MainScreenEvent.CvDataUpdated -> {
            var nextState = state.copy(visionData = action.visionData)

            if (state.isAutoCalibrating) {
                // Low-light feedback loop: lower thresholds until bounding boxes appear.
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

                    if (newT1 <= 10f && newT2 <= 20f) {
                        nextState = nextState.copy(
                            isAutoCalibrating = false,
                            warningText = "Auto-calibration failed: Too dark."
                        )
                    }
                } else {
                    nextState = nextState.copy(isAutoCalibrating = false)
                }
            }
            // Auto-advance AR_SETUP → AR_ACTIVE when all conditions are met
            val AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD = 0.8f
            if (nextState.cameraMode == CameraMode.AR_SETUP &&
                nextState.lockedHsvColor != null &&
                nextState.tableScanModel != null &&
                action.visionData.tableOverlayConfidence >= AR_AUTO_CONFIRM_CONFIDENCE_THRESHOLD) {
                nextState = nextState.copy(cameraMode = CameraMode.AR_ACTIVE)
            }

            nextState
        }

        is MainScreenEvent.LockColor -> state.copy(lockedHsvColor = action.hsvMean, lockedHsvStdDev = action.hsvStdDev)
        is MainScreenEvent.LockOrUnlockColor -> if (state.lockedHsvColor != null) state.copy(lockedHsvColor = null, lockedHsvStdDev = null) else state
        is MainScreenEvent.ClearSamplePoint -> state.copy(colorSamplePoint = null)
        else -> state
    }
}