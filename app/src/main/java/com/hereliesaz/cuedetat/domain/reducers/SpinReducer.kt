package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class SpinReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.SpinDrag -> state.copy(
                selectedSpinOffset = event.offset,
                lingeringSpinOffset = event.offset
            )
            is MainScreenEvent.SpinDragEnd -> state.copy(
                selectedSpinOffset = null
            )
            else -> state
        }
    }
}