// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetatlite.ui

sealed class MainScreenEvent {
    data class OnTouch(val x: Float, val y: Float) : MainScreenEvent()
    data class OnScale(val factor: Float) : MainScreenEvent()
    data class OnTableResize(val width: Int, val height: Int) : MainScreenEvent()
    data class OnForceLightMode(val isLightMode: Boolean) : MainScreenEvent()
    data class OnLuminanceChange(val value: Float) : MainScreenEvent()
    object OnUndo : MainScreenEvent()
    object OnRedo : MainScreenEvent()
    object OnJumpShot : MainScreenEvent()
    data class OnUpdate(val permissionGranted: Boolean) : MainScreenEvent()
    object OnUpdateDismissed : MainScreenEvent()
    object OnDownloadClicked : MainScreenEvent()
    object Reset : MainScreenEvent()
    object ToggleActualCueBall : MainScreenEvent()
}
