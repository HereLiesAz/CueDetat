package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.domain.reducers.reduceAction
import com.hereliesaz.cuedetat.domain.reducers.reduceAdvancedOptionsAction
import com.hereliesaz.cuedetat.domain.reducers.reduceControlAction
import com.hereliesaz.cuedetat.domain.reducers.reduceCvAction
import com.hereliesaz.cuedetat.domain.reducers.reduceObstacleAction
import com.hereliesaz.cuedetat.domain.reducers.reduceSpinAction
import com.hereliesaz.cuedetat.domain.reducers.reduceSystemAction
import com.hereliesaz.cuedetat.domain.reducers.reduceToggleAction
import com.hereliesaz.cuedetat.domain.reducers.reduceTutorialAction

/**
 * The single, unified reducer for the entire application state. It
 * delegates to specialized, private functions for organization.
 */
fun stateReducer(
    currentState: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils,
    gestureReducer: GestureReducer
): CueDetatState {
    return when (action) {
        // Gesture events are handled by the dedicated GestureReducer
        is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied, is MainScreenEvent.GestureEnded -> {
            gestureReducer.reduce(currentState, action)
        }
        // All other actions are handled by the sub-reducers
        is MainScreenEvent.Reset -> reduceAction(currentState, action, reducerUtils)

        is MainScreenEvent.ToggleAdvancedOptionsDialog, is MainScreenEvent.ToggleCvMask,
        is MainScreenEvent.EnterCvMaskTestMode, is MainScreenEvent.ExitCvMaskTestMode,
        is MainScreenEvent.EnterCalibrationMode, is MainScreenEvent.SampleColorAt,
        is MainScreenEvent.ToggleCvRefinementMethod, is MainScreenEvent.UpdateHoughP1,
        is MainScreenEvent.UpdateHoughP2, is MainScreenEvent.UpdateCannyT1, is MainScreenEvent.UpdateCannyT2,
        is MainScreenEvent.ToggleCvModel, is MainScreenEvent.ToggleSnapping ->
            reduceAdvancedOptionsAction(currentState, action)

        is MainScreenEvent.ZoomSliderChanged, is MainScreenEvent.ZoomScaleChanged,
        is MainScreenEvent.TableRotationApplied, is MainScreenEvent.TableRotationChanged,
        is MainScreenEvent.AdjustLuminance, is MainScreenEvent.AdjustGlow,
        is MainScreenEvent.PanView, is MainScreenEvent.ApplyQuickAlign ->
            reduceControlAction(currentState, action)

        is MainScreenEvent.CvDataUpdated, is MainScreenEvent.LockOrUnlockColor,
        is MainScreenEvent.LockColor, is MainScreenEvent.ClearSamplePoint ->
            reduceCvAction(currentState, action)

        is MainScreenEvent.AddObstacleBall -> reduceObstacleAction(
            currentState,
            action,
            reducerUtils
        )

        is MainScreenEvent.SpinApplied, is MainScreenEvent.SpinSelectionEnded,
        is MainScreenEvent.DragSpinControl, is MainScreenEvent.ClearSpinState ->
            reduceSpinAction(currentState, action)

        is MainScreenEvent.SizeChanged, is MainScreenEvent.FullOrientationChanged,
        is MainScreenEvent.ThemeChanged, is MainScreenEvent.SetWarning ->
            reduceSystemAction(currentState, action)

        is MainScreenEvent.ToggleMenu, is MainScreenEvent.ToggleExpandedMenu,
        is MainScreenEvent.ToggleNavigationRail, is MainScreenEvent.ToggleSpinControl,
        is MainScreenEvent.CycleTableSize, is MainScreenEvent.SetTableSize,
        is MainScreenEvent.ToggleTableSizeDialog, is MainScreenEvent.ToggleForceTheme,
        is MainScreenEvent.ToggleCamera, is MainScreenEvent.ToggleDistanceUnit,
        is MainScreenEvent.ToggleLuminanceDialog, is MainScreenEvent.ToggleGlowStickDialog,
        is MainScreenEvent.ToggleHelp, is MainScreenEvent.ToggleMoreHelp,
        is MainScreenEvent.ToggleOrientationLock, is MainScreenEvent.ApplyPendingOrientationLock,
        is MainScreenEvent.OrientationChanged, is MainScreenEvent.SetExperienceMode,
        is MainScreenEvent.UnlockBeginnerView, is MainScreenEvent.LockBeginnerView,
        is MainScreenEvent.ToggleCalibrationScreen, is MainScreenEvent.ToggleQuickAlignScreen,
        is MainScreenEvent.ToggleBankingMode ->
            reduceToggleAction(currentState, action, reducerUtils)

        is MainScreenEvent.StartTutorial, is MainScreenEvent.NextTutorialStep,
        is MainScreenEvent.EndTutorial -> reduceTutorialAction(currentState, action)

        else -> currentState
    }
}