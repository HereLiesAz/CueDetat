// hereliesaz/cuedetat/CueDetat-CueDetatLite/app/src/main/java/com/hereliesaz/cuedetatlite/ui/MainViewModel.kt
package com.hereliesaz.cuedetatlite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetatlite.data.GithubRepository
import com.hereliesaz.cuedetatlite.domain.StateReducer
import com.hereliesaz.cuedetatlite.domain.UpdateStateUseCase
import com.hereliesaz.cuedetatlite.domain.WarningManager
import com.hereliesaz.cuedetatlite.utils.SingleEvent
import com.hereliesaz.cuedetatlite.utils.ToastMessage
import com.hereliesaz.cuedetatlite.view.state.OverlayState
import com.hereliesaz.cuedetatlite.view.state.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stateReducer: StateReducer,
    private val warningManager: WarningManager,
    private val updateStateUseCase: UpdateStateUseCase,
    private val githubRepository: GithubRepository
) : ViewModel() {

    sealed class UiEvent {
        data class OpenUrl(val url: String) : UiEvent()
        data class ShowUpdateAvailable(val url: String) : UiEvent()
    }

    private val _screenState = MutableStateFlow(ScreenState())

    private val _overlayState = MutableStateFlow<OverlayState?>(null)
    val overlayState = _overlayState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _events = MutableStateFlow<SingleEvent<UiEvent>?>(null)
    val events = _events.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow<SingleEvent<String>?>(null)
    val isUpdateAvailable = _isUpdateAvailable.asStateFlow()

    val kineticWarning = warningManager.kineticWarning.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        viewModelScope.launch {
            combine(
                _screenState,
                stateReducer.pitchMatrix,
                stateReducer.luminance,
                stateReducer.isForceLightMode
            ) { screenState, pitchMatrix, luminance, isForceLightMode ->
                OverlayState(screenState, pitchMatrix, luminance, isForceLightMode)
            }.collect {
                _overlayState.value = it
            }
        }
        checkForUpdates()
    }

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.OnTouch -> {
                _screenState.value = stateReducer.onTouch(event.x, event.y, _screenState.value)
            }
            is MainScreenEvent.OnScale -> {
                _screenState.value = stateReducer.onScale(event.factor, _screenState.value)
            }
            is MainScreenEvent.OnTableResize -> {
                _screenState.value = stateReducer.onTableResize(event.width, event.height, _screenState.value)
            }
            is MainScreenEvent.OnForceLightMode -> stateReducer.onForceLightMode(event.isLightMode)
            is MainScreenEvent.OnLuminanceChange -> stateReducer.onLuminanceChange(event.value)
            is MainScreenEvent.OnUndo -> _screenState.value = stateReducer.onUndo(_screenState.value)
            is MainScreenEvent.OnRedo -> _screenState.value = stateReducer.onRedo(_screenState.value)
            is MainScreenEvent.OnJumpShot -> _screenState.value = stateReducer.onJumpShot(_screenState.value)
            is MainScreenEvent.OnUpdate -> updateState(event.permissionGranted)
            is MainScreenEvent.OnUpdateDismissed -> _isUpdateAvailable.value = null
            is MainScreenEvent.OnDownloadClicked -> {
                _isUpdateAvailable.value?.peekContent()?.let { url ->
                    _events.value = SingleEvent(UiEvent.OpenUrl(url))
                }
            }
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            githubRepository.getLatestReleaseUrl().onSuccess {
                _isUpdateAvailable.value = SingleEvent(it)
            }
        }
    }

    private fun updateState(permissionGranted: Boolean) {
        viewModelScope.launch {
            if (permissionGranted) {
                _toastMessage.value = ToastMessage.Text("Updating state...")
                updateStateUseCase(_screenState.value)
                _toastMessage.value = ToastMessage.Text("State updated")
            } else {
                _toastMessage.value = ToastMessage.Text("Permission denied")
            }
        }
    }
}
