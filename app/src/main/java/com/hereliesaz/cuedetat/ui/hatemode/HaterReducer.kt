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
                // The sensor roll translates to X-axis gravity, pitch to Y-axis
                val gravityX = -event.roll * 0.1f
                val gravityY = event.pitch * 0.1f
                state.copy(gravity = Offset(gravityX, gravityY))
            }

            is HaterEvent.DragTriangleStart -> state.copy(isUserDragging = true)
            is HaterEvent.DragTriangleEnd -> state.copy(
                isUserDragging = false,
                touchForce = Offset.Zero
            )
            is HaterEvent.DragTriangle -> {
                // When dragging, we apply the delta as a direct force
                state.copy(touchForce = event.delta)
            }
        }
    }
}