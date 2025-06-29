package com.hereliesaz.cuedetat.ui.state

import com.google.ar.core.HitResult

sealed interface UiEvent {
    data class OnPlaneTap(val hitResult: HitResult) : UiEvent
    data object OnReset : UiEvent
}
