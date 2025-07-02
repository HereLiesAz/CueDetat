package com.hereliesaz.cuedetatlite.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetatlite.R
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainScreenUiState(
    val isForceLightMode: Boolean? = null,
    val showLuminanceDialog: Boolean = false,
    val warningMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stateReducer: StateReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    val sensorRepository: SensorRepository,
    application: Application
) : ViewModel() {

    private val _overlayState = MutableStateFlow(OverlayState())
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val insultingWarnings: Array<String> = application.resources.getStringArray(R.array.insulting_warnings)
    private var warningIndex = 0
    private var isGestureInProgress = false

    init {
        viewModelScope.launch {
            sensorRepository.getOrientationFlow().collect { orientation ->
                onEvent(MainScreenEvent.OrientationChanged(orientation))
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        viewModelScope.launch {
            // Handle side-effects and one-off events here
            when (event) {
                is MainScreenEvent.ViewArt -> _uiEvents.send(UiEvent.OpenUrl("https://herelies.art"))
                is MainScreenEvent.ShowDonationOptions -> _uiEvents.send(UiEvent.OpenUrl("https://www.buymeacoffee.com/hereliesaz"))
                is MainScreenEvent.GestureStarted -> {
                    isGestureInProgress = true
                    _uiState.value = _uiState.value.copy(warningMessage = null)
                    return@launch // Don't process state update yet
                }
                is MainScreenEvent.GestureEnded -> {
                    isGestureInProgress = false
                    // After the gesture ends, check if a warning should be displayed
                    if (_overlayState.value.screenState.isImpossibleShot) {
                        _uiState.value = _uiState.value.copy(warningMessage = insultingWarnings[warningIndex])
                        warningIndex = (warningIndex + 1) % insultingWarnings.size
                    }
                    return@launch // Don't process state update yet
                }
                // Convert screen-space gestures to logical-space events before reducing
                is MainScreenEvent.BallMoved -> {
                    if (_overlayState.value.hasInverseMatrix) {
                        val logicalPoint = DrawingUtils.mapPoint(event.position, _overlayState.value.inversePitchMatrix)
                        val logicalEvent = MainScreenEvent.BallMoved(event.ballId, logicalPoint)
                        processStateUpdate(logicalEvent)
                    }
                    return@launch // Return to prevent double processing
                }
                is MainScreenEvent.BankingAimTargetChanged -> {
                    if(_overlayState.value.hasInverseMatrix){
                        val logicalPoint = DrawingUtils.mapPoint(event.position, _overlayState.value.inversePitchMatrix)
                        val logicalEvent = MainScreenEvent.BankingAimTargetChanged(logicalPoint)
                        processStateUpdate(logicalEvent)
                    }
                    return@launch
                }
                else -> {
                    processStateUpdate(event)
                }
            }
        }
    }

    private fun processStateUpdate(event: MainScreenEvent) {
        val currentState = _overlayState.value

        val reducedScreenState = stateReducer.reduce(currentState.screenState, event, currentState.viewWidth, currentState.viewHeight)

        var newOverlayState = currentState.copy(screenState = reducedScreenState)
        newOverlayState = when (event) {
            is MainScreenEvent.ViewResized -> newOverlayState.copy(viewWidth = event.width, viewHeight = event.height, valuesChangedSinceReset = true)
            is MainScreenEvent.OrientationChanged -> newOverlayState.copy(currentOrientation = event.orientation)
            is MainScreenEvent.ZoomChanged -> {
                val currentZoom = ZoomMapping.sliderToZoom(currentState.zoomSliderPosition)
                val newZoom = (currentZoom * event.zoomFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                val newSliderPosition = ZoomMapping.zoomToSlider(newZoom)
                newOverlayState.copy(zoomSliderPosition = newSliderPosition, valuesChangedSinceReset = true)
            }
            is MainScreenEvent.ZoomSliderChanged -> newOverlayState.copy(zoomSliderPosition = event.position, valuesChangedSinceReset = true)
            is MainScreenEvent.AimingAngleChanged -> newOverlayState.copy(screenState = newOverlayState.screenState.copy(protractorUnit = newOverlayState.screenState.protractorUnit.copy(aimingAngleDegrees = event.degrees)), valuesChangedSinceReset = true)
            is MainScreenEvent.ForceLightMode -> newOverlayState.copy(isForceLightMode = event.enabled, valuesChangedSinceReset = true)
            is MainScreenEvent.LuminanceChanged -> newOverlayState.copy(luminanceAdjustment = event.value, valuesChangedSinceReset = true)
            is MainScreenEvent.ShowLuminanceDialog -> newOverlayState.copy(showLuminanceDialog = true)
            is MainScreenEvent.DismissLuminanceDialog -> newOverlayState.copy(showLuminanceDialog = false)
            is MainScreenEvent.ToggleHelp -> newOverlayState.copy(areHelpersVisible = !currentState.areHelpersVisible)
            is MainScreenEvent.Reset -> newOverlayState.copy(zoomSliderPosition = 50f, anchorOrientation = null, valuesChangedSinceReset = false, tableRotationDegrees = 0f)
            is MainScreenEvent.TableRotationChanged -> newOverlayState.copy(tableRotationDegrees = event.degrees, valuesChangedSinceReset = true)
            else -> newOverlayState
        }

        _overlayState.value = updateStateUseCase(newOverlayState)

        if (!isGestureInProgress) {
            _uiState.value = _uiState.value.copy(
                isForceLightMode = _overlayState.value.isForceLightMode,
                showLuminanceDialog = _overlayState.value.showLuminanceDialog,
                warningMessage = if(_overlayState.value.screenState.isImpossibleShot) _uiState.value.warningMessage else null
            )
        }
    }
}