package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class ToastReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.ShowToast -> state.copy(toastMessage = event.message)
            is MainScreenEvent.SingleEventConsumed -> state.copy(toastMessage = null)
            else -> state
        }
    }
}