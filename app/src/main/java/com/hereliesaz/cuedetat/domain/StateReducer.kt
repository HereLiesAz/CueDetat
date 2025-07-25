// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt

package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.reducers.ActionReducer
import com.hereliesaz.cuedetat.domain.reducers.AdvancedOptionsReducer
import com.hereliesaz.cuedetat.domain.reducers.ControlReducer
import com.hereliesaz.cuedetat.domain.reducers.CvReducer
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.domain.reducers.ObstacleReducer
import com.hereliesaz.cuedetat.domain.reducers.SnapReducer
import com.hereliesaz.cuedetat.domain.reducers.SpinReducer
import com.hereliesaz.cuedetat.domain.reducers.SystemReducer
import com.hereliesaz.cuedetat.domain.reducers.ToggleReducer
import com.hereliesaz.cuedetat.domain.reducers.TutorialReducer
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateReducer @Inject constructor(
    private val gestureReducer: GestureReducer,
    private val toggleReducer: ToggleReducer,
    private val controlReducer: ControlReducer,
    private val systemReducer: SystemReducer,
    private val actionReducer: ActionReducer,
    private val tutorialReducer: TutorialReducer,
    private val spinReducer: SpinReducer,
    private val obstacleReducer: ObstacleReducer,
    private val cvReducer: CvReducer,
    private val advancedOptionsReducer: AdvancedOptionsReducer,
    private val snapReducer: SnapReducer
) {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        return when (event) {
            is MainScreenEvent.LogicalGestureStarted,
            is MainScreenEvent.LogicalDragApplied,
            is MainScreenEvent.GestureEnded ->
                gestureReducer.reduce(currentState, event)

            is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.CycleTableSize,
            is MainScreenEvent.SetTableSize,
            is MainScreenEvent.ToggleTableSizeDialog,
            is MainScreenEvent.ToggleForceTheme,
            is MainScreenEvent.ToggleCamera,
            is MainScreenEvent.ToggleDistanceUnit,
            is MainScreenEvent.ToggleLuminanceDialog,
            is MainScreenEvent.ToggleGlowStickDialog,
            is MainScreenEvent.ToggleHelp,
            is MainScreenEvent.ToggleMoreHelp,
            is MainScreenEvent.ToggleSpinControl,
            is MainScreenEvent.ToggleSnapping,
            is MainScreenEvent.ToggleOrientationLock,
            is MainScreenEvent.ApplyPendingOrientationLock,
            is MainScreenEvent.ToggleExperienceMode,
            is MainScreenEvent.ApplyPendingExperienceMode,
            is MainScreenEvent.SetExperienceMode,
            is MainScreenEvent.UnlockBeginnerView,
            is MainScreenEvent.ToggleCalibrationScreen,
            is MainScreenEvent.ToggleQuickAlignScreen,
            is MainScreenEvent.ToggleCvModel ->
                toggleReducer.reduce(currentState, event)

            is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.ZoomScaleChanged,
            is MainScreenEvent.TableRotationApplied,
            is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.AdjustGlow,
            is MainScreenEvent.PanView,
            is MainScreenEvent.AdjustLuminance ->
                controlReducer.reduce(currentState, event)

            is MainScreenEvent.SizeChanged,
            is MainScreenEvent.FullOrientationChanged,
            is MainScreenEvent.ThemeChanged,
            is MainScreenEvent.SetWarning ->
                systemReducer.reduce(currentState, event)

            is MainScreenEvent.Reset ->
                actionReducer.reduce(currentState, event)

            is MainScreenEvent.StartTutorial,
            is MainScreenEvent.NextTutorialStep,
            is MainScreenEvent.EndTutorial ->
                tutorialReducer.reduce(currentState, event)

            is MainScreenEvent.SpinApplied,
            is MainScreenEvent.SpinSelectionEnded,
            is MainScreenEvent.DragSpinControl,
            is MainScreenEvent.ClearSpinState ->
                spinReducer.reduce(currentState, event)

            is MainScreenEvent.AddObstacleBall ->
                obstacleReducer.reduce(currentState, event)

            is MainScreenEvent.CvDataUpdated,
            is MainScreenEvent.LockOrUnlockColor,
            is MainScreenEvent.LockColor,
            is MainScreenEvent.ClearSamplePoint -> {
                cvReducer.reduce(currentState, event)
            }

            is MainScreenEvent.ToggleAdvancedOptionsDialog,
            is MainScreenEvent.ToggleCvRefinementMethod,
            is MainScreenEvent.ToggleCvMask,
            is MainScreenEvent.EnterCvMaskTestMode,
            is MainScreenEvent.ExitCvMaskTestMode,
            is MainScreenEvent.EnterCalibrationMode,
            is MainScreenEvent.SampleColorAt,
            is MainScreenEvent.UpdateHoughP1,
            is MainScreenEvent.UpdateHoughP2,
            is MainScreenEvent.UpdateCannyT1,
            is MainScreenEvent.UpdateCannyT2 ->
                advancedOptionsReducer.reduce(currentState, event)

            is MainScreenEvent.SendFeedback, is MainScreenEvent.MenuClosed -> currentState // No state change, only a side effect

            else -> currentState
        }
    }
}