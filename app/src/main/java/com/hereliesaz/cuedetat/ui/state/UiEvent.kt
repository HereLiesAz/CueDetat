package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.xr.runtime.math.Pose

sealed class UiEvent {
    object OnTablePlaceTapped : UiEvent()
    data class OnPlaneTapped(val pose: Pose) : UiEvent()
    data class OnBallTapped(val ballId: Int) : UiEvent()
    data class SetShotType(val type: ShotType) : UiEvent()
    data class SetCueElevation(val elevation: Float) : UiEvent()
    data class SetSpin(val offset: Offset) : UiEvent()
    object OnReset : UiEvent()
    object ToggleHelpDialog : UiEvent()
}