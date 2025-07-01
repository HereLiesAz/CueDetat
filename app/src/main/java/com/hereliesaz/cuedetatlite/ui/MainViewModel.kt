package com.hereliesaz.cuedetatlite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.data.UpdateChecker
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.domain.WarningManager
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val stateReducer = StateReducer()
    private val updateStateUseCase = UpdateStateUseCase()
    private val warningManager = WarningManager()

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState(0, 0))

    private val _showUpdateAvailable = MutableStateFlow(false)
    val showUpdateAvailable: StateFlow<Boolean> = _showUpdateAvailable.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    val showKineticWarning: StateFlow<Boolean> = warningManager.showKineticWarning

    private val zoomMapping = ZoomMapping()

    init {
        checkForUpdates()
        observeSensorData()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            if (updateChecker.isUpdateAvailable()) {
                _showUpdateAvailable.value = true
            }
        }
    }

    private fun observeSensorData() {
        viewModelScope.launch {
            sensorRepository.orientationFlow.collect { orientation ->
                warningManager.updateOrientation(orientation)
                onEvent(MainScreenEvent.FullOrientationChanged(orientation))
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        val currentState = _uiState.value
        val currentScreenState = _screenState.value
        val newState = stateReducer.reduce(event, currentState, currentScreenState)
        _uiState.value = updateStateUseCase(newState, currentScreenState, zoomMapping)
    }

    fun onScreenSizeChanged(width: Int, height: Int) {
        _screenState.value = ScreenState(width, height)
        // Trigger a state update to recalculate everything with the new screen size
        onEvent(MainScreenEvent.FullOrientationChanged(_uiState.value.currentOrientation))
    }

    fun dismissUpdateDialog() {
        _showUpdateAvailable.value = false
    }

    fun dismissKineticWarning() {
        warningManager.dismissWarning()
    }

    fun onPermissionGranted() {
        _hasCameraPermission.value = true
    }
}