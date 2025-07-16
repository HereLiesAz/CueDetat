// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt
package com.hereliesaz.cuedetat.ui

import android.graphics.PointF
import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.DeviceSensorManager
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.*
import com.hereliesaz.cuedetat.view.renderer.OverlayRenderer
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceSensorManager: DeviceSensorManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val visionRepository: VisionRepository,
    private val reducer: Reducer,
    val overlayRenderer: OverlayRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState: StateFlow<OverlayState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

    val visionAnalyzer = VisionAnalyzer(visionRepository)

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                val initialState = reducer.reduce(MainScreenEvent.LoadUserSettings(prefs), _uiState.value)
                _uiState.value = initialState
            }
        }
        viewModelScope.launch {
            deviceSensorManager.orientationData
                .sample(33)
                .collect { onEvent(MainScreenEvent.OrientationChanged(it)) }
        }
        viewModelScope.launch {
            visionRepository.visionDataFlow
                .sample(100)
                .collect { onEvent(MainScreenEvent.VisionDataUpdated(it)) }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        val newState = reducer.reduce(event, _uiState.value)
        _uiState.value = newState

        if (event is MainScreenEvent.ShowToast) {
            _toastMessage.value = event.message
        }
    }

    override fun onCleared() {
        super.onCleared()
        deviceSensorManager.stopSensorUpdates()
    }
}