package com.hereliesaz.cuedetat.domain

import android.graphics.Camera
import com.hereliesaz.cuedetat.domain.reducers.*
import com.hereliesaz.cuedetat.ui.MainScreenEvent
import com.hereliesaz.cuedetat.view.state.OverlayState
import javax.inject.Inject

class StateReducer @Inject constructor(
    private val gestureReducer: GestureReducer,
    private val systemReducer: SystemReducer,
    private val toggleReducer: ToggleReducer,
    private val actionReducer: ActionReducer,
    private val obstacleReducer: ObstacleReducer,
    private val snapReducer: SnapReducer,
    private val spinReducer: SpinReducer,
    private val controlReducer: ControlReducer,
    private val cvReducer: CvReducer,
    private val toastReducer: ToastReducer,
    private val advancedOptionsReducer: AdvancedOptionsReducer,
    private val updateStateUseCase: UpdateStateUseCase
) {
    private val camera = Camera()

    fun reduce(event: MainScreenEvent, state: OverlayState): OverlayState {
        val intermediateState = when (event) {
            is MainScreenEvent.Drag, is MainScreenEvent.Release, is MainScreenEvent.ScreenGestureStarted, is MainScreenEvent.GestureEnded -> gestureReducer.reduce(event, state)
            is MainScreenEvent.FullOrientationChanged, is MainScreenEvent.ThemeChanged -> systemReducer.reduce(event, state)
            is MainScreenEvent.ToggleCamera, is MainScreenEvent.ToggleTable, is MainScreenEvent.ToggleOnPlaneBall, is MainScreenEvent.ToggleBankingMode, is MainScreenEvent.ToggleHelp, is MainScreenEvent.ToggleCvParamMenu, is MainScreenEvent.ToggleForceTheme, is MainScreenEvent.ToggleDistanceUnit -> toggleReducer.reduce(event, state)
            is MainScreenEvent.Reset, is MainScreenEvent.ClearObstacles, is MainScreenEvent.AimBankShot -> actionReducer.reduce(event, state)
            is MainScreenEvent.AddObstacle -> obstacleReducer.reduce(event, state)
            is MainScreenEvent.SnapToDetectedBall -> snapReducer.reduce(event, state)
            is MainScreenEvent.SpinDrag, is MainScreenEvent.SpinDragEnd -> spinReducer.reduce(event, state)
            is MainScreenEvent.TableRotationChanged, is MainScreenEvent.ZoomChanged, is MainScreenEvent.UpdateHoughP1, is MainScreenEvent.UpdateHoughP2, is MainScreenEvent.UpdateCannyT1, is MainScreenEvent.UpdateCannyT2, is MainScreenEvent.AdjustLuminance, is MainScreenEvent.AdjustGlow -> controlReducer.reduce(event, state)
            is MainScreenEvent.CvDataUpdated, is MainScreenEvent.LockOrUnlockColor -> cvReducer.reduce(event, state)
            is MainScreenEvent.ShowToast, is MainScreenEvent.SingleEventConsumed -> toastReducer.reduce(event, state)
            is MainScreenEvent.ToggleAdvancedOptions, is MainScreenEvent.ToggleSnapping, is MainScreenEvent.ToggleCvModel, is MainScreenEvent.ToggleCvRefinementMethod -> advancedOptionsReducer.reduce(event, state)
            else -> state
        }
        return updateStateUseCase(intermediateState, camera)
    }
}