// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/hatemode/HaterReducer.kt

package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

class HaterReducer {
    fun reduce(state: HaterState, event: HaterEvent): HaterState {
        return when (event) {
            is HaterEvent.EnterHaterMode -> state.copy(
                isFirstReveal = true,
                recentlyUsedAnswers = emptyList(),
                currentAnswer = null,
                isHaterVisible = false
            )
            is HaterEvent.ShowHater -> state.copy(isHaterVisible = true)
            is HaterEvent.HideHater -> state.copy(isHaterVisible = false)
            is HaterEvent.UpdateSensorOffset -> {
                // This event is now handled directly by the ViewModel to update the physics world's gravity.
                // No state change is needed here.
                state
            }
            is HaterEvent.DragTriangleStart -> state.copy(isUserDragging = true)
            is HaterEvent.DragTriangleEnd -> state.copy(isUserDragging = false, dragDelta = Offset.Zero)
            is HaterEvent.DragTriangle -> {
                // Simply record the delta for the ViewModel to process.
                state.copy(dragDelta = event.delta)
            }
        }
    }
}