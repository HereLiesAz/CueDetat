// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/VisionReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class VisionReducer @Inject constructor(
    private val reducerUtils: ReducerUtils
) {
    fun reduce(event: MainScreenEvent.VisionDataUpdated, state: OverlayState): OverlayState {
        return state.copy(visionData = event.visionData)
            .let(reducerUtils::snapViolatingBalls)
    }
}