package com.hereliesaz.cuedetat

import androidx.lifecycle.ViewModel
import com.hereliesaz.cuedetat.view.model.LegacyOverlayState

// This is a legacy ViewModel, kept to ensure older parts of the app can be referenced or restored.
// It uses LegacyOverlayState to avoid name collision with the main app's modern OverlayState.
class OverlayViewModel : ViewModel() {
    val state = LegacyOverlayState()
}
