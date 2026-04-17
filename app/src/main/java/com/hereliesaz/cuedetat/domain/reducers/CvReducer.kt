package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

object ArConfidenceConfig {
    const val AR_CONFIDENCE_THRESHOLD = 0.8f
    const val AR_DEGRADED_FLOOR = 0.5f
    const val AR_DEGRADED_FRAME_COUNT = 150
    const val AR_CONFIDENCE_WINDOW = 20
}

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

                    nextState = nextState.copy(
                        cannyThreshold1 = newT1,
                        cannyThreshold2 = newT2
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
            if (nextState.cameraMode == CameraMode.AR_SETUP &&
                nextState.lockedHsvColor != null &&
                nextState.tableScanModel != null) {

                val history = nextState.arConfidenceHistory.toMutableList()
                history.add(action.visionData.tableOverlayConfidence)
                if (history.size > ArConfidenceConfig.AR_CONFIDENCE_WINDOW) {
                    history.removeAt(0)
                }

                val smoothedConfidence = if (history.isNotEmpty()) history.sum() / history.size else 0f

                if (smoothedConfidence >= ArConfidenceConfig.AR_CONFIDENCE_THRESHOLD) {
                    nextState = nextState.copy(
                        cameraMode = CameraMode.AR_ACTIVE,
                        arConfidenceHistory = emptyList(),
                        arLowConfidenceFrameCount = 0
                    )
                } else if (smoothedConfidence >= ArConfidenceConfig.AR_DEGRADED_FLOOR) {
                    val newFrameCount = nextState.arLowConfidenceFrameCount + 1
                    if (newFrameCount >= ArConfidenceConfig.AR_DEGRADED_FRAME_COUNT) {
                        nextState = nextState.copy(
                            cameraMode = CameraMode.AR_ACTIVE,
                            warningText = "AR Tracking may be degraded.",
                            arConfidenceHistory = emptyList(),
                            arLowConfidenceFrameCount = 0
                        )
                    } else {
                        nextState = nextState.copy(
                            arConfidenceHistory = history.toList(),
                            arLowConfidenceFrameCount = newFrameCount
                        )
                    }
                } else {
                    nextState = nextState.copy(
                        arConfidenceHistory = history.toList(),
                        arLowConfidenceFrameCount = 0
                    )
                }
            }

            nextState
        }

        is MainScreenEvent.LockColor -> state.copy(lockedHsvColor = action.hsvMean, lockedHsvStdDev = action.hsvStdDev)
        is MainScreenEvent.LockOrUnlockColor -> if (state.lockedHsvColor != null) state.copy(lockedHsvColor = null, lockedHsvStdDev = null) else state
        is MainScreenEvent.AddFeltSample -> {
            val newList = state.savedFeltSamples.toMutableList()
            newList.add(com.hereliesaz.cuedetat.domain.FeltSample(hsv = action.hsv))
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.DeleteFeltSamples -> {
            val newList = state.savedFeltSamples.filterNot { it.id in action.ids }
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.MoveFeltSample -> {
            val newList = state.savedFeltSamples.toMutableList()
            if (action.fromIndex in newList.indices && action.toIndex in newList.indices) {
                val item = newList.removeAt(action.fromIndex)
                newList.add(action.toIndex, item)
            }
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.ArSurfaceTapped -> {
            val inverse = state.inversePitchMatrix ?: return state
            val tappedLogical = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(
                android.graphics.PointF(action.screenPoint.x, action.screenPoint.y), 
                inverse
            )
            
            val allDetectedBalls = state.visionData?.balls ?: emptyList()
            
            val nearestBallPos = allDetectedBalls.minByOrNull { ball ->
                val dist = kotlin.math.hypot((ball.position.x - tappedLogical.x).toDouble(), (ball.position.y - tappedLogical.y).toDouble())
                
                // Preference matching: if we are looking for a specific type, prefer those detections
                val isMatchingType = when (state.ballSelectionPhase) {
                    com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_TARGET -> {
                        val targetIsStripe = state.targetType == com.hereliesaz.cuedetat.domain.TargetType.STRIPES
                        (ball.type == com.hereliesaz.cuedetat.data.BallType.STRIPE && targetIsStripe) ||
                        (ball.type == com.hereliesaz.cuedetat.data.BallType.SOLID && !targetIsStripe)
                    }
                    else -> false
                }
                
                if (isMatchingType) dist * 0.5 else dist // 50% distance bonus for matching types
            }?.position ?: tappedLogical

            when (state.ballSelectionPhase) {
                com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_CUE -> state.copy(
                    cueBallCvAnchor = nearestBallPos,
                    ballSelectionPhase = com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_TARGET
                )
                com.hereliesaz.cuedetat.domain.BallSelectionPhase.AWAITING_TARGET -> state.copy(
                    targetCvAnchor = nearestBallPos,
                    ballSelectionPhase = com.hereliesaz.cuedetat.domain.BallSelectionPhase.NONE
                )
                else -> state
            }
        }
        is MainScreenEvent.ClearSamplePoint -> state.copy(colorSamplePoint = null)
        else -> state
    }
}
