// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/splash/SplashState.kt
package com.hereliesaz.cuedetat.ui.composables.splash

data class SplashState(
    val isSaving: Boolean = false,
    val navigateToMain: Boolean = false
)

sealed interface SplashEvent {
    // If your mode is an Enum or Int, change the type. I am not clairvoyant.
    data class SelectMode(val mode: String) : SplashEvent
    data object NavigationConsumed : SplashEvent
}