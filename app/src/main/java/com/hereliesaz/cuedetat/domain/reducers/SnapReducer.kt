// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SnapReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.model.SnapCandidate
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot

@Singleton
class SnapReducer @Inject constructor() {
    private val SNAP_THRESHOLD_MS = 1500L
    private val SNAP_PROXIMITY_THRESHOLD_PX = 30f

    fun reduce(currentState: OverlayState, visionData: VisionData): OverlayState {
        if (!currentState.isSnappingEnabled) {
            return currentState.copy(snapCandidates = emptyList())
        }

        val currentTime = System.currentTimeMillis()
        val detectedBalls = visionData.genericBalls + visionData.customBalls
        val previousCandidates = currentState.snapCandidates.toMutableList()
        val newCandidates = mutableListOf<SnapCandidate>()

        for (detectedBall in detectedBalls) {
            val closestExisting = previousCandidates.minByOrNull {
                hypot((it.detectedPoint.x - detectedBall.x).toDouble(), (it.detectedPoint.y - detectedBall.y).toDouble())
            }

            if (closestExisting != null && hypot((closestExisting.detectedPoint.x - detectedBall.x).toDouble(), (closestExisting.detectedPoint.y - detectedBall.y).toDouble()) < SNAP_PROXIMITY_THRESHOLD_PX) {
                previousCandidates.remove(closestExisting)
                val isNowConfirmed = closestExisting.isConfirmed || (currentTime - closestExisting.firstSeenTimestamp > SNAP_THRESHOLD_MS)
                newCandidates.add(closestExisting.copy(detectedPoint = detectedBall, isConfirmed = isNowConfirmed))
            } else {
                newCandidates.add(SnapCandidate(detectedPoint = detectedBall, firstSeenTimestamp = currentTime))
            }
        }

        val confirmedCandidates = newCandidates.filter { it.isConfirmed }.toMutableList()
        var newProtractorUnit = currentState.protractorUnit
        var newOnPlaneBall = currentState.onPlaneBall

        if (confirmedCandidates.isNotEmpty() && !currentState.hasTargetBallBeenMoved) {
            newProtractorUnit = currentState.protractorUnit.copy(center = confirmedCandidates.first().detectedPoint)
            confirmedCandidates.removeAt(0)
        }

        if (confirmedCandidates.isNotEmpty() && currentState.onPlaneBall != null && !currentState.hasCueBallBeenMoved) {
            newOnPlaneBall = currentState.onPlaneBall.copy(center = confirmedCandidates.first().detectedPoint)
        }

        return currentState.copy(
            snapCandidates = newCandidates,
            protractorUnit = newProtractorUnit,
            onPlaneBall = newOnPlaneBall
        )
    }
}