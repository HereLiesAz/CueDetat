package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.reducers.reduceAction
import com.hereliesaz.cuedetat.domain.reducers.reduceAdvancedOptionsAction
import com.hereliesaz.cuedetat.domain.reducers.reduceControlAction
import com.hereliesaz.cuedetat.domain.reducers.reduceCvAction
import com.hereliesaz.cuedetat.domain.reducers.reduceGestureAction
import com.hereliesaz.cuedetat.domain.reducers.reduceHaterAction
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
    action: CueDetatAction,
    reducerUtils: ReducerUtils
): CueDetatState {
    return when (action) {
        // High-level App Actions
        is CueDetatAction.ToggleExperienceMode -> {
            val newOverlay = if (currentState.overlay is OverlayState.None) {
                OverlayState.ExperienceModeSelection(
                    currentState.experienceMode ?: ExperienceMode.EXPERT
                )
            } else {
                OverlayState.None
            }
            currentState.copy(overlay = newOverlay)
        }
        is CueDetatAction.ApplyPendingExperienceMode -> {
            if (currentState.experienceMode == action.mode) {
                currentState.copy(overlay = OverlayState.None)
            } else {
                reduceToggleAction(
                    currentState.copy(overlay = OverlayState.None),
                    CueDetatAction.SetExperienceMode(action.mode),
                    reducerUtils
                )
            }
        }
        is CueDetatAction.HaterAction -> {
            val newHaterState = reduceHaterAction(currentState.haterState, action.action)
            currentState.copy(haterState = newHaterState)
        }

        // Delegate to specialized reducers based on action type
        is CueDetatAction.Reset -> reduceAction(currentState, action, reducerUtils)

        is CueDetatAction.ToggleAdvancedOptionsDialog, is CueDetatAction.ToggleCvMask,
        is CueDetatAction.EnterCvMaskTestMode, is CueDetatAction.ExitCvMaskTestMode,
        is CueDetatAction.EnterCalibrationMode, is CueDetatAction.SampleColorAt,
        is CueDetatAction.ToggleCvRefinementMethod, is CueDetatAction.UpdateHoughP1,
        is CueDetatAction.UpdateHoughP2, is CueDetatAction.UpdateCannyT1, is CueDetatAction.UpdateCannyT2,
        is CueDetatAction.ToggleCvModel, is CueDetatAction.ToggleSnapping ->
            reduceAdvancedOptionsAction(currentState, action)

        is CueDetatAction.ZoomSliderChanged, is CueDetatAction.ZoomScaleChanged,
        is CueDetatAction.TableRotationApplied, is CueDetatAction.TableRotationChanged,
        is CueDetatAction.AdjustLuminance, is CueDetatAction.AdjustGlow,
        is CueDetatAction.PanView, is CueDetatAction.ApplyQuickAlign ->
            reduceControlAction(currentState, action)

        is CueDetatAction.CvDataUpdated, is CueDetatAction.LockOrUnlockColor,
        is CueDetatAction.LockColor, is CueDetatAction.ClearSamplePoint ->
            reduceCvAction(currentState, action)

        is CueDetatAction.LogicalGestureStarted, is CueDetatAction.LogicalDragApplied,
        is CueDetatAction.GestureEnded -> reduceGestureAction(currentState, action, reducerUtils)

        is CueDetatAction.AddObstacleBall -> reduceObstacleAction(
            currentState,
            action,
            reducerUtils
        )

        is CueDetatAction.SpinApplied, is CueDetatAction.SpinSelectionEnded,
        is CueDetatAction.DragSpinControl, is CueDetatAction.ClearSpinState ->
            reduceSpinAction(currentState, action)

        is CueDetatAction.SizeChanged, is CueDetatAction.FullOrientationChanged,
        is CueDetatAction.ThemeChanged, is CueDetatAction.SetWarning ->
            reduceSystemAction(currentState, action)

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
            reduceToggleAction(currentState, action, reducerUtils)

        is CueDetatAction.StartTutorial, is CueDetatAction.NextTutorialStep,
        is CueDetatAction.EndTutorial -> reduceTutorialAction(currentState, action)

        // Events that don't change state directly in the reducer
        else -> currentState
    }
}