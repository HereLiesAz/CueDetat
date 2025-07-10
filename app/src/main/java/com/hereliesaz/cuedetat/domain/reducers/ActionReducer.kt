package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionReducer @Inject constructor() {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.Reset -> handleReset(currentState)
            else -> currentState
        }
    }

    private fun handleReset(currentState: OverlayState): OverlayState {
        // If a pre-reset state exists, revert to it.
        currentState.preResetState?.let {
            return it.copy(preResetState = null) // Revert and clear the saved state
        }

        // Otherwise, this is the first press. Save the current positional state.
        val stateToSave = currentState.copy()

        // Create the default positional state
        val initialSliderPos = 0f
        val initialLogicalRadius = ReducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, initialSliderPos)
        val initialProtractorCenter =
            PointF(currentState.viewWidth / 2f, currentState.viewHeight / 2f)

        // Reset only positional and rotational properties, preserving toggles
        return currentState.copy(
            protractorUnit = ProtractorUnit(
                center = initialProtractorCenter,
                radius = initialLogicalRadius,
                rotationDegrees = 0f
            ),
            zoomSliderPosition = initialSliderPos,
            tableRotationDegrees = 0f,
            bankingAimTarget = null, // This is positional
            luminanceAdjustment = 0f,
            preResetState = stateToSave // Save the original state for reverting
        )
    }
}