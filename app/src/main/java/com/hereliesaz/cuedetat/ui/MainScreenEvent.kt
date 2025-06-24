// app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.view.state.ToastMessage

sealed class MainScreenEvent {
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()

    // Protractor System Events
    data class RotationChanged(val newRotation: Float) : MainScreenEvent()
    data class UnitMoved(val position: PointF) : MainScreenEvent() // This is screen point

    // ActualCueBall / BankingBall Events
    data class ActualCueBallMoved(val position: PointF) : MainScreenEvent() // This is screen point

    // Banking System Events
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
    data class BankingAimTargetDragged(val screenPoint: PointF) : MainScreenEvent() // This is screen point, ViewModel will convert to logical

    internal data class UpdateLogicalActualCueBallPosition(val logicalPoint: PointF) : MainScreenEvent()
    internal data class UpdateLogicalUnitPosition(val logicalPoint: PointF) : MainScreenEvent()
    internal data class UpdateLogicalBankingAimTarget(val logicalPoint: PointF) : MainScreenEvent()

    data class FullOrientationChanged(val orientation: FullOrientation) : MainScreenEvent()

    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent()

    object ToggleForceTheme : MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()

    data class ShowToast(val message: ToastMessage) : MainScreenEvent()

    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()

    object Reset : MainScreenEvent()
    object ToggleHelp : MainScreenEvent()
    object ToggleActualCueBall : MainScreenEvent()
    object ToggleBankingMode : MainScreenEvent()

    data class ToggleSpatialLock(val isLocked: Boolean) : MainScreenEvent()

    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object FeatureComingSoon : MainScreenEvent()
    object ShowDonationOptions : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
    object GestureStarted : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
}