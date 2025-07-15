// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/ActionReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
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

        // Create the default positional state based on the logical origin (0,0)
        val initialSliderPos = 0f
        val initialLogicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, initialSliderPos)

        val targetBallCenter = PointF(0f, 0f) // The sacred logical origin
        val newProtractorUnit = ProtractorUnit(center = targetBallCenter, radius = initialLogicalRadius, rotationDegrees = 180f) // Corrected to point UP
        val newOnPlaneBall: OnPlaneBall?

        if (currentState.showTable) {
            // If table is shown, calculate cue ball position based on head spot
            newOnPlaneBall = OnPlaneBall(center = reducerUtils.getDefaultCueBallPosition(currentState), radius = initialLogicalRadius)
        } else {
            // If no table, place cue ball at a default logical offset, only if it currently exists
            newOnPlaneBall = if (currentState.onPlaneBall != null) {
                OnPlaneBall(center = PointF(0f, initialLogicalRadius * 4), radius = initialLogicalRadius)
            } else {
                null
            }
        }

        // Reset only positional and rotational properties, preserving toggles
        return currentState.copy(
            protractorUnit = newProtractorUnit,
            onPlaneBall = newOnPlaneBall,
            obstacleBalls = emptyList(),
            zoomSliderPosition = initialSliderPos,
            tableRotationDegrees = 0f, // Always reset table rotation
            bankingAimTarget = null,
            valuesChangedSinceReset = false,
            preResetState = stateToSave,
            hasCueBallBeenMoved = false,
            hasTargetBallBeenMoved = false
        )
    }
}