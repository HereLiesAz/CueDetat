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

        // Create the default positional state based on a logical origin
        val initialSliderPos = 0f
        val initialLogicalRadius = reducerUtils.getCurrentLogicalRadius(currentState.viewWidth, currentState.viewHeight, initialSliderPos)

        val newProtractorUnit: ProtractorUnit
        val newOnPlaneBall: OnPlaneBall?
        val newTableRotation: Float

        if (currentState.showTable) {
            val targetBallCenter = PointF(0f, 0f) // Logical center
            val scale = (initialLogicalRadius * 2) / 2.25f
            val tablePlayingSurfaceHeight = currentState.tableSize.shortSideInches * scale
            val headSpotY = tablePlayingSurfaceHeight / 4f
            val actualCueBallCenter = PointF(0f, headSpotY)

            newProtractorUnit = ProtractorUnit(center = targetBallCenter, radius = initialLogicalRadius, rotationDegrees = -90f)
            newOnPlaneBall = OnPlaneBall(center = actualCueBallCenter, radius = initialLogicalRadius)
            newTableRotation = 0f
        } else {
            // Screen-centric defaults when table is not visible
            val viewCenterX = currentState.viewWidth / 2f
            val viewCenterY = currentState.viewHeight / 2f
            val targetBallCenter = PointF(viewCenterX, viewCenterY)
            val actualCueBallCenter = PointF(viewCenterX, viewCenterY + (currentState.viewHeight / 4f))

            newProtractorUnit = ProtractorUnit(center = targetBallCenter, radius = initialLogicalRadius, rotationDegrees = -90f)
            newOnPlaneBall = if (currentState.onPlaneBall != null) OnPlaneBall(center = actualCueBallCenter, radius = initialLogicalRadius) else null
            newTableRotation = 0f
        }

        // Reset only positional and rotational properties, preserving toggles
        return currentState.copy(
            protractorUnit = newProtractorUnit,
            onPlaneBall = newOnPlaneBall,
            obstacleBalls = emptyList(), // Clear obstacles on reset
            zoomSliderPosition = initialSliderPos,
            tableRotationDegrees = newTableRotation,
            bankingAimTarget = null,
            valuesChangedSinceReset = false,
            preResetState = stateToSave,
            hasCueBallBeenMoved = false,
            hasTargetBallBeenMoved = false
        )
    }
}