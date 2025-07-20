// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ActionReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionReducer @Inject constructor(private val reducerUtils: ReducerUtils) {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.Reset -> handleReset(currentState)
            else -> currentState
        }
    }

    private fun handleReset(currentState: OverlayState): OverlayState {
        // If a pre-reset state exists, revert to it.
        currentState.preResetState?.let {
            // Also clear any obstacles that may have been added after the save
            return it.copy(preResetState = null, obstacleBalls = emptyList())
        }

        // Otherwise, this is the first press. Save the current positional state.
        val stateToSave = currentState.copy()

        // Create the default positional state based on the logical origin.
        val initialSliderPos = 0f

        // The default positions are always relative to the logical table, even if not visible.
        val targetBallCenter = reducerUtils.getDefaultTargetBallPosition()
        val cueBallCenter = reducerUtils.getDefaultCueBallPosition(currentState)

        val newProtractorUnit = ProtractorUnit(
            center = targetBallCenter,
            radius = LOGICAL_BALL_RADIUS,
            rotationDegrees = 0f
        )
        val newOnPlaneBall = if (currentState.onPlaneBall != null) {
            OnPlaneBall(center = cueBallCenter, radius = LOGICAL_BALL_RADIUS)
        } else {
            null
        }

        // Reset only positional and rotational properties, preserving toggles
        return currentState.copy(
            protractorUnit = newProtractorUnit,
            onPlaneBall = newOnPlaneBall,
            obstacleBalls = emptyList(), // Clear obstacles on reset
            zoomSliderPosition = initialSliderPos,
            worldRotationDegrees = 0f,
            bankingAimTarget = null,
            valuesChangedSinceReset = false,
            preResetState = stateToSave,
            hasCueBallBeenMoved = false,
            hasTargetBallBeenMoved = false,
            viewOffset = PointF(0f, 0f) // Also reset pan
        )
    }
}