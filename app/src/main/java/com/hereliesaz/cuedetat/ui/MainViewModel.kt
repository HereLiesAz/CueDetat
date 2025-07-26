// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt

package com.hereliesaz.cuedetat.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CalculateBankShot
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.domain.StateReducer
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.domain.UpdateType
import com.hereliesaz.cuedetat.domain.WarningManager
import com.hereliesaz.cuedetat.ui.composables.quickalign.QuickAlignViewModel
import com.hereliesaz.cuedetat.view.model.OnPlaneBall
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.ExperienceMode
import com.hereliesaz.cuedetat.view.state.OverlayState
import com.hereliesaz.cuedetat.view.state.SingleEvent
import com.hereliesaz.cuedetat.view.state.ToastMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateReducer: StateReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    private val calculateBankShot: CalculateBankShot,
    private val sensorRepository: SensorRepository,
    private val githubRepository: GithubRepository,
    private val warningManager: WarningManager,
    val visionAnalyzer: VisionAnalyzer,
    private val visionRepository: VisionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val reducerUtils: ReducerUtils,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayState())
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = Channel<SingleEvent?>()
    val singleEvent = _singleEvent.receiveAsFlow()

    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private var gestureJob: Job? = null
    private var isStateLoaded = false

    private var orientationDebounceJob: Job? = null
    private var experienceModeDebounceJob: Job? = null

    init {
        // Load persisted state first
        viewModelScope.launch {
            var savedState = userPreferencesRepository.stateFlow.first()
            if (savedState != null) {
                savedState = enforceModeInvariants(savedState)
                onEvent(MainScreenEvent.RestoreState(savedState))
            }
            isStateLoaded = true
            startDataFlows() // Start collecting high-frequency data after initial state is loaded
        }
    }

    private fun enforceModeInvariants(state: OverlayState): OverlayState {
        return when (state.experienceMode) {
            ExperienceMode.EXPERT -> {
                var expertState = state
                if (!expertState.table.isVisible) {
                    expertState = expertState.copy(table = expertState.table.copy(isVisible = true))
                }
                if (expertState.onPlaneBall == null) {
                    val cueBallCenter = reducerUtils.getDefaultCueBallPosition(expertState)
                    expertState = expertState.copy(
                        onPlaneBall = OnPlaneBall(
                            center = cueBallCenter,
                            radius = LOGICAL_BALL_RADIUS
                        )
                    )
                }
                expertState
            }

            ExperienceMode.BEGINNER -> {
                state.copy(
                    table = state.table.copy(isVisible = false),
                    onPlaneBall = null,
                    isBankingMode = false
                )
            }

            else -> state // Hater mode
        }
    }

    fun listenToQuickAlign(quickAlignViewModel: QuickAlignViewModel) {
        viewModelScope.launch {
            quickAlignViewModel.alignResult.collectLatest { result ->
                onEvent(
                    MainScreenEvent.ApplyQuickAlign(
                        translation = result.translation,
                        rotation = result.rotation,
                        scale = result.scale
                    )
                )
            }
        }
    }

    private fun startDataFlows() {
        // Sensor data flow
        viewModelScope.launch {
            sensorRepository.fullOrientationFlow
                .distinctUntilChanged()
                .collectLatest { orientation ->
                    onEvent(MainScreenEvent.FullOrientationChanged(orientation))
                }
        }

        // Vision data flow
        viewModelScope.launch {
            visionRepository.visionDataFlow.collectLatest { visionData ->
                onEvent(MainScreenEvent.CvDataUpdated(visionData))
            }
        }

        // Warning manager flow
        viewModelScope.launch {
            warningManager.currentWarning.collect { warning ->
                onEvent(MainScreenEvent.SetWarning(warning))
            }
        }

        // Auto-save state flow
        viewModelScope.launch {
            _uiState.debounce(500L).collectLatest { state ->
                if (isStateLoaded) { // Only save after initial state is loaded
                    userPreferencesRepository.saveState(state)
                }
            }
        }

        // Load calibration data
        viewModelScope.launch {
            userPreferencesRepository.calibrationDataFlow.collectLatest { (cameraMatrix, distCoeffs) ->
                _uiState.value = _uiState.value.copy(
                    cameraMatrix = cameraMatrix,
                    distCoeffs = distCoeffs
                )
            }
        }

        onEvent(MainScreenEvent.CheckForUpdate)
    }

    fun onEvent(event: MainScreenEvent) {
        if (!isStateLoaded && event !is MainScreenEvent.RestoreState) return // Don't process events until initial state is loaded

        viewModelScope.launch {
            val currentState = _uiState.value

            val reducedState = when (event) {
                // High-frequency gesture events that need a fast path
                is MainScreenEvent.Drag -> handleDrag(currentState, event)
                is MainScreenEvent.ScreenGestureStarted -> handleScreenGestureStarted(
                    currentState,
                    event
                )
                // Special case for restoring state
                is MainScreenEvent.RestoreState -> event.state
                // All other events go through the standard reducer
                else -> stateReducer.reduce(currentState, event)
            }

            // Determine the necessary level of recalculation
            val updateType = when (event) {
                is MainScreenEvent.SizeChanged,
                is MainScreenEvent.FullOrientationChanged,
                is MainScreenEvent.ZoomSliderChanged,
                is MainScreenEvent.PanView,
                is MainScreenEvent.TableRotationChanged,
                is MainScreenEvent.TableRotationApplied,
                is MainScreenEvent.ThemeChanged,
                is MainScreenEvent.OrientationChanged,
                is MainScreenEvent.ApplyPendingOrientationLock,
                is MainScreenEvent.ApplyPendingExperienceMode,
                is MainScreenEvent.ApplyQuickAlign,
                is MainScreenEvent.RestoreState -> UpdateType.FULL

                is MainScreenEvent.LogicalDragApplied,
                is MainScreenEvent.GestureEnded,
                is MainScreenEvent.Reset,
                is MainScreenEvent.AddObstacleBall,
                is MainScreenEvent.ToggleBankingMode,
                is MainScreenEvent.CvDataUpdated,
                is MainScreenEvent.SetTableSize,
                is MainScreenEvent.CycleTableSize,
                is MainScreenEvent.SetExperienceMode -> UpdateType.AIMING

                is MainScreenEvent.SpinApplied,
                is MainScreenEvent.SpinSelectionEnded,
                is MainScreenEvent.ClearSpinState -> UpdateType.SPIN_ONLY

                else -> null
            }

            val finalState = if (updateType != null) {
                updateStateUseCase(reducedState, updateType)
            } else {
                reducedState
            }

            _uiState.value = finalState
            visionAnalyzer.updateUiState(finalState)

            // Check for mode switch to Hater and trigger its reset/entry sequence
            if ((event is MainScreenEvent.SetExperienceMode && event.mode == ExperienceMode.HATER) ||
                (event is MainScreenEvent.ApplyPendingExperienceMode && finalState.experienceMode == ExperienceMode.HATER)
            ) {
                _singleEvent.send(SingleEvent.InitiateHaterMode)
            }

            handleSideEffects(event, finalState)
        }
    }

    private fun handleSideEffects(event: MainScreenEvent, state: OverlayState) {
        when (event) {
            is MainScreenEvent.GestureEnded -> {
                if (state.isGeometricallyImpossible || state.isObstructed || state.isTiltBeyondLimit) {
                    val warnings = context.resources.getStringArray(R.array.insulting_warnings)
                    warningManager.triggerWarning(warnings, viewModelScope)
                }
            }

            is MainScreenEvent.CheckForUpdate -> fetchLatestVersionName()
            is MainScreenEvent.ViewArt -> viewModelScope.launch {
                _singleEvent.send(
                    SingleEvent.OpenUrl(
                        "https://instagram.com/hereliesaz"
                    )
                )
            }

            is MainScreenEvent.ViewAboutPage -> viewModelScope.launch {
                _singleEvent.send(
                    SingleEvent.OpenUrl("https://github.com/HereLiesAz/CueDetat")
                )
            }

            is MainScreenEvent.SendFeedback -> viewModelScope.launch {
                _singleEvent.send(
                    SingleEvent.SendFeedbackEmail(
                        email = "hereliesaz@gmail.com",
                        subject = "Cue d'Etat feedback"
                    )
                )
            }
            is MainScreenEvent.ToastShown -> _toastMessage.value = null
            is MainScreenEvent.SampleColorAt -> {
                val newHsv = _uiState.value.visionData?.detectedHsvColor
                if (newHsv != null) {
                    val newStdDev = floatArrayOf(10f, 50f, 50f)
                    onEvent(MainScreenEvent.LockColor(newHsv, newStdDev))
                }
                onEvent(MainScreenEvent.ClearSamplePoint)
            }

            is MainScreenEvent.ToggleOrientationLock -> {
                orientationDebounceJob?.cancel()
                orientationDebounceJob = viewModelScope.launch {
                    delay(1000L)
                    onEvent(MainScreenEvent.ApplyPendingOrientationLock)
                }
            }

            is MainScreenEvent.ToggleExperienceMode -> {
                experienceModeDebounceJob?.cancel()
                experienceModeDebounceJob = viewModelScope.launch {
                    delay(1000L)
                    onEvent(MainScreenEvent.ApplyPendingExperienceMode)
                }
            }

            is MainScreenEvent.MenuClosed -> {
                orientationDebounceJob?.cancel()
                if (state.pendingOrientationLock != null) {
                    onEvent(MainScreenEvent.ApplyPendingOrientationLock)
                }
                experienceModeDebounceJob?.cancel()
                if (state.pendingExperienceMode != null) {
                    onEvent(MainScreenEvent.ApplyPendingExperienceMode)
                }
            }

            else -> { /* No side effect */
            }
        }
    }

    private fun fetchLatestVersionName() {
        viewModelScope.launch {
            val latestVersion = githubRepository.getLatestVersionName()?.removePrefix("v")
            if (latestVersion != null) {
                _uiState.value = _uiState.value.copy(latestVersionName = latestVersion)
                if (latestVersion != BuildConfig.VERSION_NAME) {
                    _toastMessage.value = ToastMessage.StringResource(
                        R.string.update_available,
                        listOf(latestVersion)
                    )
                }
            } else {
                _toastMessage.value = ToastMessage.StringResource(R.string.update_check_failed)
            }
        }
    }

    private fun handleScreenGestureStarted(
        currentState: OverlayState,
        event: MainScreenEvent.ScreenGestureStarted
    ): OverlayState {
        val inverseMatrix = currentState.inversePitchMatrix ?: return currentState
        val logicalPoint = Perspective.screenToLogical(event.position, inverseMatrix)
        val screenOffset = Offset(event.position.x, event.position.y)
        return stateReducer.reduce(
            currentState,
            MainScreenEvent.LogicalGestureStarted(logicalPoint, screenOffset)
        )
    }

    private fun handleDrag(currentState: OverlayState, event: MainScreenEvent.Drag): OverlayState {
        val inverseMatrix = currentState.inversePitchMatrix ?: return currentState
        val prevLogical = Perspective.screenToLogical(event.previousPosition, inverseMatrix)
        val currLogical = Perspective.screenToLogical(event.currentPosition, inverseMatrix)
        val screenDelta = Offset(
            event.currentPosition.x - event.previousPosition.x,
            event.currentPosition.y - event.previousPosition.y
        )
        return stateReducer.reduce(
            currentState,
            MainScreenEvent.LogicalDragApplied(prevLogical, currLogical, screenDelta)
        )
    }
}