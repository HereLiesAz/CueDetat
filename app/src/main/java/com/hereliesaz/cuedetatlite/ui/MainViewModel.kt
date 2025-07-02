package com.hereliesaz.cuedetatlite.ui

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetatlite.data.FullOrientation
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.view.model.ProtractorUnit
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val isForceLightMode: Boolean? = null,
    val showLuminanceDialog: Boolean = false,
    val warningMessage: String? = null
)

class MainViewModel(
    private val stateReducer: StateReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    val sensorRepository: SensorRepository
) : ViewModel() {

    private val _overlayState = MutableStateFlow(OverlayState())
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    val uiState: StateFlow<MainScreenUiState> =
        _overlayState.map { overlay ->
            MainScreenUiState(
                isForceLightMode = overlay.isForceLightMode,
                showLuminanceDialog = overlay.showLuminanceDialog,
                warningMessage = overlay.screenState.warningText?.name
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState())


    init {
        viewModelScope.launch {
            sensorRepository.getOrientationFlow().collect { orientation ->
                onEvent(MainScreenEvent.OrientationChanged(orientation))
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        viewModelScope.launch {
            val currentState = _overlayState.value
            var newOverlayState = when (event) {
                is MainScreenEvent.ViewResized -> {
                    val newScreenState = currentState.screenState.copy(
                        protractorUnit = ProtractorUnit(
                            targetBall = ProtractorUnit.LogicalBall(PointF(event.width / 2f, event.height / 2f), 30f)
                        )
                    )
                    currentState.copy(viewWidth = event.width, viewHeight = event.height, screenState = newScreenState)
                }
                is MainScreenEvent.OrientationChanged -> {
                    currentState.copy(currentOrientation = event.orientation)
                }
                is MainScreenEvent.AimingAngleChanged -> {
                    val newProtractorUnit = currentState.screenState.protractorUnit.copy(aimingAngleDegrees = event.degrees)
                    val newScreenState = currentState.screenState.copy(protractorUnit = newProtractorUnit)
                    currentState.copy(screenState = newScreenState)
                }
                is MainScreenEvent.ZoomChanged -> {
                    val currentZoom = ZoomMapping.sliderToZoom(currentState.zoomSliderPosition)
                    val newZoom = (currentZoom * event.zoomFactor).coerceIn(ZoomMapping.MIN_ZOOM, ZoomMapping.MAX_ZOOM)
                    val newSliderPosition = ZoomMapping.zoomToSlider(newZoom)
                    currentState.copy(zoomSliderPosition = newSliderPosition)
                }
                is MainScreenEvent.BallMoved -> {
                    if (event.ballId == 1) { // Only handle target ball moves
                        val newProtractorUnit = currentState.screenState.protractorUnit.copy(
                            targetBall = ProtractorUnit.LogicalBall(event.position, currentState.screenState.protractorUnit.targetBall.radius)
                        )
                        val newScreenState = currentState.screenState.copy(protractorUnit = newProtractorUnit)
                        currentState.copy(screenState = newScreenState)
                    } else {
                        currentState
                    }
                }
                is MainScreenEvent.Reset -> {
                    val newScreenState = stateReducer.reduce(currentState.screenState, event)
                    currentState.copy(screenState = newScreenState, zoomSliderPosition = 50f, currentOrientation = FullOrientation(0f, 0f, 0f), anchorOrientation = null)
                }
                is MainScreenEvent.ZoomSliderChanged -> currentState.copy(zoomSliderPosition = event.position)
                is MainScreenEvent.ForceLightMode -> currentState.copy(isForceLightMode = event.enabled)
                is MainScreenEvent.LuminanceChanged -> currentState.copy(luminanceAdjustment = event.value)
                is MainScreenEvent.ShowLuminanceDialog -> currentState.copy(showLuminanceDialog = true)
                is MainScreenEvent.DismissLuminanceDialog -> currentState.copy(showLuminanceDialog = false)
                is MainScreenEvent.ToggleBankingMode -> currentState.copy(isBankingMode = !currentState.isBankingMode)
                is MainScreenEvent.BankingAimTargetChanged -> currentState.copy(bankingAimTarget = event.position)
                is MainScreenEvent.ToggleActualCueBall -> {
                    val newScreenState = currentState.screenState.copy(showActualCueBall = !currentState.screenState.showActualCueBall)
                    currentState.copy(screenState = newScreenState)
                }
                is MainScreenEvent.ViewArt -> {
                    _uiEvents.send(UiEvent.OpenUrl("https://herelies.art"))
                    currentState
                }
                is MainScreenEvent.ShowDonationOptions -> {
                    _uiEvents.send(UiEvent.OpenUrl("https://www.buymeacoffee.com/hereliesaz"))
                    currentState
                }
                else -> currentState
            }
            // Always run the result through the use case to update derived state like matrices
            newOverlayState = updateStateUseCase(newOverlayState)
            _overlayState.value = newOverlayState
        }
    }
}