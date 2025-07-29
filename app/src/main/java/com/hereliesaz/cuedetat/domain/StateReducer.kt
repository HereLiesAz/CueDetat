package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.ui.CueDetatState
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel

/**
 * A pure function that takes the current state and an action, and returns
 * a new state. It is the only place where the application's state can be
 * mutated.
 */
fun stateReducer(currentState: CueDetatState, action: CueDetatAction): CueDetatState {
    return when (action) {
        is CueDetatAction.ToggleExperienceMode -> {
            val newOverlayState = if (currentState.overlay is OverlayState.None) {
                OverlayState.ExperienceModeSelection(currentState.experienceMode)
            } else {
                OverlayState.None
            }
            currentState.copy(overlay = newOverlayState)
        }

        is CueDetatAction.ApplyPendingExperienceMode -> {
            // If the mode is the same, just close the overlay.
            // Otherwise, update the mode and close the overlay.
            if (currentState.experienceMode == action.mode) {
                currentState.copy(overlay = OverlayState.None)
            } else {
                currentState.copy(
                    experienceMode = action.mode,
                    overlay = OverlayState.None
                )
            }
        }

        is CueDetatAction.HaterAction -> {
            // Delegate Hater actions to the Hater state reducer
            val newHaterState = haterStateReducer(currentState.haterState, action.action)
            currentState.copy(haterState = newHaterState)
        }
    }
}

/**
 * Reducer specifically for the Hater mode's state.
 */
private fun haterStateReducer(
    currentState: HaterViewModel.HaterState,
    action: HaterViewModel.Action
): HaterViewModel.HaterState {
    return when (action) {
        is HaterViewModel.Action.UpdatePhysics -> {
            currentState.copy(
                bodies = action.bodies.map { body ->
                    HaterViewModel.BodyState(
                        id = body.userData as? String ?: "",
                        x = body.position.x,
                        y = body.position.y,
                        angle = body.angle
                    )
                }
            )
        }

        is HaterViewModel.Action.SetHaterText -> {
            currentState.copy(haterText = action.text)
        }
    }
}