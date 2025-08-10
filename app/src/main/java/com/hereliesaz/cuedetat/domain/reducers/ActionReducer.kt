package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.ProtractorUnit

internal fun reduceAction(
    state: CueDetatState,
    action: MainScreenEvent.Reset,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when (action) {

        else -> {
            state.preResetState?.let {
                return it.copy(
                    preResetState = null,
                    obstacleBalls = emptyList(),
                    isWorldLocked = false
                )
            }

            val stateToSave = state.copy()
            val initialSliderPos = 0f
            val targetBallCenter = reducerUtils.getDefaultTargetBallPosition()
            val cueBallCenter = reducerUtils.getDefaultCueBallPosition(state)

            val newProtractorUnit = ProtractorUnit(
                center = targetBallCenter,
                radius = LOGICAL_BALL_RADIUS,
                rotationDegrees = 0f
            )
            val newOnPlaneBall = if (state.onPlaneBall != null) {
                OnPlaneBall(center = cueBallCenter, radius = LOGICAL_BALL_RADIUS)
            } else {
                null
            }

            state.copy(
                protractorUnit = newProtractorUnit,
                onPlaneBall = newOnPlaneBall,
                obstacleBalls = emptyList(),
                zoomSliderPosition = initialSliderPos,
                worldRotationDegrees = 0f,
                bankingAimTarget = null,
                valuesChangedSinceReset = false,
                preResetState = stateToSave,
                hasCueBallBeenMoved = false,
                hasTargetBallBeenMoved = false,
                isWorldLocked = false,
                viewOffset = PointF(0f, 0f)
            )
        }
    }
}