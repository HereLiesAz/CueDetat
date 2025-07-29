package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState

internal fun reduceSpinAction(state: CueDetatState, action: CueDetatAction): CueDetatState {
    return when (action) {
        is CueDetatAction.SpinApplied -> {
            state.copy(
                selectedSpinOffset = action.offset,
                valuesChangedSinceReset = true,
                lingeringSpinOffset = null,
                spinPaths = emptyMap(),
                spinPathsAlpha = 1.0f
            )
        }

        is CueDetatAction.SpinSelectionEnded -> {
            state.copy(
                lingeringSpinOffset = state.selectedSpinOffset,
                selectedSpinOffset = null
            )
        }

        is CueDetatAction.DragSpinControl -> {
            val currentCenter = state.spinControlCenter ?: return state
            val newCenter =
                PointF(currentCenter.x + action.delta.x, currentCenter.y + action.delta.y)
            state.copy(spinControlCenter = newCenter)
        }

        is CueDetatAction.ClearSpinState -> {
            state.copy(
                lingeringSpinOffset = null,
                spinPaths = emptyMap()
            )
        }

        else -> state
    }
}