// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt

package com.hereliesaz.cuedetat.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.data.ArDepthSession
import com.hereliesaz.cuedetat.data.ArFrameProcessor
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.TableScanModel
import com.hereliesaz.cuedetat.domain.ReducerUtils
import com.hereliesaz.cuedetat.domain.UpdateStateUseCase
import com.hereliesaz.cuedetat.domain.UpdateType
import com.hereliesaz.cuedetat.domain.WarningManager
import com.hereliesaz.cuedetat.domain.reducers.GestureReducer
import com.hereliesaz.cuedetat.domain.stateReducer
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.SingleEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val reducerUtils: ReducerUtils,
    private val gestureReducer: GestureReducer,
    private val snapReducer: com.hereliesaz.cuedetat.domain.reducers.SnapReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    private val sensorRepository: SensorRepository,
    private val githubRepository: GithubRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val tableScanRepository: TableScanRepository,
    private val warningManager: WarningManager,
    @ApplicationContext private val appContext: Context,
    private val visionRepository: VisionRepository,
    val arDepthSession: ArDepthSession,
    val arFrameProcessor: ArFrameProcessor,
) : ViewModel() {

    private var experienceModeUpdateJob: Job? = null
    private var saveJob: Job? = null
    private var lastEmittedOrientation: FullOrientation? = null
    private val eventChannel = Channel<MainScreenEvent>(Channel.UNLIMITED)
    private val _uiState = MutableStateFlow(CueDetatState())
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent?>()
    val singleEvent = _singleEvent.asSharedFlow()

    val visionAnalyzer: VisionAnalyzer = VisionAnalyzer(visionRepository)

    private val warningMessages: Array<String> by lazy {
        appContext.resources.getStringArray(R.array.insulting_warnings)
    }

    init {
        // Single background coroutine that serially processes all events —
        // keeps heavy computation (matrices, geometry) off the main thread.
        viewModelScope.launch(Dispatchers.Default) {
            for (event in eventChannel) {
                processEvent(event)
            }
        }

        viewModelScope.launch {
            val savedState = userPreferencesRepository.stateFlow.first()
            val currentExperienceMode = _uiState.value.experienceMode
            val initialState = (savedState ?: CueDetatState()).copy(experienceMode = currentExperienceMode)
            processAndEmitState(initialState, UpdateType.FULL)
        }

        // Pipe WarningManager's timed messages into uiState.warningText
        viewModelScope.launch {
            warningManager.currentWarning.collect { warning ->
                _uiState.value = _uiState.value.copy(warningText = warning)
            }
        }

        // Gate sensor collection on process lifecycle — unregisters the hardware listener
        // automatically when the screen turns off or the app goes to background.
        viewModelScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sensorRepository.fullOrientationFlow.collect { orientation ->
                    val last = lastEmittedOrientation
                    if (last == null ||
                        kotlin.math.abs(orientation.pitch - last.pitch) > 0.3f ||
                        kotlin.math.abs(orientation.roll - last.roll) > 0.3f ||
                        kotlin.math.abs(orientation.yaw - last.yaw) > 0.5f) {
                        lastEmittedOrientation = orientation
                        onEvent(MainScreenEvent.FullOrientationChanged(orientation))
                    }
                }
            }
        }

        viewModelScope.launch {
            visionRepository.visionDataFlow.collect { visionData ->
                onEvent(MainScreenEvent.CvDataUpdated(visionData))
            }
        }

        viewModelScope.launch {
            visionRepository.arEvents.collect { event -> onEvent(event) }
        }

        viewModelScope.launch {
            onEvent(MainScreenEvent.CheckForUpdate)
        }

        // Detect ARCore depth capability and store in state
        viewModelScope.launch {
            val capability = if (arDepthSession.isArCoreAvailable()) {
                // Probe depth support by creating (and immediately closing) a test session
                val testSession = arDepthSession.createSession()
                val cap = arDepthSession.capability
                if (testSession == null) arDepthSession.capability
                else {
                    arDepthSession.close()
                    cap
                }
            } else {
                com.hereliesaz.cuedetat.domain.DepthCapability.NONE
            }
            onEvent(MainScreenEvent.DepthCapabilityDetected(capability))
        }

        viewModelScope.launch {
            val savedModel = tableScanRepository.load()
            if (savedModel != null) {
                onEvent(MainScreenEvent.LoadTableScan(savedModel))
                checkLocationAndPromptIfNeeded(savedModel)
            }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        eventChannel.trySend(event)
    }

    private suspend fun processEvent(event: MainScreenEvent) {
        if (event is MainScreenEvent.ToggleExperienceModeSelection) {
            experienceModeUpdateJob?.cancel()
            val currentState = _uiState.value
            val nextMode = currentState.pendingExperienceMode?.next() ?: currentState.experienceMode?.next() ?: ExperienceMode.EXPERT
            _uiState.value = currentState.copy(pendingExperienceMode = nextMode)
            experienceModeUpdateJob = viewModelScope.launch {
                delay(1000)
                onEvent(MainScreenEvent.ApplyPendingExperienceMode)
            }
            return
        }

        val currentState = _uiState.value

        val logicalEvent = when (event) {
            is MainScreenEvent.ScreenGestureStarted -> {
                val logicalPoint = Perspective.screenToLogical(
                    event.position,
                    currentState.inversePitchMatrix ?: return
                )
                MainScreenEvent.LogicalGestureStarted(
                    logicalPoint,
                    Offset(event.position.x, event.position.y)
                )
            }

            is MainScreenEvent.Drag -> {
                val prevLogical = Perspective.screenToLogical(
                    event.previousPosition,
                    currentState.inversePitchMatrix ?: return
                )
                val currLogical = Perspective.screenToLogical(
                    event.currentPosition,
                    currentState.inversePitchMatrix ?: return
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

        // After CV data is integrated into state, update snap candidates.
        val finalState = if (logicalEvent is MainScreenEvent.CvDataUpdated) {
            snapReducer.reduce(reducedState, logicalEvent.visionData)
        } else {
            reducedState
        }

        val updateType = determineUpdateType(currentState, finalState, logicalEvent)

        processAndEmitState(finalState, updateType)

        handleSingleEvents(logicalEvent)
    }

    private fun processAndEmitState(state: CueDetatState, type: UpdateType) {
        val previousState = _uiState.value
        val derivedState = updateStateUseCase(state, type)

        // Trigger a cycling insult when the shot first becomes impossible/obstructed.
        val wasWarning = previousState.isGeometricallyImpossible || previousState.isObstructed || previousState.isTiltBeyondLimit
        val isWarning = derivedState.isGeometricallyImpossible || derivedState.isObstructed || derivedState.isTiltBeyondLimit
        if (isWarning && !wasWarning && warningMessages.isNotEmpty()) {
            warningManager.triggerWarning(warningMessages, viewModelScope)
        }

        _uiState.value = derivedState
        visionAnalyzer.updateUiState(derivedState)
        arFrameProcessor.updateUiState(derivedState)

        if (type != UpdateType.SPIN_ONLY) {
            saveJob?.cancel()
            saveJob = viewModelScope.launch {
                delay(2000L)
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
            is MainScreenEvent.FullOrientationChanged -> UpdateType.MATRICES_ONLY

            is MainScreenEvent.SizeChanged, is MainScreenEvent.ZoomScaleChanged, is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.PanView, is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.TableRotationApplied, is MainScreenEvent.SetExperienceMode, is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.SetTableSize, is MainScreenEvent.RestoreState -> UpdateType.FULL

            is MainScreenEvent.Reset, is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied,
            is MainScreenEvent.GestureEnded, is MainScreenEvent.AddObstacleBall -> UpdateType.AIMING

            is MainScreenEvent.SpinApplied -> UpdateType.SPIN_ONLY

            else -> UpdateType.AIMING
        }
    }

    private suspend fun checkLocationAndPromptIfNeeded(model: TableScanModel) {
        if (model.scanLatitude == null || model.scanLongitude == null) return
        val current = tableScanRepository.getCurrentLocation() ?: return
        val dist = haversineDistanceMetres(
            model.scanLatitude, model.scanLongitude,
            current.first, current.second
        )
        if (dist > 100.0) {
            warningManager.triggerWarning(
                arrayOf("You may be at a different table. Tap Scan Table to rescan."),
                viewModelScope
            )
        }
    }

    private fun haversineDistanceMetres(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun handleSingleEvents(event: MainScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is MainScreenEvent.CheckForUpdate -> {
                    val versionName = githubRepository.getLatestVersionName()
                    if (versionName != null) {
                        _uiState.value = _uiState.value.copy(latestVersionName = versionName)
                    }
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

                is MainScreenEvent.Shake -> _singleEvent.emit(SingleEvent.HaterShake)

                else -> { /* Do nothing for state-changing events */ }
            }
        }
    }
}