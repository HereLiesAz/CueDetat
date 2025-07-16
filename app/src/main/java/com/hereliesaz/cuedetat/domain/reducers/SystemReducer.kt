package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import javax.inject.Inject
import kotlin.math.abs

class SystemReducer @Inject constructor() {
    private val PITCH_THRESHOLD_DEGREES = 5
    private val PITCH_MAX_DEGREES = 60
    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        return when (event) {
            is MainScreenEvent.FullOrientationChanged -> {
                val newPitch = event.orientation.pitch.coerceIn(-PITCH_MAX_DEGREES.toFloat(), PITCH_MAX_DEGREES.toFloat())
                if (abs(newPitch - state.pitchAngle) > 0.1) {
                    state.copy(currentOrientation = event.orientation, pitchAngle = if (abs(newPitch) > PITCH_THRESHOLD_DEGREES) newPitch else 0f)
                } else {
                    state
                }
            }
            is MainScreenEvent.ThemeChanged -> state.copy(appControlColorScheme = event.scheme)
            else -> state
        }
    }
}