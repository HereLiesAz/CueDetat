package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.xr.runtime.Session
import androidx.xr.arcore.HitResult

sealed class UiEvent {
    // This event is triggered by the UI layer after it performs a hit test.
    data class OnHitResult(val hitResult: HitResult) : UiEvent()
    object OnReset : UiEvent()
    data class SetShotPower(val power: Float) : UiEvent()
    data class SetSpin(val spin: Offset) : UiEvent()
    object ExecuteShot : UiEvent()
    data class SetSession(val session: Session?) : UiEvent()
    object ToggleHelp : UiEvent()
}
