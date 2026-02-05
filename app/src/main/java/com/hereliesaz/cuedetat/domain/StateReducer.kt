// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt

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
 * The Root Reducer for the application's Model-View-Intent (MVI) architecture.
 *
 * CONCEPT:
 * In MVI, the State is immutable. To change the UI, an [Intent] (or Event) must be fired.
 * The Reducer takes the [currentState] and the [Event], and calculates the [newState].
 *
 * This function is the single entry point for ALL state changes in the main screen.
 * To keep this file manageable, it delegates specific event types to specialized sub-reducers
 * (e.g., [reduceControlAction] for zoom/pan, [reduceCvAction] for vision data).
 *
 * @param currentState The state before the event is processed.
 * @param action The event triggered by the UI or System.
 * @param reducerUtils Helper functions for common state manipulations (e.g., matrix updates).
 * @param gestureReducer A specialized class-based reducer for handling complex touch gestures.
 * @return The new immutable state.
 */
fun stateReducer(
    currentState: CueDetatState,
    action: MainScreenEvent,
    reducerUtils: ReducerUtils,
    gestureReducer: GestureReducer
): CueDetatState {
    return when (action) {
        // --- GESTURE HANDLING ---
        // Complex interactions like Dragging balls, panning the table, or rotating.
        // These are handled by a stateful helper class [GestureReducer] because they
        // often depend on the "start" position of the drag.
        is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied, is MainScreenEvent.GestureEnded -> {
            gestureReducer.reduce(currentState, action)
        }

        // --- SYSTEM RESET ---
        // Resets the app to default settings.
        is MainScreenEvent.Reset -> reduceAction(currentState, action, reducerUtils)

        // --- ADVANCED OPTIONS / DEBUGGING ---
        // Handled by [AdvancedOptionsReducer].
        // Covers CV tuning (thresholds), Debug Masks, and hidden developer toggles.
        is MainScreenEvent.ToggleAdvancedOptionsDialog, is MainScreenEvent.ToggleCvMask,
        is MainScreenEvent.EnterCvMaskTestMode, is MainScreenEvent.ExitCvMaskTestMode,
        is MainScreenEvent.EnterCalibrationMode, is MainScreenEvent.SampleColorAt,
        is MainScreenEvent.ToggleCvRefinementMethod, is MainScreenEvent.UpdateHoughP1,
        is MainScreenEvent.UpdateHoughP2, is MainScreenEvent.UpdateCannyT1, is MainScreenEvent.UpdateCannyT2,
        is MainScreenEvent.ToggleCvModel, is MainScreenEvent.ToggleSnapping ->
            reduceAdvancedOptionsAction(currentState, action)

        // --- CONTROLS & TRANSFORMS ---
        // Handled by [ControlReducer].
        // Covers explicit UI controls like Sliders (Zoom/Rotation) and Button presses for alignment.
        is MainScreenEvent.ZoomSliderChanged, is MainScreenEvent.ZoomScaleChanged,
        is MainScreenEvent.TableRotationApplied, is MainScreenEvent.TableRotationChanged,
        is MainScreenEvent.AdjustLuminance, is MainScreenEvent.AdjustGlow,
        is MainScreenEvent.PanView, is MainScreenEvent.ApplyQuickAlign ->
            reduceControlAction(currentState, action)

        // --- COMPUTER VISION DATA ---
        // Handled by [CvReducer].
        // Covers updates flowing in from the VisionRepository and color locking interactions.
        is MainScreenEvent.CvDataUpdated, is MainScreenEvent.LockOrUnlockColor,
        is MainScreenEvent.LockColor, is MainScreenEvent.ClearSamplePoint ->
            reduceCvAction(currentState, action)

        // --- OBSTACLES ---
        // Handled by [ObstacleReducer].
        // Adding/Removing virtual obstacles.
        is MainScreenEvent.AddObstacleBall -> reduceObstacleAction(
            currentState,
            action,
            reducerUtils
        )

        // --- SPIN CONTROL ---
        // Handled by [SpinReducer].
        // Interactions with the cue ball spin selector.
        is MainScreenEvent.SpinApplied, is MainScreenEvent.SpinSelectionEnded,
        is MainScreenEvent.DragSpinControl, is MainScreenEvent.ClearSpinState ->
            reduceSpinAction(currentState, action)

        // --- SYSTEM EVENTS ---
        // Handled by [SystemReducer].
        // Lifecycle events (resize), theme changes, and warnings.
        is MainScreenEvent.SizeChanged, is MainScreenEvent.FullOrientationChanged,
        is MainScreenEvent.ThemeChanged, is MainScreenEvent.SetWarning ->
            reduceSystemAction(currentState, action)

        // --- UI TOGGLES ---
        // Handled by [ToggleReducer].
        // Simple boolean flips for showing/hiding dialogs and menus.
        is MainScreenEvent.ToggleMenu, is MainScreenEvent.ToggleNavigationRail,
        is MainScreenEvent.ToggleSpinControl,
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

        // --- ONBOARDING ---
        // Handled by [TutorialReducer].
        is MainScreenEvent.StartTutorial, is MainScreenEvent.NextTutorialStep,
        is MainScreenEvent.EndTutorial -> reduceTutorialAction(currentState, action)

        // Default catch-all (should ideally never happen for known events).
        else -> currentState
    }
}
