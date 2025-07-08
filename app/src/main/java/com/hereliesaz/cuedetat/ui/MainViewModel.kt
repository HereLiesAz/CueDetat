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
    private val application: Application,
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
        when (event) {
            is MainScreenEvent.ThemeChanged -> {
                _uiState.value = _uiState.value.copy(appControlColorScheme = event.scheme)
                return
            }
            is MainScreenEvent.CheckForUpdate -> checkForUpdate()
            is MainScreenEvent.ViewArt -> _singleEvent.value = SingleEvent.OpenUrl("https://instagram.com/hereliesaz")
            is MainScreenEvent.ShowDonationOptions -> _singleEvent.value = SingleEvent.ShowDonationDialog
            is MainScreenEvent.SingleEventConsumed -> _singleEvent.value = null
            is MainScreenEvent.ToastShown -> _toastMessage.value = null

            is MainScreenEvent.ScreenGestureStarted -> {
                Log.d(GESTURE_TAG, "VIEWMODEL: Received ScreenGestureStarted.")
                if (_uiState.value.hasInverseMatrix) {
                    val logicalPoint = Perspective.screenToLogical(event.position, _uiState.value.inversePitchMatrix)
                    Log.d(GESTURE_TAG, "VIEWMODEL: Converted to LogicalGestureStarted at $logicalPoint. Dispatching to reducer.")
                    updateContinuousState(MainScreenEvent.LogicalGestureStarted(logicalPoint))
                } else {
                    Log.w(GESTURE_TAG, "VIEWMODEL: No inverse matrix, dropping gesture start.")
                }
            }
            is MainScreenEvent.Drag -> {
                if (_uiState.value.hasInverseMatrix) {
                    // Convert start and end points of drag to logical space
                    val logicalPrev = Perspective.screenToLogical(event.previousPosition, _uiState.value.inversePitchMatrix)
                    val logicalCurr = Perspective.screenToLogical(event.currentPosition, _uiState.value.inversePitchMatrix)
                    // Calculate delta in logical space
                    val logicalDelta = PointF(logicalCurr.x - logicalPrev.x, logicalCurr.y - logicalPrev.y)

                    // Calculate screen delta for rotation
                    val screenDelta = Offset(
                        event.currentPosition.x - event.previousPosition.x,
                        event.currentPosition.y - event.previousPosition.y
                    )

                    Log.d(GESTURE_TAG, "VIEWMODEL: Converted screen drag to logicalDelta $logicalDelta. Dispatching to reducer.")
                    updateContinuousState(MainScreenEvent.LogicalDragApplied(logicalDelta, screenDelta))
                }
            }

            else -> updateContinuousState(event)
        }
    }

    private fun updateContinuousState(event: MainScreenEvent) {
        val oldState = _uiState.value
        val stateFromReducer = stateReducer.reduce(oldState, event)
        val finalGeometricState = updateStateUseCase(stateFromReducer, graphicsCamera)

        var newWarningText = finalGeometricState.warningText

        if (event is MainScreenEvent.GestureEnded) {
            if (!finalGeometricState.isBankingMode && finalGeometricState.isImpossibleShot) {
                newWarningText = insultingWarnings[warningIndex]
                warningIndex = (warningIndex + 1) % insultingWarnings.size
            } else {
                newWarningText = null
            }
        } else if (event !is MainScreenEvent.ScreenGestureStarted && event !is MainScreenEvent.LogicalGestureStarted) {
            if (oldState.warningText != null && finalGeometricState.isImpossibleShot && !finalGeometricState.isBankingMode) {
                newWarningText = oldState.warningText
            } else if (!finalGeometricState.isImpossibleShot || finalGeometricState.isBankingMode) {
                newWarningText = null
            }
        } else {
            newWarningText = null
        }

        _uiState.value = finalGeometricState.copy(
            warningText = newWarningText,
            appControlColorScheme = oldState.appControlColorScheme
        )
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