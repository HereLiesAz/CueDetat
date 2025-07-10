// app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt
package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.UpdateChecker
import com.hereliesaz.cuedetat.data.UpdateResult
import com.hereliesaz.cuedetat.domain.StateReducer
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SingleEvent
import com.hereliesaz.cuedetat.view.state.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val GESTURE_TAG = "GestureDebug"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val updateChecker: UpdateChecker,
    application: Application,
    private val updateStateUseCase: UpdateStateUseCase,
    private val stateReducer: StateReducer
) : ViewModel() {

    private val graphicsCamera = Camera()
    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)
    private var warningIndex = 0

    private val _uiState = MutableStateFlow(OverlayState(appControlColorScheme = darkColorScheme()))
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _singleEvent = MutableStateFlow<SingleEvent?>(null)
    val singleEvent = _singleEvent.asStateFlow()

    init {
        sensorRepository.fullOrientationFlow
            .onEach { orientation -> onEvent(MainScreenEvent.FullOrientationChanged(orientation)) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MainScreenEvent) {
        // Events that trigger a single state change or action
        when (event) {
            is MainScreenEvent.CheckForUpdate -> checkForUpdate()
            is MainScreenEvent.ViewArt -> _singleEvent.value = SingleEvent.OpenUrl("https://instagram.com/hereliesaz")
            is MainScreenEvent.ShowDonationOptions -> _singleEvent.value = SingleEvent.ShowDonationDialog
            is MainScreenEvent.SingleEventConsumed -> _singleEvent.value = null
            is MainScreenEvent.ToastShown -> _toastMessage.value = null
            else -> {
                // For all other events, run the main state update logic
                updateState(event)
            }
        }
    }

    private fun updateState(event: MainScreenEvent) {
        val currentState = _uiState.value

        // Convert screen-space gestures to logical-space gestures before reducing
        val logicalEvent = when (event) {
            is MainScreenEvent.Drag -> {
                if (currentState.hasInverseMatrix) {
                    val logicalPrev = Perspective.screenToLogical(event.previousPosition, currentState.inversePitchMatrix)
                    val logicalCurr = Perspective.screenToLogical(event.currentPosition, currentState.inversePitchMatrix)
                    val logicalDelta = PointF(logicalCurr.x - logicalPrev.x, logicalCurr.y - logicalPrev.y)
                    val screenDelta = Offset(event.currentPosition.x - event.previousPosition.x, event.currentPosition.y - event.previousPosition.y)
                    MainScreenEvent.LogicalDragApplied(logicalDelta, screenDelta)
                } else {
                    event // Pass original event if no matrix
                }
            }
            is MainScreenEvent.ScreenGestureStarted -> {
                if (currentState.hasInverseMatrix) {
                    val logicalPoint = Perspective.screenToLogical(event.position, currentState.inversePitchMatrix)
                    MainScreenEvent.LogicalGestureStarted(logicalPoint)
                } else {
                    event // Pass original event if no matrix
                }
            }
            else -> event
        }

        // Reduce the state based on the (potentially translated) event
        val stateFromReducer = stateReducer.reduce(currentState, logicalEvent)

        // Calculate all derived geometric properties
        var finalState = updateStateUseCase(stateFromReducer, graphicsCamera)

        // Handle final state adjustments that happen after a gesture ends
        if (event is MainScreenEvent.GestureEnded) {
            val warningText = if (!finalState.isBankingMode && finalState.isImpossibleShot) {
                insultingWarnings[warningIndex].also {
                    warningIndex = (warningIndex + 1) % insultingWarnings.size
                }
            } else {
                null
            }
            finalState = finalState.copy(warningText = warningText)
        } else if (event !is MainScreenEvent.ScreenGestureStarted && event !is MainScreenEvent.LogicalGestureStarted) {
            if (finalState.warningText != null && (!finalState.isImpossibleShot || finalState.isBankingMode)) {
                finalState = finalState.copy(warningText = null)
            }
        }

        _uiState.value = finalState
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val result = updateChecker.checkForUpdate()
            _toastMessage.value = when (result) {
                is UpdateResult.UpdateAvailable -> ToastMessage.StringResource(R.string.update_available, listOf(result.latestVersion))
                is UpdateResult.UpToDate -> ToastMessage.StringResource(R.string.update_no_new_release)
                is UpdateResult.CheckFailed -> ToastMessage.PlainText(result.reason)
            }
        }
    }
}