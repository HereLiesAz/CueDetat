package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject

class ActionReducer @Inject constructor() {
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.Reset -> OverlayState()
            is MainScreenEvent.ClearObstacles -> state.copy(obstacleBalls = emptyList())
            is MainScreenEvent.AimBankShot -> {
                if (state.onPlaneBall == null) {
                    state.copy(toastMessage = "Can't bank without a cue ball")
                } else {
                    state.copy(bankingAimTarget = event.logicalTarget)
                }
            }
            else -> state
        }
    }
}