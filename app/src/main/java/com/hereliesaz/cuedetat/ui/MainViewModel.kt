// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt

package com.hereliesaz.cuedetat.ui

import android.app.Application
import android.graphics.Camera
import android.graphics.PointF
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CalculateBankShot
import com.hereliesaz.cuedetat.domain.StateReducer
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.InteractionMode
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
    private val githubRepository: GithubRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    visionRepository: VisionRepository,
    application: Application,
    private val updateStateUseCase: UpdateStateUseCase,
    private val calculateBankShotUseCase: CalculateBankShot,
    private val stateReducer: StateReducer
) : ViewModel() {

    val visionAnalyzer = VisionAnalyzer(visionRepository)

    private val graphicsCamera = Camera()
    private val insultingWarnings: Array<String> =
        application.resources.getStringArray(R.array.insulting_warnings)
    private var warningIndex = 0

    private val _uiState = MutableStateFlow(
        OverlayState(
            appControlColorScheme = darkColorScheme(),
            distanceUnit = userPreferencesRepository.getDistanceUnit(),
            tableSize = userPreferencesRepository.getTableSize(),
            houghP1 = userPreferencesRepository.getCvHoughP1(),
            houghP2 = userPreferencesRepository.getCvHoughP2(),
            cannyThreshold1 = userPreferencesRepository.getCvCannyT1(),
            cannyThreshold2 = userPreferencesRepository.getCvCannyT2()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _singleEvent = MutableStateFlow<SingleEvent?>(null)
    val singleEvent = _singleEvent.asStateFlow()

    init {
        sensorRepository.fullOrientationFlow
            .onEach { orientation -> onEvent(MainScreenEvent.FullOrientationChanged(orientation)) }
            .launchIn(viewModelScope)

        visionRepository.visionDataFlow
            .onEach { visionData -> onEvent(MainScreenEvent.CvDataUpdated(visionData)) }
            .launchIn(viewModelScope)

        // Pass the initial state to the analyzer
        visionAnalyzer.updateUiState(_uiState.value)

        fetchLatestVersionName()
    }

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.CheckForUpdate -> _singleEvent.value = SingleEvent.OpenUrl("https://github.com/HereLiesAz/CueDetat/releases")
            is MainScreenEvent.ViewArt -> _singleEvent.value = SingleEvent.OpenUrl("https://instagram.com/hereliesaz")
            is MainScreenEvent.ShowDonationOptions -> _singleEvent.value = SingleEvent.ShowDonationDialog
            is MainScreenEvent.SingleEventConsumed -> _singleEvent.value = null
            is MainScreenEvent.ToastShown -> _toastMessage.value = null
            is MainScreenEvent.ToggleDistanceUnit -> {
                val newUnit = if (uiState.value.distanceUnit == DistanceUnit.METRIC) DistanceUnit.IMPERIAL else DistanceUnit.METRIC
                userPreferencesRepository.setDistanceUnit(newUnit)
                updateState(event)
            }
            is MainScreenEvent.SetTableSize -> {
                userPreferencesRepository.setTableSize(event.size)
                updateState(event)
            }
            is MainScreenEvent.CycleTableSize -> {
                val newSize = uiState.value.tableSize.next()
                userPreferencesRepository.setTableSize(newSize)
                updateState(event)
            }
            // Persist CV param changes
            is MainScreenEvent.UpdateHoughP1 -> userPreferencesRepository.setCvHoughP1(event.value)
            is MainScreenEvent.UpdateHoughP2 -> userPreferencesRepository.setCvHoughP2(event.value)
            is MainScreenEvent.UpdateCannyT1 -> userPreferencesRepository.setCvCannyT1(event.value)
            is MainScreenEvent.UpdateCannyT2 -> userPreferencesRepository.setCvCannyT2(event.value)

            else -> {
                // For all other events, just proceed to update the state
            }
        }
        updateState(event) // Update state for all events, including the ones handled above
    }

    private fun fetchLatestVersionName() {
        viewModelScope.launch {
            val latestVersion = githubRepository.getLatestVersionName()
            _uiState.value = _uiState.value.copy(latestVersionName = latestVersion)
        }
    }

    private fun updateState(event: MainScreenEvent) {
        val currentState = _uiState.value

        val logicalEvent = when (event) {
            is MainScreenEvent.Drag -> {
                val logicalPrev = Perspective.screenToLogical(event.previousPosition, currentState.inversePitchMatrix)
                val logicalCurr = Perspective.screenToLogical(event.currentPosition, currentState.inversePitchMatrix)
                val screenDelta = Offset(event.currentPosition.x - event.previousPosition.x, event.currentPosition.y - event.previousPosition.y)

                if (currentState.interactionMode == InteractionMode.MOVING_SPIN_CONTROL) {
                    return onEvent(MainScreenEvent.DragSpinControl(PointF(screenDelta.x, screenDelta.y)))
                }

                if (currentState.interactionMode == InteractionMode.AIMING_BANK_SHOT && currentState.hasInverseMatrix) {
                    MainScreenEvent.AimBankShot(logicalCurr)
                } else if (currentState.hasInverseMatrix) {
                    val logicalDelta = PointF(logicalCurr.x - logicalPrev.x, logicalCurr.y - logicalPrev.y)
                    MainScreenEvent.LogicalDragApplied(logicalDelta, screenDelta)
                } else {
                    event
                }
            }
            is MainScreenEvent.ScreenGestureStarted -> {
                if (currentState.hasInverseMatrix) {
                    val logicalPoint = Perspective.screenToLogical(event.position, currentState.inversePitchMatrix)
                    MainScreenEvent.LogicalGestureStarted(logicalPoint, Offset(event.position.x, event.position.y))
                } else {
                    event
                }
            }
            else -> event
        }

        val stateFromReducer = stateReducer.reduce(currentState, logicalEvent)
        var derivedState = updateStateUseCase(stateFromReducer, graphicsCamera)

        if (derivedState.isBankingMode) {
            val bankShotResult = calculateBankShotUseCase(derivedState)
            derivedState = derivedState.copy(
                bankShotPath = bankShotResult.path,
                pocketedBankShotPocketIndex = bankShotResult.pocketedPocketIndex
            )
        }

        var finalState = derivedState

        if (event is MainScreenEvent.GestureEnded) {
            val warningText = if (!finalState.isBankingMode && finalState.isImpossibleShot) {
                insultingWarnings[warningIndex].also {
                    warningIndex = (warningIndex + 1) % insultingWarnings.size
                }
            } else {
                null
            }
            finalState = finalState.copy(warningText = warningText)
        } else if (event !is MainScreenEvent.ScreenGestureStarted) {
            if (finalState.warningText != null && (!finalState.isImpossibleShot || finalState.isBankingMode)) {
                finalState = finalState.copy(warningText = null)
            }
        }

        visionAnalyzer.updateUiState(finalState)
        _uiState.value = finalState
    }
}