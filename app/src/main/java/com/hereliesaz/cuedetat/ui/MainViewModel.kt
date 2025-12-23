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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The central ViewModel for the main application screen.
 *
 * This ViewModel is the core of the MVI (Model-View-Intent) architecture for the main UI. It is responsible for:
 * - Holding the single source of truth for the UI state ([CueDetatState]).
 * - Processing user and system events ([MainScreenEvent]).
 * - Reducing the current state and an event into a new state.
 * - Calculating all derived state via [UpdateStateUseCase].
 * - Handling one-time "side-effect" events ([SingleEvent]).
 * - Persisting the UI state to [UserPreferencesRepository].
 *
 * @property reducerUtils A collection of utility functions used by the state reducer.
 * @property gestureReducer The specific reducer responsible for handling all gesture-related events.
 * @property updateStateUseCase The use case that calculates derived properties of the state (e.g., matrices, line coordinates).
 * @property sensorRepository The repository for accessing device sensor data (e.g., orientation).
 * @property githubRepository The repository for checking for new app versions on GitHub.
 * @property userPreferencesRepository The repository for persisting user settings and the application state.
 * @property visionRepository The repository for receiving data from the computer vision module.
 */
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

    private var experienceModeUpdateJob: Job? = null
    private val _uiState = MutableStateFlow(CueDetatState())
    /** The StateFlow that exposes the current UI state to the Composables. */
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent?>()
    /** The SharedFlow for one-time events that should not be tied to the state (e.g., navigation). */
    val singleEvent = _singleEvent.asSharedFlow()

    /** The analyzer responsible for processing camera frames for computer vision tasks. */
    val visionAnalyzer: VisionAnalyzer = VisionAnalyzer(visionRepository)

    init {
        // On initialization, load the last saved state from user preferences.
        viewModelScope.launch {
            val savedState = userPreferencesRepository.stateFlow.first()
            val initialState = savedState ?: CueDetatState()
            processAndEmitState(initialState, UpdateType.FULL)
        }

        // Collect orientation data from the sensor repository and dispatch events.
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow.collect { orientation ->
                onEvent(MainScreenEvent.FullOrientationChanged(orientation))
            }
        }

        // Collect computer vision data and dispatch events.
        viewModelScope.launch {
            visionRepository.visionDataFlow.collect { visionData ->
                onEvent(MainScreenEvent.CvDataUpdated(visionData))
            }
        }
    }

    /**
     * The main entry point for all UI events.
     * This function takes an event, processes it, and updates the UI state accordingly.
     *
     * @param event The [MainScreenEvent] to be processed.
     */
    fun onEvent(event: MainScreenEvent) {
        viewModelScope.launch {
            // Handle special case for toggling experience mode with a delay.
            if (event is MainScreenEvent.ToggleExperienceModeSelection) {
                experienceModeUpdateJob?.cancel()
                val currentState = _uiState.value
                val nextMode = currentState.pendingExperienceMode?.next() ?: currentState.experienceMode?.next() ?: ExperienceMode.EXPERT
                _uiState.value = currentState.copy(pendingExperienceMode = nextMode)
                experienceModeUpdateJob = viewModelScope.launch {
                    delay(1000)
                    onEvent(MainScreenEvent.ApplyPendingExperienceMode)
                }
                return@launch
            }

            val currentState = _uiState.value

            // Convert screen-space gesture events into logical-space events.
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

            // The core of the MVI loop: reduce the current state and event to a new state.
            val reducedState =
                stateReducer(currentState, logicalEvent, reducerUtils, gestureReducer)

            // Determine the type of update needed based on the event and state change.
            val updateType = determineUpdateType(currentState, reducedState, logicalEvent)

            // Process the new state to calculate derived properties and emit it to the UI.
            processAndEmitState(reducedState, updateType)

            // Handle any side-effects (single events) triggered by the event.
            handleSingleEvents(logicalEvent)
        }
    }

    /**
     * Processes a new state, calculates its derived properties, and emits it to the UI.
     * Also handles persisting the state.
     *
     * @param state The new state to be processed.
     * @param type The [UpdateType] indicating how much of the derived state needs to be recalculated.
     */
    private fun processAndEmitState(state: CueDetatState, type: UpdateType) {
        val derivedState = updateStateUseCase(state, type)
        _uiState.value = derivedState
        visionAnalyzer.updateUiState(derivedState) // Keep the vision analyzer in sync.

        // Persist the state unless it was a minor, transient update (like spin).
        if (type != UpdateType.SPIN_ONLY) {
            viewModelScope.launch {
                userPreferencesRepository.saveState(derivedState)
            }
        }
    }

    /**
     * Determines the [UpdateType] based on the event and the changes between the old and new state.
     * This is a performance optimization to avoid recalculating the entire derived state on every event.
     */
    private fun determineUpdateType(
        oldState: CueDetatState,
        newState: CueDetatState,
        event: MainScreenEvent
    ): UpdateType {
        return when (event) {
            // Events that require a full recalculation of the derived state.
            is MainScreenEvent.SizeChanged, is MainScreenEvent.ZoomScaleChanged, is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.PanView, is MainScreenEvent.FullOrientationChanged, is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.TableRotationApplied, is MainScreenEvent.SetExperienceMode, is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.SetTableSize, is MainScreenEvent.RestoreState -> UpdateType.FULL

            // Events that only affect the aiming-related parts of the state.
            is MainScreenEvent.Reset, is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied,
            is MainScreenEvent.GestureEnded, is MainScreenEvent.AddObstacleBall -> UpdateType.AIMING

            is MainScreenEvent.ToggleArScreen -> UpdateType.FULL

            // Events that only affect the spin visualization.
            is MainScreenEvent.SpinApplied -> UpdateType.SPIN_ONLY

            // Default to aiming for most other events.
            else -> UpdateType.AIMING
        }
    }

    /**
     * Handles events that should trigger a one-time side-effect, such as opening a URL or sending an email.
     */
    private fun handleSingleEvents(event: MainScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is MainScreenEvent.CheckForUpdate -> {
                    githubRepository.getLatestVersionName()
                }
                is MainScreenEvent.ViewArt -> _singleEvent.emit(SingleEvent.OpenUrl("https://herelies.az"))
                is MainScreenEvent.ViewAboutPage -> _singleEvent.emit(SingleEvent.OpenUrl("https://github.com/HereLiesAz/CueDetat"))
                is MainScreenEvent.SendFeedback -> _singleEvent.emit(
                    SingleEvent.SendFeedbackEmail(
                        "hereliesaz@gmail.com",
                        "Cue d'Etat Feedback"
                    )
                )
                is MainScreenEvent.SingleEventConsumed -> _singleEvent.emit(null)
                is MainScreenEvent.SetExperienceMode -> {
                    if (event.mode == ExperienceMode.HATER) {
                        _singleEvent.emit(SingleEvent.InitiateHaterMode)
                    }
                }
                else -> { /* Do nothing for events that only change state. */ }
            }
        }
    }
}
