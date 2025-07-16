// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/Reducer.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import com.hereliesaz.cuedetat.domain.reducers.*
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class Reducer @Inject constructor(
    private val gestureReducer: GestureReducer,
    private val orientationReducer: OrientationReducer,
    private val settingsReducer: SettingsReducer,
    private val toastReducer: ToastReducer,
    private val tutorialReducer: TutorialReducer,
    private val visionReducer: VisionReducer,
    private val updateStateUseCase: UpdateStateUseCase
) {
    private val camera = Camera()

    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        val intermediateState = when (event) {
            is MainScreenEvent.Drag,
            is MainScreenEvent.Release -> gestureReducer.reduce(event, state)

            is MainScreenEvent.OrientationChanged -> orientationReducer.reduce(event, state)

            is MainScreenEvent.VisionDataUpdated -> visionReducer.reduce(event, state)

            is MainScreenEvent.LoadUserSettings,
            is MainScreenEvent.Reset,
            is MainScreenEvent.UpdateColorScheme,
            is MainScreenEvent.ToggleCamera,
            is MainScreenEvent.SwitchCamera,
            is MainScreenEvent.UpdateZoom,
            is MainScreenEvent.ToggleTable,
            is MainScreenEvent.UpdateTableRotation,
            is MainScreenEvent.CycleTableSize,
            is MainScreenEvent.SetTableSize,
            is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.UpdateBankingAim,
            is MainScreenEvent.ToggleOnPlaneBall,
            is MainScreenEvent.ToggleLuminanceDialog,
            is MainScreenEvent.ToggleGlowStickDialog,
            is MainScreenEvent.ToggleTableSizeDialog,
            is MainScreenEvent.ToggleAdvancedOptionsDialog,
            is MainScreenEvent.ToggleForceLightMode,
            is MainScreenEvent.ToggleHelpers,
            is MainScreenEvent.ToggleSpinControl,
            is MainScreenEvent.ClearSpinState,
            is MainScreenEvent.AddObstacle -> settingsReducer.reduce(event, state)

            is MainScreenEvent.ShowToast,
            is MainScreenEvent.ToastShown -> toastReducer.reduce(event, state)

            is MainScreenEvent.StartTutorial,
            is MainScreenEvent.NextTutorialStep,
            is MainScreenEvent.FinishTutorial -> tutorialReducer.reduce(event, state)

            else -> state
        }

        // Only run the full update if the state has actually changed.
        return if (intermediateState != state) {
            updateStateUseCase(intermediateState, camera)
        } else {
            state
        }
    }
}