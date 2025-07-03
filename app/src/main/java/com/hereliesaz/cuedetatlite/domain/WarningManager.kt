package com.hereliesaz.cuedetatlite.domain

import com.hereliesaz.cuedetatlite.view.state.ScreenState
import javax.inject.Inject

class WarningManager @Inject constructor() {
    fun getWarning(state: ScreenState): WarningText? {
        if (state.isImpossibleShot) {
            return WarningText.IMPOSSIBLE_SHOT
        }
        return null
    }
}
