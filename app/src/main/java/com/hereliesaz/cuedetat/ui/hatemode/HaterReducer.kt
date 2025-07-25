package com.hereliesaz.cuedetat.ui.hatemode

import androidx.compose.ui.geometry.Offset

class HaterReducer {
    fun reduce(state: HaterState, event: HaterEvent): HaterState {
        return when (event) {
            is HaterEvent.ShowHater -> state.copy(isHaterVisible = true)
            is HaterEvent.HideHater -> state.copy(isHaterVisible = false)
            is HaterEvent.UpdateSensorOffset -> {
                val offsetX = -event.roll * 4.0f
                val offsetY = event.pitch * 4.0f
                state.copy(gravityTargetOffset = Offset(offsetX, offsetY))
            }

            is HaterEvent.DragTriangleStart -> state.copy(isUserDragging = true)
            is HaterEvent.DragTriangle -> {
                // Apply a damping factor to simulate resistance/pushing
                state.copy(touchDrivenOffset = state.touchDrivenOffset + (event.delta * 0.5f))
            }

            is HaterEvent.DragTriangleEnd -> state.copy(
                isUserDragging = false,
                touchDrivenOffset = Offset.Zero // Reset touch offset on release
            )
        }
    }
}