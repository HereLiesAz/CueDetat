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
import com.hereliesaz.cuedetat.ui.MainScreenEvent

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
    // Downcast to CueDetatAction for all reducers except the gesture reducer
    val cueDetatAction = action as? CueDetatAction

    return when {
        // Gesture events are handled by the dedicated GestureReducer
        action is MainScreenEvent.LogicalGestureStarted || action is MainScreenEvent.LogicalDragApplied || action is MainScreenEvent.GestureEnded -> {
            gestureReducer.reduce(currentState, action)
        }
        // All other actions are handled by the sub-reducers
        cueDetatAction != null -> {
            when (cueDetatAction) {
                is CueDetatAction.Reset -> reduceAction(currentState, cueDetatAction, reducerUtils)
                is CueDetatAction.ToggleAdvancedOptionsDialog, is CueDetatAction.ToggleCvMask,
                is CueDetatAction.EnterCvMaskTestMode, is CueDetatAction.ExitCvMaskTestMode,
                is CueDetatAction.EnterCalibrationMode, is CueDetatAction.SampleColorAt,
                is CueDetatAction.ToggleCvRefinementMethod, is CueDetatAction.UpdateHoughP1,
                is CueDetatAction.UpdateHoughP2, is CueDetatAction.UpdateCannyT1, is CueDetatAction.UpdateCannyT2,
                is CueDetatAction.ToggleCvModel, is CueDetatAction.ToggleSnapping ->
                    reduceAdvancedOptionsAction(currentState, cueDetatAction)

                is CueDetatAction.ZoomSliderChanged, is CueDetatAction.ZoomScaleChanged,
                is CueDetatAction.TableRotationApplied, is CueDetatAction.TableRotationChanged,
                is CueDetatAction.AdjustLuminance, is CueDetatAction.AdjustGlow,
                is CueDetatAction.PanView, is CueDetatAction.ApplyQuickAlign ->
                    reduceControlAction(currentState, cueDetatAction)

                is CueDetatAction.CvDataUpdated, is CueDetatAction.LockOrUnlockColor,
                is CueDetatAction.LockColor, is CueDetatAction.ClearSamplePoint ->
                    reduceCvAction(currentState, cueDetatAction)

                is CueDetatAction.AddObstacleBall -> reduceObstacleAction(
                    currentState,
                    cueDetatAction,
                    reducerUtils
                )

                is CueDetatAction.SpinApplied, is CueDetatAction.SpinSelectionEnded,
                is CueDetatAction.DragSpinControl, is CueDetatAction.ClearSpinState ->
                    reduceSpinAction(currentState, cueDetatAction)

                is CueDetatAction.SizeChanged, is CueDetatAction.FullOrientationChanged,
                is CueDetatAction.ThemeChanged, is CueDetatAction.SetWarning ->
                    reduceSystemAction(currentState, cueDetatAction)

                is CueDetatAction.ToggleSpinControl, is CueDetatAction.CycleTableSize,
                is CueDetatAction.SetTableSize, is CueDetatAction.ToggleTableSizeDialog,
                is CueDetatAction.ToggleForceTheme, is CueDetatAction.ToggleCamera,
                is CueDetatAction.ToggleDistanceUnit, is CueDetatAction.ToggleLuminanceDialog,
                is CueDetatAction.ToggleGlowStickDialog, is CueDetatAction.ToggleHelp,
                is CueDetatAction.ToggleMoreHelp,
                is CueDetatAction.ToggleOrientationLock,
                is CueDetatAction.ApplyPendingOrientationLock, is CueDetatAction.OrientationChanged,
                is CueDetatAction.SetExperienceMode, is CueDetatAction.UnlockBeginnerView,
                is CueDetatAction.LockBeginnerView, is CueDetatAction.ToggleCalibrationScreen,
                is CueDetatAction.ToggleQuickAlignScreen, is CueDetatAction.ToggleBankingMode ->
                    reduceToggleAction(currentState, cueDetatAction, reducerUtils)

                is CueDetatAction.StartTutorial, is CueDetatAction.NextTutorialStep,
                is CueDetatAction.EndTutorial -> reduceTutorialAction(currentState, cueDetatAction)

                else -> currentState
            }
        }
        else -> currentState
    }
}