package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme

sealed class MainScreenEvent {
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()

    // Protractor System Events
    data class RotationChanged(val newRotation: Float) : MainScreenEvent() // For ProtractorUnit
    data class UnitMoved(val position: PointF) :
        MainScreenEvent()      // For ProtractorUnit (SCREEN Coords)

    // ActualCueBall / BankingBall Events
    data class ActualCueBallMoved(val position: PointF) :
        MainScreenEvent() // For user-dragged ActualCueBall / BankingBall (SCREEN Coords)

    // Banking System Events
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
    data class BankingAimTargetDragged(val screenPoint: PointF) :
        MainScreenEvent() // (SCREEN Coords)

    // Internal events for reducer after screen->logical conversion
    internal data class UpdateLogicalActualCueBallPosition(val logicalPoint: PointF) :
        MainScreenEvent()

    internal data class UpdateLogicalUnitPosition(val logicalPoint: PointF) : MainScreenEvent()
    internal data class UpdateLogicalBankingAimTarget(val logicalPoint: PointF) : MainScreenEvent()

    data class PitchAngleChanged(val pitch: Float) : MainScreenEvent()
    data class ThemeChanged(val scheme: ColorScheme) :
        MainScreenEvent() // When MaterialTheme provides its scheme FOR APP CONTROLS

    // New Theme/Appearance Events for drawn elements
    object ToggleForceTheme : MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()

    // Tutorial Events
    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent() // Could add PreviousTutorialStep
    object EndTutorial : MainScreenEvent()

    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleActualCueBall :
        MainScreenEvent() // Toggles the optional ActualCueBall in protractor mode
    object ToggleBankingMode : MainScreenEvent()
    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object FeatureComingSoon : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
    object GestureStarted : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
}