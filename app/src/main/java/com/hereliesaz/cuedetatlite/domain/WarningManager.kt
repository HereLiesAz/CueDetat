// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/domain/WarningManager.kt
package com.hereliesaz.cuedetatlite.domain

import com.hereliesaz.cuedetatlite.view.state.ScreenState

class WarningManager {
    /**
     * Determines the appropriate warning text based on the current screen state.
     */
    fun getWarning(state: ScreenState): WarningText? {
        return when {
            state.isImpossibleShot -> WarningText.IMPOSSIBLE_SHOT
            // Add other conditions for different warnings here
            else -> null
        }
    }
}
