// app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt
package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.UpdateChecker
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
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val updateChecker: UpdateChecker,
    private val application: Application,
    private val updateStateUseCase: UpdateStateUseCase,
    private val stateReducer: StateReducer
) : ViewModel() {

    private val graphicsCamera =
        Camera() // Consider making this local in UpdateStateUseCase if no other use
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
        sensorRepository.pitchAngleFlow
            .onEach { onEvent(MainScreenEvent.PitchAngleChanged(it)) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MainScreenEvent) {
        // Handle screen-to-logical conversions before passing to reducer for some events
        when (event) {
            is MainScreenEvent.ActualCueBallMoved -> {
                if (_uiState.value.hasInverseMatrix) {
                    val logicalPos = Perspective.screenToLogical(
                        event.position,
                        _uiState.value.inversePitchMatrix
                    )
                    updateContinuousState(
                        MainScreenEvent.UpdateLogicalActualCueBallPosition(
                            logicalPos
                        )
                    )
                }
                return // Consume, internal event dispatched
            }

            is MainScreenEvent.UnitMoved -> { // For ProtractorUnit
                if (_uiState.value.hasInverseMatrix) {
                    val logicalPos = Perspective.screenToLogical(
                        event.position,
                        _uiState.value.inversePitchMatrix
                    )
                    updateContinuousState(MainScreenEvent.UpdateLogicalUnitPosition(logicalPos))
                }
                return // Consume
            }

            is MainScreenEvent.BankingAimTargetDragged -> {
                if (_uiState.value.hasInverseMatrix) {
                    val logicalTarget = Perspective.screenToLogical(
                        event.screenPoint,
                        _uiState.value.inversePitchMatrix
                    )
                    updateContinuousState(
                        MainScreenEvent.UpdateLogicalBankingAimTarget(
                            logicalTarget
                        )
                    )
                }
                return // Consume
            }
            // --- Events that directly update state or trigger actions ---
            is MainScreenEvent.CheckForUpdate -> checkForUpdate()
            is MainScreenEvent.ViewArt -> _singleEvent.value =
                SingleEvent.OpenUrl("https://instagram.com/hereliesaz")

            is MainScreenEvent.ShowDonationOptions -> _singleEvent.value =
                SingleEvent.ShowDonationDialog

            is MainScreenEvent.FeatureComingSoon -> _toastMessage.value =
                ToastMessage.PlainText("Feature coming soon!")
            is MainScreenEvent.SingleEventConsumed -> _singleEvent.value = null
            is MainScreenEvent.ToastShown -> _toastMessage.value = null
            is MainScreenEvent.ThemeChanged -> { // Direct state copy
                _uiState.value = _uiState.value.copy(dynamicColorScheme = event.scheme)
            }
            is MainScreenEvent.GestureStarted -> {
                _uiState.value = _uiState.value.copy(warningText = null)
                // Do not return, let updateContinuousState run if needed for other reasons
            }
            // For other events, pass them to updateContinuousState
            else -> updateContinuousState(event)
        }
    }

    private fun updateContinuousState(event: MainScreenEvent) {
        val oldState = _uiState.value
        val stateAfterReducer = stateReducer.reduce(oldState, event)
        val finalGeometricState = updateStateUseCase(stateAfterReducer, graphicsCamera)

        var newWarningText = finalGeometricState.warningText

        if (event is MainScreenEvent.GestureEnded) {
            if (!finalGeometricState.isBankingMode && finalGeometricState.isImpossibleShot) {
                newWarningText = insultingWarnings[warningIndex]
                warningIndex = (warningIndex + 1) % insultingWarnings.size
            } else {
                newWarningText = null
            }
        } else if (event !is MainScreenEvent.GestureStarted) {
            if (oldState.warningText != null && finalGeometricState.isImpossibleShot && !finalGeometricState.isBankingMode) {
                newWarningText = oldState.warningText
            } else if (!finalGeometricState.isImpossibleShot || finalGeometricState.isBankingMode) {
                newWarningText = null
            }
        }
        _uiState.value = finalGeometricState.copy(warningText = newWarningText)
    }

    private fun checkForUpdate() { /* ... no change ... */
    }
}