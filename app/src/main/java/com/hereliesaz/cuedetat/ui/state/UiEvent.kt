package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import com.google.ar.core.HitResult

sealed class UiEvent {
    // We get the HitResult directly from the ARScene tap listener
    data class OnHitResult(val hitResult: HitResult) : UiEvent()
    object OnReset : UiEvent()
    data class SetShotPower(val power: Float) : UiEvent()
    data class SetSpin(val spin: Offset) : UiEvent()
    object ExecuteShot : UiEvent()
    object ToggleHelpDialog : UiEvent()
    object ToggleArMode : UiEvent()
    object ToggleFlashlight : UiEvent() // This will now just be a signal
}