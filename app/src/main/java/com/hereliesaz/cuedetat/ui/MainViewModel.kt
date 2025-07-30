// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt

package com.hereliesaz.cuedetat.ui

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.domain.UpdateType
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.domain.stateReducer
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
    visionRepository: VisionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CueDetatState())
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent?>()
    val singleEvent = _singleEvent.asSharedFlow()

    val visionAnalyzer: VisionAnalyzer = VisionAnalyzer(visionRepository)

    init {
        viewModelScope.launch {
            val savedState = userPreferencesRepository.stateFlow.first()
            val initialState = savedState ?: CueDetatState()
            processAndEmitState(initialState, UpdateType.FULL)
        }

        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collect { orientation ->
                onEvent(MainScreenEvent.FullOrientationChanged(orientation))
            }
        }

        viewModelScope.launch {
            visionRepository.visionDataFlow.collect { visionData ->
                onEvent(MainScreenEvent.CvDataUpdated(visionData))
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        viewModelScope.launch {
            val currentState = _uiState.value

            val logicalEvent = when (event) {
                is MainScreenEvent.ScreenGestureStarted -> {
                    val logicalPoint = Perspective.screenToLogical(
                        event.position,
                        currentState.inversePitchMatrix ?: return@launch
                    )
                    MainScreenEvent.LogicalGestureStarted(
                        logicalPoint,
                        Offset(event.position.x, event.position.y)
                    )
                }

                is MainScreenEvent.Drag -> {
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

            val reducedState =
                stateReducer(currentState, logicalEvent, reducerUtils, gestureReducer)

            val updateType = determineUpdateType(currentState, reducedState, logicalEvent)

            processAndEmitState(reducedState, updateType)

            handleSingleEvents(logicalEvent)
        }
    }

    private fun processAndEmitState(state: CueDetatState, type: UpdateType) {
        val derivedState = updateStateUseCase(state, type)
        _uiState.value = derivedState
        visionAnalyzer.updateUiState(derivedState)

        if (type != UpdateType.SPIN_ONLY) {
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
            is MainScreenEvent.SizeChanged, is MainScreenEvent.ZoomScaleChanged, is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.PanView, is MainScreenEvent.FullOrientationChanged, is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.TableRotationApplied, is MainScreenEvent.SetExperienceMode, is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.SetTableSize, is MainScreenEvent.RestoreState -> UpdateType.FULL

            is MainScreenEvent.Reset, is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied,
            is MainScreenEvent.GestureEnded, is MainScreenEvent.AddObstacleBall -> UpdateType.AIMING

            is MainScreenEvent.SpinApplied -> UpdateType.SPIN_ONLY

            else -> UpdateType.AIMING
        }
    }

    private fun handleSingleEvents(event: MainScreenEvent) {
        if (event is MainScreenEvent) {
            viewModelScope.launch {
                when (event) {
                    is MainScreenEvent.CheckForUpdate -> {
                        githubRepository.getLatestVersionName()
                    }

                    is MainScreenEvent.ViewArt -> _singleEvent.emit(SingleEvent.OpenUrl("https://herelies.az"))
                    is MainScreenEvent.ViewAboutPage -> _singleEvent.emit(SingleEvent.OpenUrl("https://github.com/HereLiesAz/CueDetat"))
                    is MainScreenEvent.SendFeedback -> _singleEvent.emit(
                        SingleEvent.SendFeedbackEmail(
                            "dev@herelies.az",
                            "Cue d'Etat Feedback"
                        )
                    )

                    is MainScreenEvent.SingleEventConsumed -> _singleEvent.emit(null)
                    is MainScreenEvent.SetExperienceMode -> {
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