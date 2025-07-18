// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/StateReducer.kt

package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.reducers.*
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
            is MainScreenEvent.GestureEnded,
            is MainScreenEvent.AimBankShot ->
                gestureReducer.reduce(currentState, event)

            is MainScreenEvent.ToggleOnPlaneBall,
            is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.ToggleTable,
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
            is MainScreenEvent.ToggleCvModel ->
                toggleReducer.reduce(currentState, event)

            is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.ZoomScaleChanged,
            is MainScreenEvent.TableRotationApplied,
            is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.AdjustGlow,
            is MainScreenEvent.PanView, // <-- The missing link is added here
            is MainScreenEvent.AdjustLuminance ->
                controlReducer.reduce(currentState, event)

            is MainScreenEvent.SizeChanged,
            is MainScreenEvent.FullOrientationChanged,
            is MainScreenEvent.ThemeChanged ->
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

            is MainScreenEvent.CvDataUpdated -> {
                // First, process standard CV data changes (like color)
                val stateAfterCv = cvReducer.reduce(currentState, event)
                // Then, process snapping based on the new vision data
                snapReducer.reduce(stateAfterCv, event.data)
            }
            is MainScreenEvent.LockOrUnlockColor ->
                cvReducer.reduce(currentState, event)

            is MainScreenEvent.ToggleAdvancedOptionsDialog,
            is MainScreenEvent.ToggleCvRefinementMethod,
            is MainScreenEvent.UpdateHoughP1,
            is MainScreenEvent.UpdateHoughP2,
            is MainScreenEvent.UpdateCannyT1,
            is MainScreenEvent.UpdateCannyT2 ->
                advancedOptionsReducer.reduce(currentState, event)

            else -> currentState
        }
    }
}