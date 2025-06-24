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
import com.hereliesaz.cuedetat.view.model.ProtractorUnit
import com.hereliesaz.cuedetat.view.model.Perspective
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

    fun onEvent(event: MainScreenEvent) {
        _uiState.update { currentState ->
            val newState = when (event) {
                is MainScreenEvent.SizeChanged -> currentState.copy(viewWidth = event.width, viewHeight = event.height)
                is MainScreenEvent.ZoomSliderChanged -> currentState.copy(zoomSliderPosition = event.position)
                is MainScreenEvent.ZoomScaleChanged -> currentState
                is MainScreenEvent.RotationChanged -> currentState.copy(protractorUnit = currentState.protractorUnit.copy(rotationDegrees = event.newRotation))
                is MainScreenEvent.UnitMoved -> currentState.copy(protractorUnit = currentState.protractorUnit.copy(screenCenter = event.position))
                is MainScreenEvent.ActualCueBallMoved -> currentState.copy(actualCueBall = currentState.actualCueBall?.copy(screenCenter = event.position) ?: ActualCueBall(screenCenter = event.position, radius = currentState.protractorUnit.radius / 2f, logicalPosition = PointF(0f,0f)))
                is MainScreenEvent.TableRotationChanged -> currentState.copy(tableRotationDegrees = event.degrees)
                is MainScreenEvent.BankingAimTargetDragged -> {
                    val logicalAimPoint = Perspective.screenToLogical(event.screenPoint, currentState.inversePitchMatrix)
                    currentState.copy(bankingAimTarget = logicalAimPoint)
                }
                is MainScreenEvent.UpdateLogicalActualCueBallPosition -> currentState.copy(actualCueBall = currentState.actualCueBall?.copy(logicalPosition = event.logicalPoint) ?: ActualCueBall(logicalPosition = event.logicalPoint, radius = currentState.protractorUnit.radius / 2f, screenCenter = PointF(0f,0f)))
                is MainScreenEvent.UpdateLogicalUnitPosition -> currentState.copy(protractorUnit = currentState.protractorUnit.copy(logicalPosition = event.logicalPoint))
                is MainScreenEvent.UpdateLogicalBankingAimTarget -> currentState.copy(bankingAimTarget = event.logicalPoint)
                is MainScreenEvent.FullOrientationChanged -> currentState.copy(
                    pitchAngle = event.orientation.pitch,
                    yawAngle = event.orientation.yaw,
                    rollAngle = event.orientation.roll,
                    currentOrientation = event.orientation
                )
                is MainScreenEvent.ThemeChanged -> currentState.copy(currentThemeColor = event.scheme.primary, appControlColorScheme = event.scheme)
                MainScreenEvent.ToggleForceTheme -> currentState.copy(isForceLightMode = when (currentState.isForceLightMode) { true -> false; false -> null; null -> true })
                MainScreenEvent.ToggleLuminanceDialog -> currentState.copy(showLuminanceDialog = !currentState.showLuminanceDialog)
                is MainScreenEvent.AdjustLuminance -> currentState.copy(luminanceAdjustment = event.adjustment)
                is MainScreenEvent.ShowToast -> { _toastMessage.value = event.message; currentState }
                MainScreenEvent.StartTutorial -> currentState.copy(
                    showTutorialOverlay = true, currentTutorialStep = 0,
                    areHelpersVisible = false, showLuminanceDialog = false, isMoreHelpVisible = false,
                    isSpatiallyLocked = false, anchorOrientation = null
                )
                MainScreenEvent.NextTutorialStep -> currentState.copy(currentTutorialStep = currentState.currentTutorialStep + 1)
                MainScreenEvent.EndTutorial -> currentState.copy(showTutorialOverlay = false)
                MainScreenEvent.Reset -> currentState.copy(
                    protractorUnit = ProtractorUnit(screenCenter = PointF(0f,0f), radius = 100f, rotationDegrees = 0f, logicalPosition = PointF(0f,0f)),
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
                MainScreenEvent.ToggleHelp -> currentState.copy(areHelpersVisible = !currentState.areHelpersVisible)
                MainScreenEvent.ToggleActualCueBall -> currentState.copy(actualCueBall = if (currentState.actualCueBall == null) ActualCueBall(screenCenter = PointF(currentState.viewWidth / 2f + 100, currentState.viewHeight / 2f + 100), radius = currentState.protractorUnit.radius / 2f, logicalPosition = PointF(0f,0f)) else null)
                MainScreenEvent.ToggleBankingMode -> currentState.copy(isBankingMode = !currentState.isBankingMode, showProtractor = !currentState.isBankingMode, showTable = !currentState.isBankingMode)
                is MainScreenEvent.ToggleSpatialLock -> {
                    if (event.isLocked) {
                        currentState.copy(
                            isSpatiallyLocked = true,
                            anchorOrientation = currentState.currentOrientation
                        )
                    } else {
                        currentState.copy(
                            isSpatiallyLocked = false,
                            anchorOrientation = null
                        )
                    }
                }
                MainScreenEvent.CheckForUpdate -> currentState
                MainScreenEvent.ViewArt -> currentState
                MainScreenEvent.FeatureComingSoon -> currentState
                MainScreenEvent.ShowDonationOptions -> currentState
                MainScreenEvent.SingleEventConsumed -> currentState
                MainScreenEvent.ToastShown -> { _toastMessage.value = null; currentState }
                MainScreenEvent.GestureStarted -> currentState
                MainScreenEvent.GestureEnded -> currentState
            }
            if (!newState.isSpatiallyLocked) {
                updateStateUseCase.invoke(newState, graphicsCamera)
            } else {
                newState
            }
        }.also { newState ->
        }
        val currentWarning = warningManager.checkWarnings(uiState.value)
        if (currentWarning != uiState.value.warningText) {
            _uiState.update { it.copy(warningText = currentWarning) }
        }
    }

    fun onArSessionCreated(session: Session) {
        _uiState.update { it.copy(arSession = session) }
        Log.d("MainViewModel", "AR Session provided to ViewModel.")
    }
}