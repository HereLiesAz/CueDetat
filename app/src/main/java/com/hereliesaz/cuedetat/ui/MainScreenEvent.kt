package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.UserPreferences
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.model.TableSize

sealed interface MainScreenEvent {
    data class Drag(val position: PointF, val isLongPress: Boolean = false) : MainScreenEvent
    data class Release(val position: PointF) : MainScreenEvent
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent
    data class ZoomChanged(val position: Float) : MainScreenEvent
    data class FullOrientationChanged(val orientation: FullOrientation) : MainScreenEvent
    data class AimBankShot(val logicalTarget: PointF) : MainScreenEvent
    data class CvDataUpdated(val visionData: VisionData) : MainScreenEvent
    data class ShowToast(val message: String) : MainScreenEvent
    data class SpinDrag(val offset: Offset) : MainScreenEvent
    data class ThemeChanged(val scheme: ColorScheme) : MainScreenEvent
    data class UpdateHoughP1(val value: Float) : MainScreenEvent
    data class UpdateHoughP2(val value: Float) : MainScreenEvent
    data class UpdateCannyT1(val value: Float) : MainScreenEvent
    data class UpdateCannyT2(val value: Float) : MainScreenEvent
    data class AdjustLuminance(val value: Float) : MainScreenEvent
    data class AdjustGlow(val value: Float) : MainScreenEvent
    data class SnapToDetectedBall(val ball: PointF) : MainScreenEvent
    data class SetTableSize(val size: TableSize) : MainScreenEvent
    data class LoadUserSettings(val prefs: UserPreferences) : MainScreenEvent
    object Reset : MainScreenEvent
    object ToggleCamera : MainScreenEvent
    object ToggleTable : MainScreenEvent
    object ToggleOnPlaneBall : MainScreenEvent
    object ToggleBankingMode : MainScreenEvent
    object ToggleHelp : MainScreenEvent
    object ToggleCvParamMenu : MainScreenEvent
    object ToggleForceTheme : MainScreenEvent
    object ToggleDistanceUnit : MainScreenEvent
    object CheckForUpdate : MainScreenEvent
    object ViewArt : MainScreenEvent
    object AddObstacle : MainScreenEvent
    object ClearObstacles : MainScreenEvent
    object ScreenGestureStarted : MainScreenEvent
    object GestureEnded : MainScreenEvent
    object SpinDragEnd : MainScreenEvent
    object SingleEventConsumed : MainScreenEvent
    object LockOrUnlockColor : MainScreenEvent
    object ToggleAdvancedOptions : MainScreenEvent
    object ToggleSnapping : MainScreenEvent
    object ToggleCvModel : MainScreenEvent
    object ToggleCvRefinementMethod : MainScreenEvent
}