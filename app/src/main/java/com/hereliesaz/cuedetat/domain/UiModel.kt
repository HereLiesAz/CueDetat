package com.hereliesaz.cuedetat.domain

import androidx.glance.action.Action
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel

// Represents the different modes the application can be in.
enum class ExperienceMode {
    NORMAL,
    HATER
}

// Defines the state of any overlay that might be shown over the main content.
sealed class OverlayState {
    // No overlay is visible.
    object None : OverlayState()

    // The experience mode selection overlay is visible.
    data class ExperienceModeSelection(val currentMode: ExperienceMode) : OverlayState()
}

// A sealed class for all possible actions/events that can be dispatched to the reducer.
sealed class CueDetatAction {
    // Action to toggle the visibility of the experience mode selection overlay.
    object ToggleExperienceMode : CueDetatAction()

    // Action to apply a newly selected experience mode.
    data class ApplyPendingExperienceMode(val mode: ExperienceMode) : CueDetatAction()

    // Actions specific to the Hater mode.
    data class HaterAction(val action: HaterViewModel.Action) : CueDetatAction()
}