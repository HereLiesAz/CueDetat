// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SnapReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.data.BallType
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.domain.Assignment
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
/** Which virtual ball an anchor belongs to, for matching both to candidates together. */
private enum class AnchorKind { CUE, TARGET }

class SnapReducer @Inject constructor() {

    // The time in milliseconds a ball must be tracked before becoming a confirmed snap target.
    private val SNAP_THRESHOLD_MS = 1500L

    // The maximum distance a detection can move and still be considered the "same" candidate.
    // Unit is NOT consistently pixels: per VisionRepository's genericBalls/balls construction,
    // these are logical-plane units when a homography pose exists, and screen pixels otherwise.
    private val SNAP_PROXIMITY_THRESHOLD = 30f

    /**
     * Updates the list of snap candidates based on new vision data.
     *
     * @param currentState The current application state.
     * @param visionData The latest frame of computer vision detections.
     * @param nowMs Wall-clock time for the hysteresis. Defaults to [System.currentTimeMillis],
     *   so production calls are time-dependent; tests pass an explicit value.
     * @return The updated state with refreshed [CueDetatState.snapCandidates].
     */
    fun reduce(
        currentState: CueDetatState,
        visionData: VisionData,
        nowMs: Long = System.currentTimeMillis()
    ): CueDetatState {
        // If snapping logic is globally disabled, clear all candidates and return.
        if (!currentState.isSnappingEnabled) {
            return currentState.copy(snapCandidates = emptyList())
        }

        val currentTime = nowMs

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

        // Match detections to last frame's candidates one-to-one, optimally (Assignment): matching
        // each detection to its nearest candidate in turn lets an early detection take the
        // candidate a later one needed, so two close balls could swap or drop a track.
        val matched = Assignment.match(
            rows = detectedBalls.size,
            cols = previousCandidates.size,
            gate = SNAP_PROXIMITY_THRESHOLD.toDouble(),
        ) { row, col ->
            hypot(
                (previousCandidates[col].detectedPoint.x - detectedBalls[row].first.x).toDouble(),
                (previousCandidates[col].detectedPoint.y - detectedBalls[row].first.y).toDouble()
            )
        }

        // Iterate through every detected ball in the current frame.
        for ((index, ball) in detectedBalls.withIndex()) {
            val point = ball.first
            val type = ball.second

            // The candidate this detection was matched to, if any (within the proximity threshold).
            val closestExisting = matched[index].takeIf { it >= 0 }?.let { previousCandidates[it] }

            if (closestExisting != null) {
                // MATCH FOUND: This detection corresponds to an existing candidate.

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
        val anchorFollowThreshold = SNAP_PROXIMITY_THRESHOLD * 3

        var newOnPlaneBall = currentState.onPlaneBall
        var newProtractorUnit = currentState.protractorUnit
        var newCueBallAnchor = currentState.cueBallCvAnchor
        var newTargetAnchor = currentState.targetCvAnchor

        // Both anchors are matched to candidates together, so two adjacent balls can't both be
        // claimed by the same candidate: the cue ball is type-agnostic (always white), the target
        // only follows a candidate of its suit. An anchor with no candidate in range holds its last
        // known position (anchor and virtual ball unchanged).
        val matchesTarget: (SnapCandidate) -> Boolean = { candidate ->
            candidate.ballType == BallType.UNKNOWN ||
            (currentState.targetType == TargetType.STRIPES && candidate.ballType == BallType.STRIPE) ||
            (currentState.targetType == TargetType.SOLIDS && candidate.ballType == BallType.SOLID)
        }
        val anchors = buildList {
            currentState.cueBallCvAnchor?.let { add(AnchorKind.CUE to it) }
            currentState.targetCvAnchor?.let { add(AnchorKind.TARGET to it) }
        }
        val follow = Assignment.match(
            rows = anchors.size,
            cols = newCandidates.size,
            gate = anchorFollowThreshold.toDouble(),
        ) { row, col ->
            val (kind, anchor) = anchors[row]
            val candidate = newCandidates[col]
            if (kind == AnchorKind.TARGET && !matchesTarget(candidate)) Double.POSITIVE_INFINITY
            else hypot(
                (candidate.detectedPoint.x - anchor.x).toDouble(),
                (candidate.detectedPoint.y - anchor.y).toDouble()
            )
        }
        anchors.forEachIndexed { row, (kind, _) ->
            val col = follow[row]
            if (col < 0) return@forEachIndexed
            val point = newCandidates[col].detectedPoint
            when (kind) {
                AnchorKind.CUE -> {
                    newCueBallAnchor = point
                    newOnPlaneBall = newOnPlaneBall?.copy(center = point)
                }
                AnchorKind.TARGET -> {
                    newTargetAnchor = point
                    newProtractorUnit = newProtractorUnit.copy(center = point)
                }
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
