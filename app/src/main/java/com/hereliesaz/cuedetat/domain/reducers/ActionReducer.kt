package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

/**
 * Reducer function responsible for handling "Action" events, primarily the Reset functionality.
 *
 * This function takes the current state and a Reset event, and returns a new state
 * that restores default values or reverts to a pre-reset checkpoint.
 *
 * @param state The current state of the application.
 * @param action The reset event triggering this reduction.
 * @param reducerUtils Utility class providing default position calculations.
 * @return A new [CueDetatState] with the reset logic applied.
 */
internal fun reduceAction(
    state: CueDetatState,
    action: MainScreenEvent.Reset,
    reducerUtils: ReducerUtils
): CueDetatState {
    // Determine the type of Reset action and apply the appropriate logic.
    return when (action) {

        // Fallback for generic reset logic (currently the only implementation).
        else -> {
            // Check if a pre-reset state exists (i.e., we are currently in a "reset" or "undo" pending state).
            state.preResetState?.let {
                // If it exists, restore that saved state but clear the checkpoint.
                return it.copy(
                    // Clear the saved state to avoid infinite recursion or stale data.
                    preResetState = null,
                    // Ensure obstacle balls are cleared upon restoration.
                    obstacleBalls = emptyList(),
                    // Ensure the world is unlocked upon restoration.
                    isWorldLocked = false
                )
            }

            // If no pre-reset state exists, we are initiating a reset.
            // Save the current state so the user can undo this reset if it was accidental.
            val stateToSave = state.copy()

            // Define the initial default position for the zoom slider (0.0).
            val initialSliderPos = 0f

            // Calculate the default position for the target ball (usually center table).
            val targetBallCenter = reducerUtils.getDefaultTargetBallPosition()

            // Calculate the default position for the cue ball (usually based on screen center/camera).
            val cueBallCenter = reducerUtils.getDefaultCueBallPosition(state)

            // Create a new ProtractorUnit (target ball) at the default position with 0 rotation.
            val newProtractorUnit = ProtractorUnit(
                center = targetBallCenter,
                radius = LOGICAL_BALL_RADIUS, // Use standard pool ball radius.
                rotationDegrees = 0f // Reset rotation to 0.
            )

            // Determine if the cue ball (OnPlaneBall) should be active.
            // If it was present in the old state, recreate it at the default position.
            val newOnPlaneBall = if (state.onPlaneBall != null) {
                // Create new cue ball instance at default center.
                OnPlaneBall(center = cueBallCenter, radius = LOGICAL_BALL_RADIUS)
            } else {
                // If it wasn't active, keep it null.
                null
            }

            // Return the completely reset state.
            state.copy(
                // Apply the reset target ball.
                protractorUnit = newProtractorUnit,
                // Apply the reset cue ball.
                onPlaneBall = newOnPlaneBall,
                // Clear all obstacle balls.
                obstacleBalls = emptyList(),
                // Reset zoom level to default.
                zoomSliderPosition = initialSliderPos,
                // Reset world rotation to 0.
                worldRotationDegrees = 0f,
                // Clear any banking aim targets.
                bankingAimTarget = null,
                // Mark that no values have been changed since this reset.
                valuesChangedSinceReset = false,
                // Store the previous state to allow for "Undo".
                preResetState = stateToSave,
                // Reset movement flags for tracking user interaction.
                hasCueBallBeenMoved = false,
                hasTargetBallBeenMoved = false,
                // Unlock the world (AR tracking resumes standard behavior).
                isWorldLocked = false,
                // Reset the view offset (panning) to (0,0).
                viewOffset = PointF(0f, 0f)
            )
        }
    }
}
