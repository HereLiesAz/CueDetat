package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import kotlin.math.hypot

internal fun reduceCvAction(state: CueDetatState, action: CueDetatAction): CueDetatState {
    return when (action) {
        is CueDetatAction.CvDataUpdated -> {
            val stateWithNewData = state.copy(visionData = action.data)
            // Snap reducer logic is now triggered here
            reduceSnapAction(stateWithNewData, action.data)
        }

        is CueDetatAction.LockOrUnlockColor -> {
            if (state.lockedHsvColor == null) {
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
                state.copy(lockedHsvColor = null, lockedHsvStdDev = null)
            }
        }

        is CueDetatAction.LockColor -> {
            state.copy(lockedHsvColor = action.hsvMean, lockedHsvStdDev = action.hsvStdDev)
        }

        is CueDetatAction.ClearSamplePoint -> {
            state.copy(colorSamplePoint = null)
        }

        else -> state
    }
}

private fun reduceSnapAction(currentState: CueDetatState, visionData: VisionData): CueDetatState {
    if (!currentState.isSnappingEnabled) {
        return currentState.copy(snapCandidates = emptyList())
    }

    val SNAP_THRESHOLD_MS = 1500L
    val SNAP_PROXIMITY_THRESHOLD_PX = 30f
    val currentTime = System.currentTimeMillis()
    val detectedBalls = visionData.genericBalls + visionData.customBalls
    val previousCandidates = currentState.snapCandidates?.toMutableList() ?: mutableListOf()
    val newCandidates = mutableListOf<SnapCandidate>()

    for (detectedBall in detectedBalls) {
        val closestExisting = previousCandidates.minByOrNull {
            hypot(
                (it.detectedPoint.x - detectedBall.x).toDouble(),
                (it.detectedPoint.y - detectedBall.y).toDouble()
            )
        }

        if (closestExisting != null && hypot(
                (closestExisting.detectedPoint.x - detectedBall.x).toDouble(),
                (closestExisting.detectedPoint.y - detectedBall.y).toDouble()
            ) < SNAP_PROXIMITY_THRESHOLD_PX
        ) {
            previousCandidates.remove(closestExisting)
            val isNowConfirmed =
                closestExisting.isConfirmed || (currentTime - closestExisting.firstSeenTimestamp > SNAP_THRESHOLD_MS)
            newCandidates.add(
                closestExisting.copy(
                    detectedPoint = detectedBall,
                    isConfirmed = isNowConfirmed
                )
            )
        } else {
            newCandidates.add(
                SnapCandidate(
                    detectedPoint = detectedBall,
                    firstSeenTimestamp = currentTime
                )
            )
        }
    }

    return currentState.copy(snapCandidates = newCandidates)
}