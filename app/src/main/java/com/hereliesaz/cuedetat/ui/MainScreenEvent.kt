// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme

sealed class MainScreenEvent {
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()
    data class RotationChanged(val newRotation: Float) : MainScreenEvent()
    data class PitchAngleChanged(val pitch: Float) : MainScreenEvent()
    data class UnitMoved(val position: PointF) : MainScreenEvent()
    data class ActualCueBallMoved(val position: PointF) : MainScreenEvent()
    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent()
    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleActualCueBall : MainScreenEvent()
    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object FeatureComingSoon : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
    object GestureStarted : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
}
