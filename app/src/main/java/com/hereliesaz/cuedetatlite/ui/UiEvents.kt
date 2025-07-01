// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/ui/UiEvent.kt
package com.hereliesaz.cuedetatlite.ui

/**
 * Defines one-off events sent from the ViewModel to the UI, which should only be handled once.
 */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class OpenUrl(val url: String) : UiEvent()
}
