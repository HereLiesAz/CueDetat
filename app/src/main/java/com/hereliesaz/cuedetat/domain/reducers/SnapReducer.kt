package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class SnapReducer @Inject constructor() {

    private val snapConfirmTimeMs = 2000L

    fun reduce(event: MainScreenEvent.SnapToDetectedBall, state: OverlayState): OverlayState {
        val currentTime = System.currentTimeMillis()
        val logicalPoint = event.ball
        val currentTarget = state.protractorUnit.center
        val currentCue = state.onPlaneBall?.center

        val snapCandidates = state.snapCandidates.toMutableList()

        val existingCandidate = snapCandidates.find {
            val dx = it.detectedPoint.x - logicalPoint.x
            val dy = it.detectedPoint.y - logicalPoint.y
            (dx * dx + dy * dy) < (state.protractorUnit.radius * state.protractorUnit.radius)
        }

        if (existingCandidate != null) {
            if (!existingCandidate.isConfirmed && currentTime - existingCandidate.firstSeenTimestamp > snapConfirmTimeMs) {
                val confirmedCandidate = existingCandidate.copy(isConfirmed = true)
                snapCandidates[snapCandidates.indexOf(existingCandidate)] = confirmedCandidate
            }
        } else {
            // New candidate
            // snapCandidates.add(SnapCandidate(detectedPoint = logicalPoint, firstSeenTimestamp = currentTime))
        }

        val confirmedSnaps = snapCandidates.filter { it.isConfirmed }

        var newState = state
        if (confirmedSnaps.isNotEmpty()) {
            val hasTargetMoved = state.hasTargetBallBeenMoved
            if (!hasTargetMoved) {
                // Find closest confirmed snap to current target ball
                val closestToTarget = confirmedSnaps.minByOrNull {
                    val dx = it.detectedPoint.x - currentTarget.x
                    val dy = it.detectedPoint.y - currentTarget.y
                    dx * dx + dy * dy
                }
                if (closestToTarget != null) {
                    newState = newState.copy(protractorUnit = newState.protractorUnit.copy(center = closestToTarget.detectedPoint))
                }
            }

            if (currentCue != null && !state.hasCueBallBeenMoved) {
                val closestToCue = confirmedSnaps.minByOrNull {
                    val dx = it.detectedPoint.x - currentCue.x
                    val dy = it.detectedPoint.y - currentCue.y
                    dx * dx + dy * dy
                }
                if (closestToCue != null) {
                    newState = newState.copy(onPlaneBall = newState.onPlaneBall?.copy(center = closestToCue.detectedPoint))
                }
            }
        }

        return newState.copy(snapCandidates = snapCandidates)
    }
}