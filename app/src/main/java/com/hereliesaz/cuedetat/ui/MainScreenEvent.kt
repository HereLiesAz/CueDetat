// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme

sealed class MainScreenEvent {
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()

    // Protractor System Events
    data class RotationChanged(val newRotation: Float) : MainScreenEvent()
    data class UnitMoved(val position: PointF) : MainScreenEvent()

    // ActualCueBall / BankingBall Events
    data class ActualCueBallMoved(val position: PointF) : MainScreenEvent()

    // Banking System Events
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
    data class BankingAimTargetDragged(val screenPoint: PointF) : MainScreenEvent()

    internal data class UpdateLogicalActualCueBallPosition(val logicalPoint: PointF) : MainScreenEvent()
    internal data class UpdateLogicalUnitPosition(val logicalPoint: PointF) : MainScreenEvent()
    internal data class UpdateLogicalBankingAimTarget(val logicalPoint: PointF) : MainScreenEvent()

    data class PitchAngleChanged(val pitch: Float) : MainScreenEvent()
    // Add events for other sensor data (roll, yaw) if needed for locking
    // data class FullOrientationChanged(val pitch: Float, val roll: Float, val yaw: Float): MainScreenEvent()

    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent()

    object ToggleForceTheme : MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()

    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()

    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleActualCueBall : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()

    // New Spatial Lock Event
    object ToggleSpatialLock : MainScreenEvent()

    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object FeatureComingSoon : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
    object GestureStarted : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
}