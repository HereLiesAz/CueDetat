// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SnapReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SnapCandidate
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

        // Update existing candidates and find new ones
        for (detectedBall in detectedBalls) {
            val closestExisting = previousCandidates.minByOrNull {
                hypot((it.detectedPoint.x - detectedBall.x).toDouble(), (it.detectedPoint.y - detectedBall.y).toDouble())
            }

            if (closestExisting != null && hypot((closestExisting.detectedPoint.x - detectedBall.x).toDouble(), (closestExisting.detectedPoint.y - detectedBall.y).toDouble()) < SNAP_PROXIMITY_THRESHOLD_PX) {
                // This is a returning candidate
                previousCandidates.remove(closestExisting)
                val isNowConfirmed = closestExisting.isConfirmed || (currentTime - closestExisting.firstSeenTimestamp > SNAP_THRESHOLD_MS)
                newCandidates.add(closestExisting.copy(detectedPoint = detectedBall, isConfirmed = isNowConfirmed))
            } else {
                // This is a new candidate
                newCandidates.add(SnapCandidate(detectedPoint = detectedBall, firstSeenTimestamp = currentTime))
            }
        }

        // The heresy of passive auto-snapping has been purged.
        // Snapping now only occurs in the GestureReducer upon user action.
        // This reducer is now only responsible for maintaining the candidate list.

        return currentState.copy(
            snapCandidates = newCandidates,
        )
    }
}