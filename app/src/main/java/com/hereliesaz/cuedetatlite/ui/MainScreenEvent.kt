package com.hereliesaz.cuedetatlite.ui

import android.graphics.PointF

sealed class MainScreenEvent {
    data class BallMoved(val ballId: Int, val position: PointF) : MainScreenEvent()
    data class BallRadiusChanged(val ballId: Int, val radius: Float) : MainScreenEvent()
    data class ZoomChanged(val zoomFactor: Float) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    object Reset : MainScreenEvent()
    object Redo : MainScreenEvent()
    object JumpShot : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
    data class BankingAimTargetChanged(val position: PointF) : MainScreenEvent()
    data class ForceLightMode(val enabled: Boolean?) : MainScreenEvent()
    data class LuminanceChanged(val value: Float) : MainScreenEvent()
    object ShowLuminanceDialog : MainScreenEvent()
    object DismissLuminanceDialog : MainScreenEvent()
    object StartTutorial : MainScreenEvent()
    data class TutorialNext(val currentStep: Int) : MainScreenEvent()
    object TutorialPrevious : MainScreenEvent()
    object EndTutorial : MainScreenEvent()
    object ToggleSpatialLock : MainScreenEvent()
    object DismissUpdateDialog : MainScreenEvent()
    object DownloadUpdate : MainScreenEvent()
    // ADDED a toggle for the aiming ball, which was missing.
    object ToggleActualCueBall : MainScreenEvent()
    // ADDED events for meta menu items
    object ViewArt : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    object CheckForUpdate : MainScreenEvent()
}