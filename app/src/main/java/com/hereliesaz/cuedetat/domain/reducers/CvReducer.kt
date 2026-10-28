package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.BallPlacement
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
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
            // Starts the low-light loop in CvDataUpdated. It no longer needs a locked felt
            // colour: it only lowers the Canny thresholds until detections appear.
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
        is MainScreenEvent.AddFeltSample -> {
            val newList = state.savedFeltSamples.toMutableList()
            newList.add(com.hereliesaz.cuedetat.domain.FeltSample(hsv = action.hsv))
            state.copy(savedFeltSamples = newList)
        }
        is MainScreenEvent.DeleteFeltSamples -> {
            val newList = state.savedFeltSamples.filterNot { it.id in action.ids }
            state.copy(savedFeltSamples = newList)
        }
        // The Cue ball / Target ball buttons: arm that pick, or disarm it if it is already armed.
        is MainScreenEvent.BeginBallSelection ->
            state.copy(
                ballSelectionPhase =
                    if (state.ballSelectionPhase == action.phase) BallSelectionPhase.NONE else action.phase,
            )

        // The tap that completes a pick. Snaps to a detected ball within BallPlacement.SNAP_RADIUS,
        // else goes where the finger landed; moves the virtual ball there either way.
        is MainScreenEvent.ArSurfaceTapped -> {
            val inverse = state.inversePitchMatrix ?: return state
            val tapped = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(
                android.graphics.PointF(action.screenPoint.x, action.screenPoint.y),
                inverse
            )
            val placed = BallPlacement.resolve(
                tapped, state.ballSelectionPhase, state.targetType,
                state.snapCandidates, state.visionData?.balls,
            )
            // Only a snapped pick follows its detection; a free placement stays where it was put.
            val anchor = if (placed.snapped) placed.position else null
            when (state.ballSelectionPhase) {
                BallSelectionPhase.AWAITING_CUE -> state.copy(
                    onPlaneBall = (state.onPlaneBall ?: OnPlaneBall(placed.position, LOGICAL_BALL_RADIUS))
                        .copy(center = placed.position),
                    cueBallCvAnchor = anchor,
                    hasCueBallBeenMoved = true,
                    // First-run setup carries on to the target; re-picking the cue ball does not.
                    ballSelectionPhase = if (state.hasTargetBallBeenMoved || state.targetCvAnchor != null)
                        BallSelectionPhase.NONE else BallSelectionPhase.AWAITING_TARGET,
                    valuesChangedSinceReset = true,
                )
                BallSelectionPhase.AWAITING_TARGET -> state.copy(
                    protractorUnit = state.protractorUnit.copy(center = placed.position),
                    targetCvAnchor = anchor,
                    hasTargetBallBeenMoved = true,
                    ballSelectionPhase = BallSelectionPhase.NONE,
                    valuesChangedSinceReset = true,
                )
                BallSelectionPhase.NONE -> state
            }
        }
        is MainScreenEvent.ForceArActive -> {
            if (state.cameraMode == CameraMode.AR_ACTIVE) return state
            state.copy(
                cameraMode = CameraMode.AR_ACTIVE,
                arConfidenceHistory = emptyList(),
                arLowConfidenceFrameCount = 0,
                relocaliserDeltaQ = null,
                relocaliserAttemptFrames = 0
            )
        }
        is MainScreenEvent.SeedRelocaliser -> {
            state.copy(
                relocaliserDeltaQ = action.deltaQ,
                relocaliserAttemptFrames = 0
            )
        }
        else -> state
    }
}
