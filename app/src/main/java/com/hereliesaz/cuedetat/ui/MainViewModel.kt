// app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt
package com.hereliesaz.cuedetat.ui

import android.content.Context
import android.graphics.Camera
import android.graphics.PointF
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.domain.WarningManager
import com.hereliesaz.cuedetat.view.model.ActualCueBall
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val updateStateUseCase: UpdateStateUseCase,
    private val warningManager: WarningManager,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

    private val graphicsCamera = Camera()

    init {
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collect { fullOrientation ->
                if (!uiState.value.isSpatiallyLocked) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            pitchAngle = fullOrientation.pitch,
                            yawAngle = fullOrientation.yaw,
                            rollAngle = fullOrientation.roll,
                            currentOrientation = fullOrientation
                        )
                    }
                    _uiState.update { currentState ->
                        updateStateUseCase.invoke(currentState, graphicsCamera)
                    }
                }
            }
        }
    }

    private fun getCurrentLogicalRadius(
        stateWidth: Int,
        stateHeight: Int,
        zoomSliderPos: Float
    ): Float {
        if (stateWidth == 0 || stateHeight == 0) return 1f
        val zoomFactor = ZoomMapping.sliderToZoom(zoomSliderPos)
        return (min(stateWidth, stateHeight) * 0.30f / 2f) * zoomFactor
    }


    fun onEvent(event: MainScreenEvent) {
        val preEventState = _uiState.value
        val stateAfterEvent = when (event) {
            is MainScreenEvent.SizeChanged -> preEventState.copy(viewWidth = event.width, viewHeight = event.height)
            is MainScreenEvent.ZoomSliderChanged -> {
                val newSliderPos = event.position.coerceIn(0f, 100f)
                val newLogicalRadius = getCurrentLogicalRadius(preEventState.viewWidth, preEventState.viewHeight, newSliderPos)
                preEventState.copy(
                    protractorUnit = preEventState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = preEventState.actualCueBall?.copy(radius = newLogicalRadius),
                    zoomSliderPosition = newSliderPos
                )
            }
            is MainScreenEvent.ZoomScaleChanged -> {
                val currentZoomValue = ZoomMapping.sliderToZoom(preEventState.zoomSliderPosition)
                val newZoomValue = (currentZoomValue * event.scaleFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                val newSliderPos = ZoomMapping.zoomToSlider(newZoomValue)
                val newLogicalRadius = getCurrentLogicalRadius(preEventState.viewWidth, preEventState.viewHeight, newSliderPos)
                preEventState.copy(
                    protractorUnit = preEventState.protractorUnit.copy(radius = newLogicalRadius),
                    actualCueBall = preEventState.actualCueBall?.copy(radius = newLogicalRadius),
                    zoomSliderPosition = newSliderPos
                )
            }
            is MainScreenEvent.RotationChanged -> preEventState.copy(protractorUnit = preEventState.protractorUnit.copy(rotationDegrees = event.newRotation))
            is MainScreenEvent.UnitMoved -> {
                val newLogicalPoint = Perspective.screenToLogical(event.position, preEventState.inversePitchMatrix)
                preEventState.copy(protractorUnit = preEventState.protractorUnit.copy(logicalPosition = newLogicalPoint))
            }
            is MainScreenEvent.ActualCueBallMoved -> {
                val newLogicalPoint = Perspective.screenToLogical(event.position, preEventState.inversePitchMatrix)
                preEventState.copy(actualCueBall = preEventState.actualCueBall?.copy(logicalPosition = newLogicalPoint))
            }
            is MainScreenEvent.TableRotationChanged -> preEventState.copy(tableRotationDegrees = event.degrees)
            is MainScreenEvent.BankingAimTargetDragged -> {
                val logicalAimPoint = Perspective.screenToLogical(event.screenPoint, preEventState.inversePitchMatrix)
                preEventState.copy(bankingAimTarget = logicalAimPoint)
            }
            is MainScreenEvent.UpdateLogicalActualCueBallPosition -> preEventState.copy(actualCueBall = preEventState.actualCueBall?.copy(logicalPosition = event.logicalPoint) ?: ActualCueBall(logicalPosition = event.logicalPoint, radius = preEventState.protractorUnit.radius / 2f))
            is MainScreenEvent.UpdateLogicalUnitPosition -> preEventState.copy(protractorUnit = preEventState.protractorUnit.copy(logicalPosition = event.logicalPoint))
            is MainScreenEvent.UpdateLogicalBankingAimTarget -> preEventState.copy(bankingAimTarget = event.logicalPoint)
            is MainScreenEvent.FullOrientationChanged -> preEventState.copy(
                pitchAngle = event.orientation.pitch,
                yawAngle = event.orientation.yaw,
                rollAngle = event.orientation.roll,
                currentOrientation = event.orientation
            )
            is MainScreenEvent.ThemeChanged -> preEventState.copy(currentThemeColor = event.scheme.primary, appControlColorScheme = event.scheme)
            MainScreenEvent.ToggleForceTheme -> preEventState.copy(isForceLightMode = when (preEventState.isForceLightMode) { true -> false; false -> null; null -> true })
            MainScreenEvent.ToggleLuminanceDialog -> preEventState.copy(showLuminanceDialog = !preEventState.showLuminanceDialog)
            is MainScreenEvent.AdjustLuminance -> preEventState.copy(luminanceAdjustment = event.adjustment)
            is MainScreenEvent.ShowToast -> {
                _toastMessage.value = event.message
                preEventState
            }
            MainScreenEvent.StartTutorial -> preEventState.copy(
                showTutorialOverlay = true, currentTutorialStep = 0,
                areHelpersVisible = false, showLuminanceDialog = false, isMoreHelpVisible = false,
                isSpatiallyLocked = false, anchorOrientation = null
            )
            MainScreenEvent.NextTutorialStep -> preEventState.copy(currentTutorialStep = preEventState.currentTutorialStep + 1)
            MainScreenEvent.EndTutorial -> preEventState.copy(showTutorialOverlay = false)
            MainScreenEvent.Reset -> preEventState.copy(
                protractorUnit = ProtractorUnit(radius = 100f, rotationDegrees = 0f, logicalPosition = PointF(preEventState.viewWidth/2f, preEventState.viewHeight/2f)),
                actualCueBall = null,
                tableRotationDegrees = 0f,
                bankingAimTarget = null,
                isBankingMode = false,
                isSpatiallyLocked = false,
                zoomSliderPosition = 0.5f,
                valuesChangedSinceReset = false,
                currentOrientation = FullOrientation(0f, 0f, 0f),
                anchorOrientation = null,
                pitchMatrix = android.graphics.Matrix(),
                railPitchMatrix = android.graphics.Matrix(),
                isMoreHelpVisible = false,
                appControlColorScheme = null
            )
            MainScreenEvent.ToggleHelp -> preEventState.copy(areHelpersVisible = !preEventState.areHelpersVisible)
            MainScreenEvent.ToggleActualCueBall -> preEventState.copy(actualCueBall = if (preEventState.actualCueBall == null) ActualCueBall(radius = preEventState.protractorUnit.radius / 2f, logicalPosition = PointF(preEventState.viewWidth / 2f + 100, preEventState.viewHeight / 2f + 100)) else null)
            MainScreenEvent.ToggleBankingMode -> preEventState.copy(isBankingMode = !preEventState.isBankingMode, showProtractor = !preEventState.isBankingMode, showTable = !preEventState.isBankingMode)
            is MainScreenEvent.ToggleSpatialLock -> {
                if (event.isLocked) {
                    preEventState.copy(
                        isSpatiallyLocked = true,
                        anchorOrientation = preEventState.currentOrientation
                    )
                } else {
                    preEventState.copy(
                        isSpatiallyLocked = false,
                        anchorOrientation = null
                    )
                }
            }
            MainScreenEvent.CheckForUpdate -> preEventState
            MainScreenEvent.ViewArt -> preEventState
            MainScreenEvent.FeatureComingSoon -> preEventState
            MainScreenEvent.ShowDonationOptions -> preEventState
            MainScreenEvent.SingleEventConsumed -> preEventState
            MainScreenEvent.ToastShown -> {
                _toastMessage.value = null
                preEventState
            }
            MainScreenEvent.GestureStarted -> preEventState
            MainScreenEvent.GestureEnded -> preEventState
        }

        val finalState = if (!stateAfterEvent.isSpatiallyLocked) {
            updateStateUseCase.invoke(stateAfterEvent, graphicsCamera)
        } else {
            stateAfterEvent
        }
        _uiState.value = finalState

        val currentWarning = warningManager.checkWarnings(finalState)
        if (currentWarning != _uiState.value.warningText) {
            _uiState.update { it.copy(warningText = currentWarning) }
        }
    }

    fun onArSessionCreated(session: Session) {
        _uiState.update { it.copy(arSession = session) }
        Log.d("MainViewModel", "AR Session provided to ViewModel.")
    }
}