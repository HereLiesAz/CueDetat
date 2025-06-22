package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.PointF
import androidx.compose.material3.darkColorScheme // For default state initialization
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

    // Initialize OverlayState with a default appControlColorScheme
    private val _uiState = MutableStateFlow(OverlayState(appControlColorScheme = darkColorScheme()))
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
        when (event) {
            is MainScreenEvent.ThemeChanged -> {
                // This event is now for the APP's UI controls' theme.
                _uiState.value =
                    _uiState.value.copy(appControlColorScheme = event.scheme) // Corrected field name
                return
            }
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
                return
            }

            is MainScreenEvent.UnitMoved -> {
                if (_uiState.value.hasInverseMatrix) {
                    val logicalPos = Perspective.screenToLogical(
                        event.position,
                        _uiState.value.inversePitchMatrix
                    )
                    updateContinuousState(MainScreenEvent.UpdateLogicalUnitPosition(logicalPos))
                }
                return
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
                return
            }
            is MainScreenEvent.CheckForUpdate -> checkForUpdate()
            is MainScreenEvent.ViewArt -> _singleEvent.value =
                SingleEvent.OpenUrl("https://instagram.com/hereliesaz")

            is MainScreenEvent.ShowDonationOptions -> _singleEvent.value =
                SingleEvent.ShowDonationDialog

            is MainScreenEvent.FeatureComingSoon -> _toastMessage.value =
                ToastMessage.PlainText("Feature coming soon!")
            is MainScreenEvent.SingleEventConsumed -> _singleEvent.value = null
            is MainScreenEvent.ToastShown -> _toastMessage.value = null
            is MainScreenEvent.GestureStarted -> {
                _uiState.value = _uiState.value.copy(warningText = null)
                // Fall through to updateContinuousState for other potential logic
            }

            else -> Unit // Let updateContinuousState handle the rest
        }
        // For events not handled by a return, proceed to updateContinuousState
        // This ensures events like GestureStarted and GestureEnded also flow through the main update path.
        updateContinuousState(event)
    }

    private fun updateContinuousState(event: MainScreenEvent) {
        val oldState = _uiState.value
        // Pass the current appControlColorScheme from oldState to the reducer
        // The reducer itself does not modify appControlColorScheme; it's set by ThemeChanged event directly.
        val stateFromReducer = stateReducer.reduce(oldState, event)
        val finalGeometricState = updateStateUseCase(stateFromReducer, graphicsCamera)

        var newWarningText =
            finalGeometricState.warningText // Start with what reducer/usecase might have set

        if (event is MainScreenEvent.GestureEnded) {
            if (!finalGeometricState.isBankingMode && finalGeometricState.isImpossibleShot) {
                newWarningText = insultingWarnings[warningIndex]
                warningIndex = (warningIndex + 1) % insultingWarnings.size
            } else {
                newWarningText = null // Clear warning if not impossible or in banking mode
            }
        } else if (event !is MainScreenEvent.GestureStarted) {
            // For continuous events, preserve existing warning if shot is still impossible & not banking
            if (oldState.warningText != null && finalGeometricState.isImpossibleShot && !finalGeometricState.isBankingMode) {
                newWarningText = oldState.warningText
            } else if (!finalGeometricState.isImpossibleShot || finalGeometricState.isBankingMode) {
                // Clear warning if shot becomes possible or if we enter banking mode
                newWarningText = null
            }
        }
        // If event is GestureStarted, warningText was already nulled in the onEvent handler for immediate UI feedback.

        // Ensure appControlColorScheme from the oldState is preserved, as reducer doesn't touch it.
        // The ThemeChanged event is the sole source for updating appControlColorScheme.
        _uiState.value = finalGeometricState.copy(
            warningText = newWarningText,
            appControlColorScheme = oldState.appControlColorScheme
        )
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