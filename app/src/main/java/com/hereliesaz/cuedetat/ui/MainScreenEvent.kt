// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.view.state.TableSize

sealed class MainScreenEvent {
    // UI-Originated Events
    data class ScreenGestureStarted(val position: PointF) : MainScreenEvent()
    data class Drag(val previousPosition: PointF, val currentPosition: PointF) : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()
    data class TableRotationApplied(val degrees: Float) : MainScreenEvent() // For gestures (delta)
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()

    // Logical Events (dispatched by ViewModel)
    internal data class LogicalGestureStarted(val logicalPoint: PointF) : MainScreenEvent()
    internal data class LogicalDragApplied(val logicalDelta: PointF, val screenDelta: Offset) : MainScreenEvent()
    internal data class AimBankShot(val logicalTarget: PointF) : MainScreenEvent() // For direct bank aiming

    // Direct State Change Events
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent() // For the slider (absolute)
    data class FullOrientationChanged(val orientation: FullOrientation) : MainScreenEvent()
    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent()
    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleOnPlaneBall : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()
    object ToggleTable: MainScreenEvent()
    object CycleTableSize: MainScreenEvent()
    data class SetTableSize(val size: TableSize) : MainScreenEvent()
    object ToggleTableSizeDialog : MainScreenEvent()
    object ToggleForceTheme : MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()
    object ToggleDistanceUnit : MainScreenEvent()

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