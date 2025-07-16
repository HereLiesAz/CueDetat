package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.UserPreferences
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.model.TableSize
import com.hereliesaz.cuedetat.view.state.ToastMessage

sealed interface MainScreenEvent {
    // Gestures & Interactions
    data class Drag(val position: PointF, val isLongPress: Boolean = false) : MainScreenEvent
    data class Release(val position: PointF) : MainScreenEvent
    data class SpinDrag(val offset: Offset) : MainScreenEvent
    object SpinDragEnd : MainScreenEvent
    object ScreenGestureStarted : MainScreenEvent
    object GestureEnded : MainScreenEvent

    // System & Sensor Events
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent
    data class FullOrientationChanged(val orientation: FullOrientation) : MainScreenEvent
    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent
    data class LoadUserSettings(val prefs: UserPreferences) : MainScreenEvent

    // UI State Toggles
    object ToggleCamera : MainScreenEvent
    object ToggleTable : MainScreenEvent
    object ToggleOnPlaneBall : MainScreenEvent
    object ToggleBankingMode : MainScreenEvent
    object ToggleHelp : MainScreenEvent
    object ToggleCvParamMenu : MainScreenEvent
    object ToggleForceTheme : MainScreenEvent
    object ToggleDistanceUnit : MainScreenEvent
    object ToggleSpinControl : MainScreenEvent
    object ToggleLuminanceDialog : MainScreenEvent
    object ToggleGlowStickDialog : MainScreenEvent
    object ToggleTableSizeDialog : MainScreenEvent
    object ToggleAdvancedOptionsDialog : MainScreenEvent

    // Control Adjustments
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent
    data class ZoomChanged(val position: Float) : MainScreenEvent
    data class AdjustLuminance(val value: Float) : MainScreenEvent
    data class AdjustGlow(val value: Float) : MainScreenEvent
    data class SetTableSize(val size: TableSize) : MainScreenEvent
    object CycleTableSize : MainScreenEvent

    // Action Events
    object Reset : MainScreenEvent
    data class AimBankShot(val logicalTarget: PointF) : MainScreenEvent
    object AddObstacle : MainScreenEvent
    object ClearObstacles : MainScreenEvent
    object CheckForUpdate : MainScreenEvent
    object ViewArt : MainScreenEvent
    object LockOrUnlockColor : MainScreenEvent
    object ClearSpinState : MainScreenEvent

    // CV & Vision Events
    data class CvDataUpdated(val visionData: VisionData) : MainScreenEvent
    data class SnapToDetectedBall(val ball: PointF) : MainScreenEvent
    object ToggleAdvancedOptions : MainScreenEvent
    object ToggleSnapping : MainScreenEvent
    object ToggleCvModel : MainScreenEvent
    object ToggleCvRefinementMethod : MainScreenEvent
    data class UpdateHoughP1(val value: Float) : MainScreenEvent
    data class UpdateHoughP2(val value: Float) : MainScreenEvent
    data class UpdateCannyT1(val value: Float) : MainScreenEvent
    data class UpdateCannyT2(val value: Float) : MainScreenEvent

    // Tutorial Events
    object StartTutorial : MainScreenEvent
    object NextTutorialStep : MainScreenEvent
    object FinishTutorial : MainScreenEvent

    // Single Events (Toast, Navigation etc.)
    data class ShowToast(val message: ToastMessage) : MainScreenEvent
    object SingleEventConsumed : MainScreenEvent // Renamed from ToastShown for clarity
}