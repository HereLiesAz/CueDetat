package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Reducer function responsible for handling Spin (English) related events.
 *
 * This manages the user's interaction with the cue ball spin control, including:
 * - Dragging the spin indicator.
 * - Applying spin values.
 * - Handling the lingering visual state after spin is applied.
 *
 * @param state The current state of the application.
 * @param action The spin-related event.
 * @return A new [CueDetatState] with updated spin properties.
 */
internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    // Determine the specific spin action to process.
    return when (action) {
        // Case: A specific spin offset has been actively applied (e.g. while dragging).
        is MainScreenEvent.SpinApplied -> {
            state.copy(
                // Update the currently selected spin offset (normalized -1 to 1).
                selectedSpinOffset = action.offset,
                // Mark state as dirty/changed.
                valuesChangedSinceReset = true,
                // Clear any lingering "ghost" spin display since we have an active one.
                lingeringSpinOffset = null,
                // Clear calculated spin paths as they need to be recomputed for the new spin.
                spinPaths = emptyMap(),
                // Reset the alpha for drawing spin paths to full visibility.
                spinPathsAlpha = 1.0f
            )
        }

        // Case: The user has finished interacting with the spin control (released touch).
        is MainScreenEvent.SpinSelectionEnded -> {
            state.copy(
                // Transfer the active spin to "lingering" spin.
                // This allows the UI to show where the spin was set even when the control isn't active.
                lingeringSpinOffset = state.selectedSpinOffset,
                // Clear the active selection state.
                selectedSpinOffset = null
            )
        }

        // Case: The user is dragging the floating spin control widget itself (repositioning the UI).
        is MainScreenEvent.DragSpinControl -> {
            // Get the current position of the control. If null (hidden), ignore.
            val currentCenter = state.spinControlCenter ?: return state

            // Calculate the new position by adding the drag delta.
            val newCenter =
                PointF(currentCenter.x + action.delta.x, currentCenter.y + action.delta.y)

            // Update the state with the new control position.
            state.copy(spinControlCenter = newCenter)
        }

        // Case: Explicit request to clear all spin data (e.g. reset button).
        is MainScreenEvent.ClearSpinState -> {
            state.copy(
                // Remove the lingering visual indicator.
                lingeringSpinOffset = null,
                // Clear all calculated physics paths.
                spinPaths = emptyMap()
            )
        }

        // Fallback: If action is not handled here, return state unchanged.
        else -> state
    }
}
