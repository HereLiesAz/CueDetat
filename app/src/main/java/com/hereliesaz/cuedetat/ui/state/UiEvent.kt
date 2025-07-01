package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import com.google.ar.core.HitResult

sealed class UiEvent {
    data class OnTap(val hitResult: HitResult) : UiEvent()
    object OnReset : UiEvent()
    object ToggleDrawer : UiEvent()
    data class SetShotPower(val power: Float) : UiEvent()
    data class SetSpin(val spin: Offset) : UiEvent()
    object ExecuteShot : UiEvent()
}