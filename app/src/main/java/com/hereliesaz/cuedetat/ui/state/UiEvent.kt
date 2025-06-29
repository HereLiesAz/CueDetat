package com.hereliesaz.cuedetat.ui.state

import androidx.compose.ui.geometry.Offset
import androidx.xr.arcore.Anchor

/**
 * Defines all possible user interactions and system events that can modify the state.
 */
sealed class UiEvent {
    data class OnPlaneTap(val anchor: Anchor) : UiEvent()
    data class OnShotTypeSelect(val shotType: ShotType) : UiEvent()
    data class OnSpinChange(val offset: Offset) : UiEvent()
    data class OnElevationChange(val elevation: Float) : UiEvent()
    data class OnBallSelect(val ballId: String) : UiEvent()
    data object OnUndo : UiEvent()
    data object OnHelpToggle : UiEvent()
    data object OnDarkModeToggle : UiEvent()
}