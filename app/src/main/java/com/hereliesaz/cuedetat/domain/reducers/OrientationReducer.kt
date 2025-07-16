// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/OrientationReducer.kt
package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import kotlin.math.abs

class OrientationReducer @Inject constructor() {
    private val PITCH_THRESHOLD_DEGREES = 5
    private val PITCH_MAX_DEGREES = 60

    fun reduce(event: MainScreenEvent.OrientationChanged, state: OverlayState): OverlayState {
        val newPitch = event.orientation.pitch.coerceIn(-PITCH_MAX_DEGREES.toFloat(), PITCH_MAX_DEGREES.toFloat())
        return if (abs(newPitch - state.pitchAngle) > 0.1) {
            state.copy(
                currentOrientation = event.orientation,
                pitchAngle = if (abs(newPitch) > PITCH_THRESHOLD_DEGREES) newPitch else 0f
            )
        } else {
            state
        }
    }
}