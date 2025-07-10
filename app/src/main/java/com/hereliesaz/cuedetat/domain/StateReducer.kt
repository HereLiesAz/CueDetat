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
    private val spinReducer: SpinReducer
) {
    fun reduce(currentState: OverlayState, event: MainScreenEvent): OverlayState {
        // Delegate to the appropriate specialized reducer based on the event type.
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
            is MainScreenEvent.ToggleSpinControl -> // Re-routed to the correct reducer
                toggleReducer.reduce(currentState, event)

            is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.ZoomScaleChanged,
            is MainScreenEvent.TableRotationApplied,
            is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.AdjustGlow,
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

            else -> currentState
        }
    }
}