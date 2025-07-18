// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainScreenEvent.kt

package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.VisionData
import com.hereliesaz.cuedetat.view.state.TableSize

sealed class MainScreenEvent {
    // UI-Originated Events
    data class ScreenGestureStarted(val position: PointF) : MainScreenEvent()
    data class Drag(val previousPosition: PointF, val currentPosition: PointF) : MainScreenEvent()
    object GestureEnded : MainScreenEvent()
    data class SizeChanged(val width: Int, val height: Int) : MainScreenEvent()
    data class ZoomScaleChanged(val scaleFactor: Float) : MainScreenEvent()
    data class TableRotationApplied(val degrees: Float) : MainScreenEvent()
    data class ZoomSliderChanged(val position: Float) : MainScreenEvent()
    data class PanView(val delta: PointF) : MainScreenEvent()

    // Spin Control Events
    object ToggleSpinControl : MainScreenEvent()
    data class SpinApplied(val offset: PointF) : MainScreenEvent()
    object SpinSelectionEnded : MainScreenEvent()
    data class DragSpinControl(val delta: PointF): MainScreenEvent()
    object ClearSpinState : MainScreenEvent()

    // Logical Events (dispatched by ViewModel)
    internal data class LogicalGestureStarted(val logicalPoint: PointF, val screenOffset: Offset) : MainScreenEvent()
    internal data class LogicalDragApplied(val previousLogicalPoint: PointF, val currentLogicalPoint: PointF, val screenDelta: Offset) : MainScreenEvent()
    internal data class AimBankShot(val logicalTarget: PointF) : MainScreenEvent()

    // Direct State Change Events
    data class TableRotationChanged(val degrees: Float) : MainScreenEvent()
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
    object ToggleCamera: MainScreenEvent()
    object ToggleLuminanceDialog : MainScreenEvent()
    data class AdjustLuminance(val adjustment: Float) : MainScreenEvent()
    object ToggleDistanceUnit : MainScreenEvent()
    object ToggleGlowStickDialog : MainScreenEvent()
    data class AdjustGlow(val value: Float) : MainScreenEvent()

    // Obstacle Events
    object AddObstacleBall : MainScreenEvent()

    // CV Events
    data class CvDataUpdated(val data: VisionData) : MainScreenEvent()
    object LockOrUnlockColor : MainScreenEvent()
    data class LockColor(val hsv: FloatArray) : MainScreenEvent() // To lock a specific color
    object ClearSamplePoint : MainScreenEvent() // To reset after sampling
    object ToggleAdvancedOptionsDialog : MainScreenEvent()
    object ToggleCvRefinementMethod : MainScreenEvent()
    data class UpdateHoughP1(val value: Float) : MainScreenEvent()
    data class UpdateHoughP2(val value: Float) : MainScreenEvent()
    data class UpdateCannyT1(val value: Float) : MainScreenEvent()
    data class UpdateCannyT2(val value: Float) : MainScreenEvent()
    object ToggleCvModel : MainScreenEvent()
    object ToggleSnapping: MainScreenEvent()
    object ToggleCvMask : MainScreenEvent()
    object EnterCvMaskTestMode : MainScreenEvent()
    object ExitCvMaskTestMode : MainScreenEvent()
    object EnterCalibrationMode : MainScreenEvent()
    data class SampleColorAt(val screenPosition: Offset) : MainScreenEvent()


    // Tutorial Events
    object StartTutorial : MainScreenEvent()
    object NextTutorialStep : MainScreenEvent()
    object EndTutorial : MainScreenEvent()

    // Meta/Single Events
    object CheckForUpdate : MainScreenEvent()
    object ViewArt : MainScreenEvent()
    object SingleEventConsumed : MainScreenEvent()
    object ToastShown : MainScreenEvent()
}