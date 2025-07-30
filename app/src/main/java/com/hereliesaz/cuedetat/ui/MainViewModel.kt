package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.ShakeDetector
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CueDetatAction
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.domain.UpdateType
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.domain.stateReducer
import com.hereliesaz.cuedetat.ui.hatemode.HaterEvent
import com.hereliesaz.cuedetat.ui.hatemode.HaterViewModel
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val reducerUtils: ReducerUtils,
    private val gestureReducer: GestureReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    private val sensorRepository: SensorRepository,
    private val githubRepository: GithubRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val shakeDetector: ShakeDetector,
    visionRepository: VisionRepository,
    private val haterViewModel: HaterViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(CueDetatState())
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent?>()
    val singleEvent = _singleEvent.asSharedFlow()

    val visionAnalyzer: VisionAnalyzer = VisionAnalyzer(visionRepository)

    init {
        // Restore previous state or initialize
        viewModelScope.launch {
            val savedState = userPreferencesRepository.stateFlow.first()
            val initialState = savedState ?: CueDetatState()
            processAndEmitState(initialState, UpdateType.FULL)
        }

        // Collect sensor data
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collect { orientation ->
                onEvent(CueDetatAction.FullOrientationChanged(orientation))
                if (_uiState.value.experienceMode == ExperienceMode.HATER) {
                    haterViewModel.onEvent(
                        HaterEvent.SensorChanged(
                            orientation.roll,
                            orientation.pitch
                        )
                    )
                }
            }
        }

        // Collect shake events for Hater Mode
        viewModelScope.launch {
            shakeDetector.shakeFlow.collect {
                if (_uiState.value.experienceMode == ExperienceMode.HATER) {
                    haterViewModel.onEvent(HaterEvent.ScreenTapped)
                }
            }
        }

        // Forward vision data to the reducer
        viewModelScope.launch {
            visionRepository.visionDataFlow.collect { visionData ->
                onEvent(CueDetatAction.CvDataUpdated(visionData))
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Convert screen gestures to logical gestures before reducing
            val logicalEvent = when (event) {
                is CueDetatAction.ScreenGestureStarted -> {
                    val logicalPoint = Perspective.screenToLogical(
                        event.position,
                        currentState.inversePitchMatrix ?: return@launch
                    )
                    MainScreenEvent.LogicalGestureStarted(
                        logicalPoint,
                        Offset(event.position.x, event.position.y)
                    )
                }

                is CueDetatAction.Drag -> {
                    val prevLogical = Perspective.screenToLogical(
                        event.previousPosition,
                        currentState.inversePitchMatrix ?: return@launch
                    )
                    val currLogical = Perspective.screenToLogical(
                        event.currentPosition,
                        currentState.inversePitchMatrix ?: return@launch
                    )
                    val screenDelta = Offset(
                        event.currentPosition.x - event.previousPosition.x,
                        event.currentPosition.y - event.previousPosition.y
                    )
                    MainScreenEvent.LogicalDragApplied(prevLogical, currLogical, screenDelta)
                }

                else -> event
            }

            // Reduce the state
            val reducedState =
                stateReducer(currentState, logicalEvent, reducerUtils, gestureReducer)

            // Determine the required update type
            val updateType = determineUpdateType(currentState, reducedState, logicalEvent)

            // Run use case for derived state and emit
            processAndEmitState(reducedState, updateType)

            // Handle single events that don't change state
            handleSingleEvents(logicalEvent)
        }
    }

    private fun processAndEmitState(state: CueDetatState, type: UpdateType) {
        val derivedState = updateStateUseCase(state, type)
        _uiState.value = derivedState
        visionAnalyzer.updateUiState(derivedState)

        // Save state to disk if it has changed meaningfully
        if (type != UpdateType.SPIN_ONLY) { // Don't save for every little spin change
            viewModelScope.launch {
                userPreferencesRepository.saveState(derivedState)
            }
        }
    }

    private fun determineUpdateType(
        oldState: CueDetatState,
        newState: CueDetatState,
        event: MainScreenEvent
    ): UpdateType {
        return when (event) {
            is CueDetatAction.SizeChanged, is CueDetatAction.ZoomScaleChanged, is CueDetatAction.ZoomSliderChanged,
            is CueDetatAction.PanView, is CueDetatAction.FullOrientationChanged, is CueDetatAction.TableRotationChanged,
            is CueDetatAction.TableRotationApplied, is CueDetatAction.SetExperienceMode, is CueDetatAction.ToggleBankingMode,
            is CueDetatAction.SetTableSize, is CueDetatAction.RestoreState -> UpdateType.FULL

            is CueDetatAction.Reset, is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied,
            is MainScreenEvent.GestureEnded, is CueDetatAction.AddObstacleBall -> UpdateType.AIMING

            is CueDetatAction.SpinApplied -> UpdateType.SPIN_ONLY

            else -> UpdateType.AIMING // Default to aiming for most other toggles
        }
    }

    private fun handleSingleEvents(event: MainScreenEvent) {
        if (event is CueDetatAction) {
            viewModelScope.launch {
                when (event) {
                    is CueDetatAction.CheckForUpdate -> {
                        githubRepository.getLatestVersionName()
                        // This logic would be expanded to show a dialog
                    }

                    is CueDetatAction.ViewArt -> _singleEvent.emit(SingleEvent.OpenUrl("https://herelies.az"))
                    is CueDetatAction.ViewAboutPage -> _singleEvent.emit(SingleEvent.OpenUrl("https://github.com/HereLiesAz/CueDetat"))
                    is CueDetatAction.SendFeedback -> _singleEvent.emit(
                        SingleEvent.SendFeedbackEmail(
                            "dev@herelies.az",
                            "Cue d'Etat Feedback"
                        )
                    )

                    is CueDetatAction.SingleEventConsumed -> _singleEvent.emit(null)
                    is CueDetatAction.SetExperienceMode -> {
                        if (event.mode == ExperienceMode.HATER) {
                            _singleEvent.emit(SingleEvent.InitiateHaterMode)
                        }
                    }

                    else -> { /* Do nothing for state-changing events */
                    }
                }
            }
        }
    }
}