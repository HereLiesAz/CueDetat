// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SpinReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class SpinReducer @Inject constructor() {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.SpinDrag -> state.copy(selectedSpinOffset = event.offset)
            is MainScreenEvent.SpinDragEnd -> state.copy(
                selectedSpinOffset = null,
                lingeringSpinOffset = event.offset
            )
            is MainScreenEvent.ClearSpinState -> state.copy(lingeringSpinOffset = null)
            else -> state
        }
    }
}