package com.hereliesaz.cuedetat.ui.hatemode

class HaterReducer {
    fun reduce(state: HaterState, event: HaterEvent): HaterState {
        return when (event) {
            is HaterEvent.ShowHater -> state.copy(isHaterVisible = true)
            is HaterEvent.HideHater -> state.copy(isHaterVisible = false)
            is HaterEvent.UpdateOrientation -> state.copy(orientation = event.orientation)
        }
    }
}