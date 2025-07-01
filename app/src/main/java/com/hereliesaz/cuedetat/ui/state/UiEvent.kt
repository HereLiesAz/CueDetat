package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Session

// This is the complete and final set of events.
sealed class UiEvent {
    data class OnScreenTap(val offset: Offset) : UiEvent()
    object OnReset : UiEvent()
    data class SetShotPower(val power: Float) : UiEvent()
    data class SetSpin(val spin: Offset) : UiEvent()
    object ExecuteShot : UiEvent()
    object ToggleHelpDialog : UiEvent()
    object ToggleArMode : UiEvent()
    object ToggleFlashlight : UiEvent() // This is a signal to the Activity
    data class SetSession(val session: Session?) : UiEvent() // ViewModel needs the session
}