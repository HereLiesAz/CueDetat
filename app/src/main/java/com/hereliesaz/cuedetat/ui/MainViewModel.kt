package com.hereliesaz.cuedetat.ui

import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.DeviceSensorManager
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.StateReducer
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceSensorManager: DeviceSensorManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val visionRepository: VisionRepository,
    private val reducer: StateReducer,
    val overlayRenderer: OverlayRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    private val _singleEvent = MutableStateFlow<SingleEvent<*>?>(null)
    val singleEvent: StateFlow<SingleEvent<* >?> = _singleEvent.asStateFlow()

    val visionAnalyzer = VisionAnalyzer(visionRepository)

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                val initialState = reducer.reduce(MainScreenEvent.LoadUserSettings(prefs), _uiState.value)
                _uiState.value = initialState
            }
        }
        viewModelScope.launch {
            deviceSensorManager.orientationData
                .sample(33)
                .collect { onEvent(MainScreenEvent.FullOrientationChanged(it)) }
        }
        viewModelScope.launch {
            visionRepository.visionDataFlow
                .sample(100)
                .collect { onEvent(MainScreenEvent.CvDataUpdated(it)) }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        val newState = reducer.reduce(event, _uiState.value)
        _uiState.value = newState

        if (event is MainScreenEvent.ShowToast) {
            _singleEvent.value = SingleEvent.ShowToast(event.message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        deviceSensorManager.stopSensorUpdates()
    }
}