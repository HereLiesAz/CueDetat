package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Reducer function responsible for handling Obstacle-related events.
 *
 * This primarily involves adding new obstacle balls to the virtual table,
 * intelligently placing them where they don't overlap with existing balls.
 *
 * @param state The current state of the application.
 * @param action The obstacle-related event.
 * @param reducerUtils Utility class for default positions.
 * @return A new [CueDetatState] with the obstacle modifications.
 */
internal fun reduceObstacleAction(
    state: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils
): CueDetatState {
    // Determine the specific obstacle action to perform.
    return when (action) {
        // Case: User requests to add a new obstacle ball.
        is MainScreenEvent.AddObstacleBall -> handleAddObstacleBall(state, reducerUtils)

        // Fallback: If the action is not handled here, return the state unchanged.
        else -> state
    }
}

/**
 * Handles the logic for adding a single obstacle ball to the state.
 *
 * It calculates a valid position for the new ball and adds it to the list.
 *
 * @param state The current state.
 * @param reducerUtils Utility for defaults.
 * @return The updated state with the new obstacle ball added.
 */
private fun handleAddObstacleBall(state: CueDetatState, reducerUtils: ReducerUtils): CueDetatState {
    // Determine the radius for the new ball. Use the cue ball radius if available, else target ball radius.
    val newBallRadius = state.onPlaneBall?.radius ?: state.protractorUnit.radius

    // Calculate a valid placement position (x, y) for the new obstacle.
    val placement = findNextAvailablePlacement(state, reducerUtils)

    // Create the new obstacle ball object at the calculated position.
    val newBall = OnPlaneBall(center = placement, radius = newBallRadius)

    // Append the new ball to the existing list of obstacle balls.
    val updatedList = state.obstacleBalls + newBall

    // Return the updated state with the new list and mark that values have changed.
    return state.copy(obstacleBalls = updatedList, valuesChangedSinceReset = true)
}

/**
 * Algorithms to find a suitable empty spot for a new obstacle ball.
 *
 * It tries the following strategies in order:
 * 1. Snap to a physically detected ball that isn't already "claimed" by a virtual ball.
 * 2. If a table is defined, try random positions within the table bounds.
 * 3. Fallback to a grid-based placement relative to the center.
 *
 * @param state The current state.
 * @param reducerUtils Utility for defaults.
 * @return A PointF representing the center of the new obstacle.
 */
private fun findNextAvailablePlacement(state: CueDetatState, reducerUtils: ReducerUtils): PointF {
    // Get the latest computer vision data. If none exists, default to center table.
    val visionData = state.visionData ?: return reducerUtils.getDefaultTargetBallPosition()

    // Combine all detected physical balls (generic pool balls + custom trained balls).
    val detectedBalls = visionData.genericBalls + visionData.customBalls

    // List all currently existing virtual balls (cue ball, target ball, existing obstacles).
    val allLogicalBalls = (listOfNotNull(
        state.onPlaneBall,
        state.protractorUnit
    ) + state.obstacleBalls).map { it.center } // Map to their center points.

    // Strategy 1: Find a detected physical ball that doesn't overlap with any virtual ball.
    val nextAvailableBall = detectedBalls.firstOrNull { detected ->
        // Check if this detected ball is far enough away from ALL existing virtual balls.
        allLogicalBalls.none { logical ->
            // Calculate distance between detected ball and virtual ball.
            hypot(
                (logical.x - detected.x).toDouble(),
                (logical.y - detected.y).toDouble()
            ) < (state.protractorUnit.radius * 2) // Threshold: 2x radius (diameter) means overlapping.
        }
    }

    // If we found a free physical ball, use its position.
    if (nextAvailableBall != null) {
        return nextAvailableBall
    }

    // Strategy 2: If we have a virtual table defined, place randomly within it.
    if (state.table.isVisible) {
        // Get table corners to determine bounds.
        val corners = state.table.corners
        // If table corners are invalid/empty, fallback to default.
        if (corners.isEmpty()) return reducerUtils.getDefaultTargetBallPosition()

        // Calculate bounding box of the table.
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }

        // Try 10 times to find a random spot that doesn't overlap.
        for (i in 0 until 10) {
            // Generate random X within table width.
            val randomX = Random.nextFloat() * (maxX - minX) + minX
            // Generate random Y within table height.
            val randomY = Random.nextFloat() * (maxY - minY) + minY
            // Create the candidate point.
            val candidatePoint = PointF(randomX, randomY)

            // Verify the point is actually inside the table polygon (handling irregular shapes).
            if (state.table.isPointInside(candidatePoint)) {
                // Check for overlaps with existing virtual balls.
                val isOverlapping = allLogicalBalls.any {
                    // Calculate distance.
                    hypot(
                        (it.x - candidatePoint.x).toDouble(),
                        (it.y - candidatePoint.y).toDouble()
                    ) < (state.protractorUnit.radius * 2.5) // Use slightly larger buffer (2.5x radius).
                }
                // If no overlap, we found a valid spot.
                if (!isOverlapping) return candidatePoint
            }
        }
        // If 10 random attempts fail, fallback to default center.
        return reducerUtils.getDefaultTargetBallPosition()
    } else {
        // Strategy 3: No table, no free detected balls. Use a fixed grid pattern.
        // Get the current count to determine position index.
        val numObstacles = state.obstacleBalls.size
        // Cycle through 4 quadrants.
        val quadrantIndex = numObstacles % 4
        // Get ball radius for spacing.
        val radius = state.protractorUnit.radius

        // Calculate X offset: alternate left/right.
        val xOffset = if (quadrantIndex % 2 == 0) -radius * 3 else radius * 3
        // Calculate Y offset: top two vs bottom two.
        val yOffset = if (quadrantIndex < 2) -radius * 6 else radius * 6

        // Return the grid-calculated point.
        return PointF(xOffset, yOffset)
    }
}
