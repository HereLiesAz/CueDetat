// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.data.FullOrientation

sealed class MainScreenEvent {
    // UI-Originated Events, using screen-space coordinates/deltas
    data class ScreenGestureStarted(val position: PointF) : MainScreenEvent()
    data class Drag(val dragAmount: Offset) : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()

    // Logical Events (dispatched by ViewModel after processing UI events)
    internal data class LogicalGestureStarted(val logicalPoint: PointF) : MainScreenEvent()
    internal data class LogicalDragApplied(val logicalDelta: PointF) : MainScreenEvent()

    // Direct State Change Events
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
    data class FullOrientationChanged(val orientation: FullOrientation) : MainScreenEvent()
    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent()
    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleActualCueBall : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()
    object ToggleForceTheme : MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()

    // Tutorial Events
    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()

    // Meta/Single Events
    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
}