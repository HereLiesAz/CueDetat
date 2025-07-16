// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/ToastReducer.kt
package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class ToastReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.ShowToast -> state.copy(toastMessage = event.message)
            is MainScreenEvent.ToastShown -> state.copy(toastMessage = null)
            else -> state
        }
    }
}