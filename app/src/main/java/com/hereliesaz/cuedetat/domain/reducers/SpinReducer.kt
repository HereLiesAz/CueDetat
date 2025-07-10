// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SpinReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import android.graphics.PointF
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpinReducer @Inject constructor() {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.SpinApplied -> {
                currentState.copy(
                    selectedSpinOffset = event.offset,
                    valuesChangedSinceReset = true,
                    // When a new spin is applied, clear the old linger
                    lingeringSpinOffset = null,
                    spinPaths = emptyMap(),
                    spinPathsAlpha = 1.0f
                )
            }
            is MainScreenEvent.SpinSelectionEnded -> {
                // Latch the last known offset for the linger effect and clear the "live" one
                currentState.copy(
                    lingeringSpinOffset = currentState.selectedSpinOffset,
                    selectedSpinOffset = null
                )
            }
            is MainScreenEvent.DragSpinControl -> {
                val currentCenter = currentState.spinControlCenter ?: return currentState
                // THE FIX: Create a new PointF to ensure immutability.
                val newCenter = PointF(currentCenter.x + event.delta.x, currentCenter.y + event.delta.y)
                currentState.copy(spinControlCenter = newCenter)
            }
            is MainScreenEvent.ClearSpinState -> {
                currentState.copy(
                    lingeringSpinOffset = null,
                    spinPaths = emptyMap()
                )
            }
            else -> currentState
        }
    }
}