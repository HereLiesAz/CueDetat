package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
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
    return when {
        // Stage 1 — CLEAR: obstacle balls present → remove only obstacles
        state.obstacleBalls.isNotEmpty() -> {
            state.copy(obstacleBalls = emptyList())
        }

        // Stage 1.5 — UNDO-TARGET: target was CV-selected → un-select it, return to AWAITING_TARGET
        state.obstacleBalls.isEmpty() && state.targetCvAnchor != null -> {
            state.copy(
                protractorUnit = state.protractorUnit.copy(
                    center = reducerUtils.getDefaultTargetBallPosition()
                ),
                targetCvAnchor = null,
                ballSelectionPhase = BallSelectionPhase.AWAITING_TARGET
            )
        }

        // Stage 2 — UNDO: a pre-reset state exists → restore it, stash current into postResetState
        state.preResetState != null -> {
            state.preResetState.copy(
                preResetState = null,
                postResetState = state.copy(preResetState = null, postResetState = null),
                isWorldLocked = false
            )
        }

        // Stage 3 — REDO: a post-reset state exists → restore it, stash current into preResetState
        state.postResetState != null -> {
            state.postResetState.copy(
                preResetState = state.copy(preResetState = null, postResetState = null),
                postResetState = null,
                isWorldLocked = false
            )
        }

        // Stage 4 — RESET: no obstacles, no saved states → full reset, save current to preResetState
        else -> {
            val stateToSave = state.copy()
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
                zoomSliderPosition = 0f,
                worldRotationDegrees = 0f,
                bankingAimTarget = null,
                valuesChangedSinceReset = false,
                preResetState = stateToSave,
                postResetState = null,
                hasCueBallBeenMoved = false,
                hasTargetBallBeenMoved = false,
                isWorldLocked = false,
                viewOffset = PointF(0f, 0f),
                tableZOffset = 0f,
                cueBallCvAnchor = null,
                targetCvAnchor = null,
                obstacleCvAnchors = emptyList(),
                ballSelectionPhase = if (state.tableScanModel != null && state.experienceMode == ExperienceMode.EXPERT)
                    BallSelectionPhase.AWAITING_CUE else BallSelectionPhase.NONE
            )
        }
    }
}
