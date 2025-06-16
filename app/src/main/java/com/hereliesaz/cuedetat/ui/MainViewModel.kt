// app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt
package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.UpdateChecker
import com.hereliesaz.cuedetat.data.UpdateResult
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.domain.StateReducer
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.view.model.TableSize
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SingleEvent
import com.hereliesaz.cuedetat.view.state.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val updateChecker: UpdateChecker,
    private val application: Application,
    private val updateStateUseCase: UpdateStateUseCase,
    private val stateReducer: StateReducer,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val graphicsCamera = Camera()
    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)
    private var warningIndex = 0

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _singleEvent = MutableStateFlow<SingleEvent?>(null)
    val singleEvent = _singleEvent.asStateFlow()

    init {
        // Start listening to sensors
        sensorRepository.pitchAngleFlow
            .onEach { onEvent(MainScreenEvent.PitchAngleChanged(it)) }
            .launchIn(viewModelScope)

        // Load user preferences
        viewModelScope.launch {
            val initialSize = userPreferencesRepository.tableSize.first()
            onEvent(MainScreenEvent.InitialTableSizeLoaded(initialSize.name))
        }
    }

    fun onEvent(event: MainScreenEvent) {
        if (event !is MainScreenEvent.PitchAngleChanged) {
            Log.d("EVENT_DEBUG", "ViewModel onEvent: $event")
        }

        // Handle side-effects and one-off events first
        when (event) {
            is MainScreenEvent.CheckForUpdate -> checkForUpdate()
            is MainScreenEvent.ViewArt -> _singleEvent.value =
                SingleEvent.OpenUrl("https://instagram.com/hereliesaz")

            is MainScreenEvent.SingleEventConsumed -> _singleEvent.value = null
            is MainScreenEvent.ToastShown -> _toastMessage.value = null
            is MainScreenEvent.ThemeChanged -> _uiState.value =
                _uiState.value.copy(dynamicColorScheme = event.scheme)
            is MainScreenEvent.ChangeTableSize -> {
                viewModelScope.launch {
                    val nextSize = _uiState.value.poolTable?.size?.getNext() ?: TableSize.SEVEN_FOOT
                    userPreferencesRepository.saveTableSize(nextSize)
                }
                updateState(event) // Also update the state
            }
            else -> updateState(event)
        }
    }

    private fun updateState(event: MainScreenEvent) {
        val oldState = _uiState.value
        val updatedState = stateReducer.reduce(oldState, event)
        var finalState = updateStateUseCase(updatedState, graphicsCamera)

        if (finalState.isImpossibleShot) {
            val text = if (!oldState.isImpossibleShot) {
                val nextWarning = insultingWarnings[warningIndex]
                warningIndex = (warningIndex + 1) % insultingWarnings.size
                nextWarning
            } else {
                oldState.warningText ?: insultingWarnings[warningIndex]
            }
            finalState = finalState.copy(warningText = text)
        } else {
            finalState = finalState.copy(warningText = null)
        }
        _uiState.value = finalState
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val result = updateChecker.checkForUpdate()
            _toastMessage.value = when (result) {
                is UpdateResult.UpdateAvailable -> ToastMessage.StringResource(
                    R.string.update_available,
                    listOf(result.latestVersion)
                )
                is UpdateResult.UpToDate -> ToastMessage.StringResource(R.string.update_no_new_release)
                is UpdateResult.CheckFailed -> ToastMessage.PlainText(result.reason)
            }
        }
    }
}
