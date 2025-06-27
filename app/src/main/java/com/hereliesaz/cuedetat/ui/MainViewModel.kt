package com.hereliesaz.cuedetat.ui

import android.content.Context
import android.graphics.Camera
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.domain.StateReducer
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.domain.WarningManager
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
    private val stateReducer: StateReducer,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

    private val graphicsCamera = Camera()

    init {
        checkArAvailability()
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collect { fullOrientation ->
                onEvent(MainScreenEvent.FullOrientationChanged(fullOrientation))
            }
        }
    }

    private fun checkArAvailability() {
        val availability = ArCoreApk.getInstance().checkAvailability(applicationContext)
        val isSupported = availability.isSupported
        Log.i("MainViewModel", "ARCore support status: $isSupported")
        onEvent(MainScreenEvent.ArAvailabilityChecked(isSupported))
    }

    fun onEvent(event: MainScreenEvent) {
        if (event is MainScreenEvent.ToggleSpatialLock && event.isLocked && !_uiState.value.isArSupported) {
            viewModelScope.launch {
                _toastMessage.value = ToastMessage.PlainText("AR not supported. Lock feature disabled.")
            }
            return
        }

        val eventToReduce = when (event) {
            is MainScreenEvent.UnitMoved -> {
                val logicalPoint = Perspective.screenToLogical(event.position, _uiState.value.inversePitchMatrix)
                MainScreenEvent.UpdateLogicalUnitPosition(logicalPoint)
            }
            is MainScreenEvent.ActualCueBallMoved -> {
                val logicalPoint = Perspective.screenToLogical(event.position, _uiState.value.inversePitchMatrix)
                MainScreenEvent.UpdateLogicalActualCueBallPosition(logicalPoint)
            }
            is MainScreenEvent.BankingAimTargetDragged -> {
                val logicalPoint = Perspective.screenToLogical(event.screenPoint, _uiState.value.inversePitchMatrix)
                MainScreenEvent.UpdateLogicalBankingAimTarget(logicalPoint)
            }
            is MainScreenEvent.ArAnchorPlaced -> {
                if (event.anchor == null) {
                    viewModelScope.launch {
                        _toastMessage.value = ToastMessage.PlainText("Could not find a surface to lock onto.")
                    }
                }
                event
            }
            is MainScreenEvent.ToastShown -> {
                _toastMessage.value = null
                event
            }
            else -> event
        }

        _uiState.update { currentState ->
            stateReducer.reduce(currentState, eventToReduce)
        }

        if (!_uiState.value.isSpatiallyLocked) {
            _uiState.update { currentState ->
                val finalState = updateStateUseCase.invoke(currentState, graphicsCamera)
                val warning = warningManager.checkWarnings(finalState)
                if (warning != finalState.warningText) {
                    finalState.copy(warningText = warning)
                } else {
                    finalState
                }
            }
        }
    }

    fun onArSessionCreated(session: Session) {
        _uiState.update { it.copy(arSession = session) }
        Log.d("MainViewModel", "AR Session provided to ViewModel.")
    }
}
