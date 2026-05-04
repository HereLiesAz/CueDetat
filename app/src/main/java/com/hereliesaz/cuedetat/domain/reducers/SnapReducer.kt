// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SnapReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.TargetType
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

        // Use typed DetectedBall list when available (has BallType classification),
        // falling back to the legacy untyped lists for paths that don't populate it.
        val detectedBalls = if (visionData.balls.isNotEmpty()) {
            visionData.balls.map { it.position to it.type }
        } else {
            @Suppress("DEPRECATION")
            (visionData.genericBalls + visionData.customBalls).map { it to BallType.UNKNOWN }
        }

        // Create a mutable copy of the previous candidates to track which ones update.
        val previousCandidates = currentState.snapCandidates?.toMutableList() ?: mutableListOf()

        // List to hold the candidates for the NEXT frame.
        val newCandidates = mutableListOf<SnapCandidate>()

        // Iterate through every detected ball in the current frame.
        for (ball in detectedBalls) {
            val point = ball.first
            val type = ball.second

            // Find the closest existing candidate from the previous frame.
            val closestExisting = previousCandidates.minByOrNull {
                // Calculate Euclidean distance to match detection to candidate.
                hypot(
                    (it.detectedPoint.x - point.x).toDouble(),
                    (it.detectedPoint.y - point.y).toDouble()
                )
            }

            // Check if the closest candidate is within the proximity threshold.
            if (closestExisting != null && hypot(
                    (closestExisting.detectedPoint.x - point.x).toDouble(),
                    (closestExisting.detectedPoint.y - point.y).toDouble()
                ) < SNAP_PROXIMITY_THRESHOLD_PX
            ) {
                // MATCH FOUND: This detection corresponds to an existing candidate.

                // Remove from the 'previous' list so we know it was updated (used for cleanup if needed).
                previousCandidates.remove(closestExisting)

                // Determine if it is now confirmed.
                // It is confirmed if it WAS ALREADY confirmed, OR if it has existed longer than the threshold.
                val isNowConfirmed = closestExisting.isConfirmed || (currentTime - closestExisting.firstSeenTimestamp > SNAP_THRESHOLD_MS)

                // Update the candidate with the new position, type, and confirmation status.
                newCandidates.add(
                    closestExisting.copy(
                        detectedPoint = point,
                        isConfirmed = isNowConfirmed,
                        ballType = type
                    )
                )
            } else {
                // NO MATCH: This is a brand new detection.
                // Create a new candidate with the current timestamp.
                newCandidates.add(
                    SnapCandidate(
                        detectedPoint = point,
                        firstSeenTimestamp = currentTime,
                        ballType = type
                    )
                )
            }
        }

        // NOTE: Any candidates remaining in 'previousCandidates' were NOT matched in this frame.
        // They are implicitly dropped (lost tracking). This acts as a basic debouncing/cleanup.

        // Re-anchor virtual balls to their nearest CV candidate, if anchored.
        // Uses a generous threshold (3× snap proximity) to hold through brief occlusions.
        val anchorFollowThreshold = SNAP_PROXIMITY_THRESHOLD_PX * 3

        var newOnPlaneBall = currentState.onPlaneBall
        var newProtractorUnit = currentState.protractorUnit
        var newCueBallAnchor = currentState.cueBallCvAnchor
        var newTargetAnchor = currentState.targetCvAnchor

        // Cue ball anchor follow — type-agnostic (cue ball is always white)
        currentState.cueBallCvAnchor?.let { anchor ->
            val nearest = newCandidates.minByOrNull {
                hypot((it.detectedPoint.x - anchor.x).toDouble(), (it.detectedPoint.y - anchor.y).toDouble())
            }
            val dist = nearest?.let {
                hypot((it.detectedPoint.x - anchor.x).toDouble(), (it.detectedPoint.y - anchor.y).toDouble())
            } ?: Double.MAX_VALUE
            if (nearest != null && dist < anchorFollowThreshold) {
                newCueBallAnchor = nearest.detectedPoint
                newOnPlaneBall = newOnPlaneBall?.copy(center = nearest.detectedPoint)
            }
            // else: hold last known position — anchor and virtual ball unchanged
        }

        // Target ball anchor follow — filtered by targetType
        currentState.targetCvAnchor?.let { anchor ->
            val matchesTarget: (SnapCandidate) -> Boolean = { candidate ->
                candidate.ballType == BallType.UNKNOWN ||
                (currentState.targetType == TargetType.STRIPES && candidate.ballType == BallType.STRIPE) ||
                (currentState.targetType == TargetType.SOLIDS && candidate.ballType == BallType.SOLID)
            }
            val nearest = newCandidates.filter(matchesTarget).minByOrNull {
                hypot((it.detectedPoint.x - anchor.x).toDouble(), (it.detectedPoint.y - anchor.y).toDouble())
            }
            val dist = nearest?.let {
                hypot((it.detectedPoint.x - anchor.x).toDouble(), (it.detectedPoint.y - anchor.y).toDouble())
            } ?: Double.MAX_VALUE
            if (nearest != null && dist < anchorFollowThreshold) {
                newTargetAnchor = nearest.detectedPoint
                newProtractorUnit = newProtractorUnit.copy(center = nearest.detectedPoint)
            }
        }

        return currentState.copy(
            snapCandidates = newCandidates,
            onPlaneBall = newOnPlaneBall,
            protractorUnit = newProtractorUnit,
            cueBallCvAnchor = newCueBallAnchor,
            targetCvAnchor = newTargetAnchor
        )
    }
}
