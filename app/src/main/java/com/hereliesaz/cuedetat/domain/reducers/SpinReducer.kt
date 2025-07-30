package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.MainScreenEvent

internal fun reduceSpinAction(state: CueDetatState, action: MainScreenEvent): CueDetatState {
    return when (action) {
        is MainScreenEvent.SpinApplied -> {
            state.copy(
                selectedSpinOffset = action.offset,
                valuesChangedSinceReset = true,
                lingeringSpinOffset = null,
                spinPaths = emptyMap(),
                spinPathsAlpha = 1.0f
            )
        }

        is MainScreenEvent.SpinSelectionEnded -> {
            state.copy(
                lingeringSpinOffset = state.selectedSpinOffset,
                selectedSpinOffset = null
            )
        }

        is MainScreenEvent.DragSpinControl -> {
            val currentCenter = state.spinControlCenter ?: return state
            val newCenter =
                PointF(currentCenter.x + action.delta.x, currentCenter.y + action.delta.y)
            state.copy(spinControlCenter = newCenter)
        }

        is MainScreenEvent.ClearSpinState -> {
            state.copy(
                lingeringSpinOffset = null,
                spinPaths = emptyMap()
            )
        }

        else -> state
    }
}