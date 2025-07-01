package com.hereliesaz.cuedetatlite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetatlite.data.SensorRepository
import com.hereliesaz.cuedetatlite.data.UpdateChecker
import com.hereliesaz.cuedetatlite.data.UpdateResult
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainScreenUiState(
    val isUpdateAvailable: Boolean = false,
    val isForceLightMode: Boolean? = null,
    val showLuminanceDialog: Boolean = false,
    val warningMessage: String? = null
)

class MainViewModel(
    private val stateReducer: StateReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    val sensorRepository: SensorRepository, // Make public
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val _overlayState = MutableStateFlow(OverlayState())
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    private val _uiEvents = Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    // FIX: Combine with the new updateResult StateFlow from UpdateChecker
    val uiState: StateFlow<MainScreenUiState> =
        combine(
            _overlayState,
            updateChecker.updateResult
        ) { overlay, updateResult ->
            MainScreenUiState(
                isUpdateAvailable = updateResult is UpdateResult.UpdateAvailable,
                isForceLightMode = overlay.isForceLightMode,
                showLuminanceDialog = overlay.showLuminanceDialog,
                warningMessage = overlay.screenState.warningText?.name
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState())


    init {
        viewModelScope.launch {
            sensorRepository.getOrientationFlow().collect {
                // This would be where you update perspective based on sensor data
            }
        }
        checkForUpdates()
    }

    fun onEvent(event: MainScreenEvent) {
        viewModelScope.launch {
            val currentState = _overlayState.value
            val newOverlayState = when (event) {
                // Events that modify ScreenState via the reducer
                is MainScreenEvent.BallMoved, is MainScreenEvent.BallRadiusChanged, is MainScreenEvent.Reset -> {
                    val newScreenState = stateReducer.reduce(currentState.screenState, event)
                    currentState.copy(screenState = newScreenState)
                }
                // Events that modify OverlayState directly
                is MainScreenEvent.ZoomSliderChanged -> currentState.copy(zoomSliderPosition = event.position)
                is MainScreenEvent.ForceLightMode -> currentState.copy(isForceLightMode = event.enabled)
                is MainScreenEvent.LuminanceChanged -> currentState.copy(luminanceAdjustment = event.value)
                is MainScreenEvent.ShowLuminanceDialog -> currentState.copy(showLuminanceDialog = true)
                is MainScreenEvent.DismissLuminanceDialog -> currentState.copy(showLuminanceDialog = false)
                is MainScreenEvent.ToggleBankingMode -> currentState.copy(isBankingMode = !currentState.isBankingMode)
                is MainScreenEvent.BankingAimTargetChanged -> currentState.copy(bankingAimTarget = event.position)
                // ADDED: Handle the new event
                is MainScreenEvent.ToggleActualCueBall -> {
                    val newScreenState = currentState.screenState.copy(showActualCueBall = !currentState.screenState.showActualCueBall)
                    currentState.copy(screenState = newScreenState)
                }

                // Events that trigger a one-off UI action
                is MainScreenEvent.DownloadUpdate -> {
                    // FIX: Call the correct method
                    updateChecker.getLatestReleaseUrl()?.let {
                        _uiEvents.send(UiEvent.OpenUrl(it))
                    }
                    currentState // No state change from this event itself
                }
                // ADDED: Handle meta events
                is MainScreenEvent.ViewArt -> {
                    _uiEvents.send(UiEvent.OpenUrl("https://herelies.art"))
                    currentState
                }
                is MainScreenEvent.ShowDonationOptions -> {
                    _uiEvents.send(UiEvent.OpenUrl("https://www.buymeacoffee.com/hereliesaz"))
                    currentState
                }
                is MainScreenEvent.CheckForUpdate -> {
                    checkForUpdates() // Re-trigger the check
                    currentState
                }
                // Events handled by other flows or with no state change
                is MainScreenEvent.DismissUpdateDialog -> currentState
                else -> currentState
            }
            _overlayState.value = newOverlayState
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            updateChecker.checkForUpdate()
        }
    }
}