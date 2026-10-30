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

/**
 * The Root Reducer for the application's Model-View-Intent (MVI) architecture.
 *
 * CONCEPT:
 * In MVI, the State is immutable. To change the UI, an [Intent] (or Event) must be fired.
 * The Reducer takes the [currentState] and the [Event], and calculates the [newState].
 *
 * This function routes most main-screen events to specialized sub-reducers
 * (e.g., [reduceControlAction] for zoom/pan, [reduceCvAction] for vision data).
 *
 * It is NOT the single entry point for state changes. Known writers outside it:
 * - `SnapReducer.reduce`, invoked by MainViewModel after this reducer on CvDataUpdated.
 * - MainViewModel branches that write state directly or re-copy fields after this reducer
 *   (ToggleExperienceModeSelection, SetTopDownBitmap, CvDataUpdated phase promotion).
 * - UpdateStateUseCase, which derives matrices and aiming geometry after every reduction.
 *
 * Purity: deterministic given its inputs, except [reduceObstacleAction]'s random-placement
 * fallback (default [kotlin.random.Random.Default]; injectable for tests). SnapReducer's clock is
 * likewise injectable via its `nowMs` parameter.
 *
 * Every `MainScreenEvent` a sub-reducer pattern-matches must be listed here, or it falls into
 * `else` and is silently dropped. StateReducerRoutingTest enforces this.
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
    val primaryResult = when (action) {
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
        is MainScreenEvent.UpdateCannyT1, is MainScreenEvent.UpdateCannyT2 ->
            reduceAdvancedOptionsAction(currentState, action)

        // --- CONTROLS & TRANSFORMS ---
        // Handled by [ControlReducer].
        // Covers explicit UI controls like Sliders (Zoom/Rotation) and Button presses for alignment.
        is MainScreenEvent.ZoomSliderChanged, is MainScreenEvent.ZoomScaleChanged,
        is MainScreenEvent.TableRotationApplied, is MainScreenEvent.TableRotationChanged,
        is MainScreenEvent.TableScaleChanged, is MainScreenEvent.SetTableFitScale,
        is MainScreenEvent.ArNotice, is MainScreenEvent.ArSessionStarted, is MainScreenEvent.ArTableLockLost,
        is MainScreenEvent.AdjustLuminance, is MainScreenEvent.AdjustGlow,
        is MainScreenEvent.PanView, is MainScreenEvent.MoveTableZ,
        is MainScreenEvent.SetTableZ,
        is MainScreenEvent.ApplyQuickAlign,
        is MainScreenEvent.LoadTableScan, is MainScreenEvent.ClearTableScan,
        is MainScreenEvent.UpdateArPose,
        is MainScreenEvent.ArCameraPoseUpdated,
        is MainScreenEvent.DepthCapabilityDetected,
        is MainScreenEvent.ArTableMatrixUpdated, is MainScreenEvent.LockArTable,
        is MainScreenEvent.UnlockArTable, is MainScreenEvent.ArTableLockResult,
        is MainScreenEvent.CloudPlacementResult,
        is MainScreenEvent.TableFitUpdated, is MainScreenEvent.ApplyTablePose,
        is MainScreenEvent.ArModuleLoadStarted, is MainScreenEvent.ArModuleLoadSucceeded,
        is MainScreenEvent.ArModuleLoadFailed, is MainScreenEvent.ToggleViewLock ->
            reduceControlAction(currentState, action)

        // --- COMPUTER VISION DATA ---
        // Handled by [CvReducer].
        // Covers updates flowing in from the VisionRepository and color locking interactions.
        is MainScreenEvent.CvDataUpdated, is MainScreenEvent.LockColor,
        is MainScreenEvent.AddFeltSample, is MainScreenEvent.DeleteFeltSamples,
        is MainScreenEvent.AutoCalibrateCv,
        is MainScreenEvent.ForceArActive, is MainScreenEvent.SeedRelocaliser,
        is MainScreenEvent.ArSurfaceTapped, is MainScreenEvent.BeginBallSelection ->
            reduceCvAction(currentState, action)

        // --- OBSTACLES ---
        // Handled by [ObstacleReducer].
        // Adding/Removing virtual obstacles.
        is MainScreenEvent.AddObstacleBall -> reduceObstacleAction(
            currentState,
            action,
            reducerUtils
        )

        // --- SPIN & MASSE CONTROL ---
        // Handled by [SpinReducer].
        // Interactions with the cue ball spin selector and Masse trajectory calculations.
        // ToggleMasseMode and SpinPathTick are included here to manage interaction isolation
        // and visual path decay.
        is MainScreenEvent.SpinApplied, is MainScreenEvent.SpinSelectionEnded,
        is MainScreenEvent.DragSpinControl, is MainScreenEvent.ClearSpinState,
        is MainScreenEvent.ToggleMasseMode, is MainScreenEvent.SpinPathTick,
        is MainScreenEvent.ToggleSpinControl ->
            reduceSpinAction(currentState, action)

        // --- SYSTEM EVENTS ---
        // Handled by [SystemReducer].
        // Lifecycle events (resize), theme changes, and warnings.
        is MainScreenEvent.SizeChanged, is MainScreenEvent.FullOrientationChanged,
        is MainScreenEvent.ThemeChanged, is MainScreenEvent.SetWarning,
        is MainScreenEvent.WearableStateUpdated ->
            reduceSystemAction(currentState, action)

        // --- UI TOGGLES ---
        // Handled by [ToggleReducer].
        // Simple boolean flips for showing/hiding dialogs and menus.
        is MainScreenEvent.CycleTableSize, is MainScreenEvent.SetTableSize,
        is MainScreenEvent.ToggleTableSizeDialog,
        is MainScreenEvent.CycleCameraMode, is MainScreenEvent.ToggleDistanceUnit,
        is MainScreenEvent.ToggleLuminanceDialog, is MainScreenEvent.ToggleGlowStickDialog,
        is MainScreenEvent.ToggleSnapping,
        is MainScreenEvent.ToggleHelp,
        is MainScreenEvent.ToggleOrientationLock, is MainScreenEvent.ApplyPendingOrientationLock,
        is MainScreenEvent.ApplyPendingExperienceMode,
        is MainScreenEvent.SetExperienceMode,
        is MainScreenEvent.UnlockBeginnerView, is MainScreenEvent.LockBeginnerView,
        is MainScreenEvent.ExitToSplash,
        is MainScreenEvent.ToggleBankingMode, is MainScreenEvent.ToggleTableScanScreen,
        is MainScreenEvent.CancelArSetup,
        is MainScreenEvent.TurnCameraOff,
        is MainScreenEvent.SetCameraMode,
        is MainScreenEvent.ToggleTargetType, is MainScreenEvent.ToggleAdvisor,
        is MainScreenEvent.ToggleTopDownView, is MainScreenEvent.ClearTopDownView,
        is MainScreenEvent.SetTopDownBitmap,
        is MainScreenEvent.StartArTracking ->
            reduceToggleAction(currentState, action, reducerUtils)

        is MainScreenEvent.RecommendationComputed ->
            currentState.copy(recommendedShot = action.shot)


        // Events handled outside the reducer (MainViewModel intercepts) land here unchanged.
        // A sub-reducer-handled event landing here is a bug; StateReducerRoutingTest catches it.
        else -> currentState
    }

    return primaryResult
}