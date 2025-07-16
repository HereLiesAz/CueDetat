// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/SystemReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class SystemReducer @Inject constructor() {
    fun reduce(state: OverlayState, event: MainScreenEvent): OverlayState {
        return when(event) {
            is MainScreenEvent.SizeChanged -> state.copy(viewWidth = event.width, viewHeight = event.height)
            is MainScreenEvent.FullOrientationChanged -> state.copy(currentOrientation = event.orientation, pitchAngle = event.orientation.pitch)
            is MainScreenEvent.ThemeChanged -> state.copy(appControlColorScheme = event.scheme)
            else -> state
        }
    }
}