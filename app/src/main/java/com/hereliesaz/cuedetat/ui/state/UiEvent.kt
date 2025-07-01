package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Session

sealed class UiEvent {
    data class OnScreenTap(val offset: Offset) : UiEvent()
    object OnReset : UiEvent()
    data class SetShotPower(val power: Float) : UiEvent()
    data class SetSpin(val spin: Offset) : UiEvent()
    object ExecuteShot : UiEvent()
    data class SetSession(val session: Session?) : UiEvent()
    object ToggleHelpDialog : UiEvent()
}