// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SnapReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.state.SnapCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.hypot

/**
 * A dedicated reducer for managing "Snap Candidates".
 *
 * This component processes raw vision data to identify stable ball detections
 * that the user might want to "snap" a virtual ball onto.
 *
 * It employs a hysteresis mechanism: a candidate must be detected consistently
 * over a period of time (`SNAP_THRESHOLD_MS`) before it is considered "Confirmed".
 *
 * @constructor Injected by Hilt.
 */
@Singleton
class SnapReducer @Inject constructor() {

    // The time in milliseconds a ball must be tracked before becoming a confirmed snap target.
    private val SNAP_THRESHOLD_MS = 1500L

    // The maximum distance in pixels a detection can move and still be considered the "same" candidate.
    private val SNAP_PROXIMITY_THRESHOLD_PX = 30f

    /**
     * Updates the list of snap candidates based on new vision data.
     *
     * @param currentState The current application state.
     * @param visionData The latest frame of computer vision detections.
     * @return The updated state with refreshed [CueDetatState.snapCandidates].
     */
    fun reduce(currentState: CueDetatState, visionData: VisionData): CueDetatState {
        // If snapping logic is globally disabled, clear all candidates and return.
        if (!currentState.isSnappingEnabled) {
            return currentState.copy(snapCandidates = emptyList())
        }

        // Get the current system time to calculate duration.
        val currentTime = System.currentTimeMillis()

        // Combine generic and custom trained balls into a single list of detections.
        val detectedBalls = visionData.genericBalls + visionData.customBalls

        // Create a mutable copy of the previous candidates to track which ones update.
        val previousCandidates = currentState.snapCandidates?.toMutableList() ?: mutableListOf()

        // List to hold the candidates for the NEXT frame.
        val newCandidates = mutableListOf<SnapCandidate>()

        // Iterate through every detected ball in the current frame.
        for (detectedBall in detectedBalls) {
            // Find the closest existing candidate from the previous frame.
            val closestExisting = previousCandidates.minByOrNull {
                // Calculate Euclidean distance to match detection to candidate.
                hypot(
                    (it.detectedPoint.x - detectedBall.x).toDouble(),
                    (it.detectedPoint.y - detectedBall.y).toDouble()
                )
            }

            // Check if the closest candidate is within the proximity threshold.
            if (closestExisting != null && hypot(
                    (closestExisting.detectedPoint.x - detectedBall.x).toDouble(),
                    (closestExisting.detectedPoint.y - detectedBall.y).toDouble()
                ) < SNAP_PROXIMITY_THRESHOLD_PX
            ) {
                // MATCH FOUND: This detection corresponds to an existing candidate.

                // Remove from the 'previous' list so we know it was updated (used for cleanup if needed).
                previousCandidates.remove(closestExisting)

                // Determine if it is now confirmed.
                // It is confirmed if it WAS ALREADY confirmed, OR if it has existed longer than the threshold.
                val isNowConfirmed = closestExisting.isConfirmed || (currentTime - closestExisting.firstSeenTimestamp > SNAP_THRESHOLD_MS)

                // Update the candidate with the new position and confirmation status.
                newCandidates.add(
                    closestExisting.copy(
                        detectedPoint = detectedBall,
                        isConfirmed = isNowConfirmed
                    )
                )
            } else {
                // NO MATCH: This is a brand new detection.
                // Create a new candidate with the current timestamp.
                newCandidates.add(
                    SnapCandidate(
                        detectedPoint = detectedBall,
                        firstSeenTimestamp = currentTime
                    )
                )
            }
        }

        // NOTE: Any candidates remaining in 'previousCandidates' were NOT matched in this frame.
        // They are implicitly dropped (lost tracking). This acts as a basic debouncing/cleanup.

        // The heresy of passive auto-snapping has been purged.
        // Snapping now only occurs in the GestureReducer upon user action (e.g., releasing a drag).
        // This reducer is now only responsible for maintaining the candidate list for UI visualization.

        // Return the state with the updated list of candidates.
        return currentState.copy(
            snapCandidates = newCandidates,
        )
    }
}
