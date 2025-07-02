package com.hereliesaz.cuedetatlite.ui

import android.graphics.PointF
import com.hereliesaz.cuedetatlite.data.FullOrientation

sealed class MainScreenEvent {
    data class ViewResized(val width: Int, val height: Int) : MainScreenEvent()
    data class OrientationChanged(val orientation: FullOrientation) : MainScreenEvent()
    data class BallMoved(val ballId: Int, val position: PointF) : MainScreenEvent()
    data class BallRadiusChanged(val ballId: Int, val radius: Float) : MainScreenEvent()
    data class ZoomChanged(val zoomFactor: Float) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class AimingAngleChanged(val degrees: Float) : MainScreenEvent()
    object Reset : MainScreenEvent()
    object Redo : MainScreenEvent()
    object JumpShot : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleMoreHelp : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()
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
    object ToggleActualCueBall : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
}