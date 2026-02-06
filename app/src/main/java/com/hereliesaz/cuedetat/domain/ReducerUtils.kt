// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ReducerUtils.kt

package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class providing helper methods for Reducers.
 *
 * Contains logic for:
 * - Calculating default positions for balls.
 * - Enforcing table boundaries (snapping balls back inside).
 * - Common geometric checks used across multiple reducers.
 *
 * @constructor Injected by Hilt.
 */
@Singleton
class ReducerUtils @Inject constructor() {

    /**
     * Returns the default position for the Target Ball (Protractor Unit).
     * Usually the geometric center of the logical space (0,0).
     */
    fun getDefaultTargetBallPosition(): PointF = PointF(0f, 0f)

    /**
     * Returns the default position for the Cue Ball.
     * Usually placed at the "Head Spot" (approx 1/4th up the table from center).
     */
    fun getDefaultCueBallPosition(state: CueDetatState): PointF {
        val headSpotY = state.table.logicalHeight / 4f // Relative to table center
        return PointF(0f, headSpotY)
    }

    /**
     * Validates ball positions against the current table boundaries and moves them inside if necessary.
     *
     * This is typically called after changing table size or rotation, where balls might
     * end up "off the table" in the new coordinate space.
     *
     * @param state The current state with potentially violating ball positions.
     * @return A new state with corrected ball positions.
     */
    fun snapViolatingBalls(state: CueDetatState): CueDetatState {
        // If the table is not visible (Infinite Space mode), boundaries don't apply.
        if (!state.table.isVisible) return state

        var updatedState = state

        // Check if the Target Ball (Protractor Unit) is inside the table.
        if (!state.table.isPointInside(state.protractorUnit.center)) {
            // If outside, reset to default center.
            updatedState = updatedState.copy(
                protractorUnit = state.protractorUnit.copy(center = getDefaultTargetBallPosition())
            )
        }

        // Check if the Cue Ball (OnPlaneBall) is inside the table.
        updatedState.onPlaneBall?.let { ball ->
            if (!state.table.isPointInside(ball.center)) {
                // If outside, reset to default head spot.
                updatedState = updatedState.copy(
                    onPlaneBall = ball.copy(center = getDefaultCueBallPosition(updatedState))
                )
            }
        }

        // Check Obstacle Balls.
        // Filter out any obstacles that are now outside the table.
        val confinedObstacles = updatedState.obstacleBalls.filter {
            state.table.isPointInside(it.center)
        }

        // If any were removed, update the list in the state.
        if (confinedObstacles.size != updatedState.obstacleBalls.size) {
            updatedState = updatedState.copy(obstacleBalls = confinedObstacles)
        }

        return updatedState
    }
}
