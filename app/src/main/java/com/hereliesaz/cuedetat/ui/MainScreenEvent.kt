// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.UserPreferences
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.model.TableSize
import com.hereliesaz.cuedetat.view.state.ToastMessage

sealed interface MainScreenEvent {
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent
    data class OrientationChanged(val orientation: FullOrientation) : MainScreenEvent
    data class Drag(val position: PointF, val isLongPress: Boolean = false) : MainScreenEvent
    data class Release(val position: PointF) : MainScreenEvent
    data class VisionDataUpdated(val visionData: VisionData) : MainScreenEvent
    data class UpdateZoom(val zoom: Float) : MainScreenEvent
    data class ShowToast(val message: ToastMessage) : MainScreenEvent
    data class UpdateTableRotation(val rotation: Float) : MainScreenEvent
    data class CycleTableSize(val forward: Boolean = true) : MainScreenEvent
    data class SetTableSize(val size: TableSize) : MainScreenEvent
    data class UpdateBankingAim(val position: PointF) : MainScreenEvent
    data class UpdateColorScheme(val colorScheme: ColorScheme) : MainScreenEvent // ADDED
    data class LoadUserSettings(val prefs: UserPreferences) : MainScreenEvent
    data object Reset : MainScreenEvent
    data object ToggleTable : MainScreenEvent
    data object ToggleOnPlaneBall : MainScreenEvent
    data object ToggleCamera : MainScreenEvent
    data object SwitchCamera : MainScreenEvent
    data object ToggleBankingMode : MainScreenEvent
    data object ToggleLuminanceDialog : MainScreenEvent
    data object ToggleGlowStickDialog : MainScreenEvent
    data object ToggleTableSizeDialog : MainScreenEvent
    data object ToggleAdvancedOptionsDialog : MainScreenEvent
    data object ToggleSpinControl : MainScreenEvent
    data object ClearSpinState : MainScreenEvent
    data object ToastShown : MainScreenEvent
    data object AddObstacle : MainScreenEvent
    data object FinishTutorial : MainScreenEvent
    data object NextTutorialStep : MainScreenEvent
    data object StartTutorial : MainScreenEvent
    data object ToggleForceLightMode : MainScreenEvent
    data object ToggleHelpers : MainScreenEvent
}