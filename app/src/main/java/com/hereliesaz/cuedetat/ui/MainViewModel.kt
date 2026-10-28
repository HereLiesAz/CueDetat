// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt

package com.hereliesaz.cuedetat.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.data.MetaWearableRepository
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.ArModuleState
import com.hereliesaz.cuedetat.domain.SavedScanPlausibility
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.hereliesaz.cuedetat.domain.advisor.toAdvisorInput
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val reducerUtils: ReducerUtils,
    private val gestureReducer: GestureReducer,
    private val snapReducer: com.hereliesaz.cuedetat.domain.reducers.SnapReducer,
    private val updateStateUseCase: UpdateStateUseCase,
    private val sensorRepository: SensorRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val tableScanRepository: TableScanRepository,
    private val warningManager: WarningManager,
    @ApplicationContext private val appContext: Context,
    private val visionRepository: VisionRepository,
    val visionAnalyzer: VisionAnalyzer,
    val metaWearableRepository: MetaWearableRepository,
    val arController: ArController,
    private val shotAdvisor: com.hereliesaz.cuedetat.domain.advisor.ShotAdvisor,
    private val appUpdater: com.hereliesaz.cuedetat.update.AppUpdater,
    val wristWearableRepository: com.hereliesaz.cuedetat.data.WristWearableRepository,
    private val tablePoseStore: com.hereliesaz.cuedetat.data.TablePoseStore,
    private val captureRecorder: com.hereliesaz.cuedetat.data.CaptureRecorder,
    private val debugLogRecorder: com.hereliesaz.cuedetat.data.DebugLogRecorder,
) : ViewModel() {

    /** Training capture is on (the user agreed). */
    val captureEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = captureRecorder.enabled

    /** The one-time training-capture consent dialog is still owed. */
    val captureNeedsConsent: kotlinx.coroutines.flow.StateFlow<Boolean> = captureRecorder.needsConsent

    fun setCaptureEnabled(on: Boolean) = captureRecorder.setEnabled(on)

    /** The "Debug log" setting (DebugLogRecorder): records this app's logcat for sharing. */
    val debugLogEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = debugLogRecorder.enabled

    fun setDebugLogEnabled(on: Boolean) = debugLogRecorder.setEnabled(on)

    /** Share-sheet intent for the recorded log, or null if nothing is recorded yet. */
    fun debugLogShareIntent(): android.content.Intent? = debugLogRecorder.shareIntent()

    /** Newer release from the same source as this install; UI offers the right store. */
    private val _updateInfo = kotlinx.coroutines.flow.MutableStateFlow<com.hereliesaz.cuedetat.update.UpdateInfo?>(null)
    val updateInfo: kotlinx.coroutines.flow.StateFlow<com.hereliesaz.cuedetat.update.UpdateInfo?> =
        _updateInfo.asStateFlow()

    init {
        // Before anything else can fail: a debug log that starts late misses the failure.
        debugLogRecorder.resumeIfEnabled()
    }

    /** Open the available update in the original distribution channel. */
    fun installUpdate(activity: android.app.Activity) {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            runCatching { appUpdater.downloadAndInstall(activity, info) }
        }
    }

    /** Dismiss the update popup for this session. */
    fun dismissUpdate() {
        _updateInfo.value = null
    }

    private var experienceModeUpdateJob: Job? = null

    /** Debounce for the orientation-lock cycler; mirrors [experienceModeUpdateJob]. */
    private var orientationLockApplyJob: Job? = null

    /** Fades the spin paths after the dial is released; see [startSpinPathDecay]. */
    private var spinDecayJob: Job? = null
    private var saveJob: Job? = null

    /** Latest state awaiting persistence by [saveJob]; written on the main thread only. */
    @Volatile private var pendingSaveState: CueDetatState? = null
    private var arModuleLoadJob: Job? = null
    // Bounded so an unbounded burst (sensors + vision + gestures) cannot grow without limit.
    // High-frequency producers (sensors, vision, AR) are conflated upstream so they
    // contribute at most one in-flight event each.
    private val eventChannel = Channel<MainScreenEvent>(
        capacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _uiState = MutableStateFlow(CueDetatState())
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent?>()
    val singleEvent = _singleEvent.asSharedFlow()

    private val warningMessages: Array<String> by lazy {
        appContext.resources.getStringArray(R.array.insulting_warnings)
    }

    init {
        // Single background coroutine that serially processes all events —
        // keeps heavy computation (matrices, geometry) off the main thread.
        // Per-event try/catch keeps a single bad event from killing the whole
        // loop (and the app, since uncaught coroutine exceptions force-close).
        viewModelScope.launch(Dispatchers.Default) {
            // The saved state is loaded and committed by this same coroutine before the first
            // event is taken from the channel. Loading it on a separate coroutine let early
            // events (SizeChanged, sensors) be reduced first and then be replaced wholesale by
            // the loaded state. Events sent meanwhile wait in the channel.
            loadInitialState()
            for (event in eventChannel) {
                try {
                    processEvent(event)
                } catch (t: Throwable) {
                    // Let cancellation propagate so structured concurrency keeps working
                    // if processEvent ever becomes suspending.
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    android.util.Log.e(
                        "MainViewModel",
                        "Reducer crashed processing $event",
                        t
                    )
                }
            }
        }

        // Shot advisor driver: recompute the recommendation off-main whenever the advisor is
        // enabled and the detected ball layout changes. The quantized key avoids recomputing on
        // sub-pixel CV jitter; while the advisor is off the key is the constant "off", so no
        // per-emission String is built. recommend() does not suspend, so collectLatest cannot
        // cancel a computation already running; it only skips keys superseded before it starts.
        viewModelScope.launch(Dispatchers.Default) {
            _uiState
                .map { s ->
                    if (!s.isAdvisorEnabled) "off"
                    else buildString {
                        append(s.targetType).append('|').append(s.hasInverseMatrix).append('|')
                        s.visionData?.balls?.forEach {
                            append(it.type).append(it.position.x.toInt()).append(',')
                                .append(it.position.y.toInt()).append(';')
                        }
                    }
                }
                .distinctUntilChanged()
                .collectLatest {
                    val st = _uiState.value
                    val shot = if (st.isAdvisorEnabled) st.toAdvisorInput()?.let { shotAdvisor.recommend(it) } else null
                    onEvent(com.hereliesaz.cuedetat.domain.MainScreenEvent.RecommendationComputed(shot))
                }
        }

        // Pipe WarningManager's timed messages into uiState.warningText through the event loop.
        // Reducers also write warningText (CvReducer, ControlReducer, ToggleReducer), so the loop
        // must be its only writer: a direct _uiState.update{} here was overwritten whenever the
        // loop committed a state it had reduced from a read taken before this write.
        viewModelScope.launch {
            warningManager.currentWarning.collect { warning ->
                onEvent(MainScreenEvent.SetWarning(warning))
            }
        }

        // Gate sensor collection on process lifecycle — unregisters the hardware listener
        // automatically when the screen turns off or the app goes to background.
        // conflate() drops intermediate sensor samples if the reducer falls behind,
        // bounding event-channel pressure.
        viewModelScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sensorRepository.fullOrientationFlow.conflate().collect { orientation ->
                    onEvent(MainScreenEvent.FullOrientationChanged(orientation))
                }
            }
        }

        // visionDataFlow is a StateFlow — it conflates by design.
        viewModelScope.launch {
            visionRepository.visionDataFlow.collect { visionData ->
                onEvent(MainScreenEvent.CvDataUpdated(visionData))
            }
        }

        viewModelScope.launch {
            visionRepository.arEvents.collect { event -> onEvent(event) }
        }

        viewModelScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Fires once per foreground transition. Seed the relocaliser with a
                // null delta — orientation-tracking via foreground service was removed,
                // so the relocaliser recovers via vision (runEdgeFallback) instead.
                // Only a restored (plausibly-here) scan has anything to relocalise against.
                if (_uiState.value.tableScanModel != null) {
                    onEvent(MainScreenEvent.SeedRelocaliser(null))
                }
                // Hold until lifecycle leaves STARTED, so this fires again on next resume.
                // Leaving STARTED (pocketed, screen off, app switched) stamps the restored scan as
                // last used: the restore window runs from the last time the table was in play,
                // not from when it was scanned (SavedScanPlausibility).
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    _uiState.value.tableScanModel?.let { model ->
                        val stamped = model.copy(lastUsedTimestamp = System.currentTimeMillis())
                        withContext(NonCancellable + Dispatchers.IO) { tableScanRepository.save(stamped) }
                    }
                }
            }
        }

        // collectLatest drops in-flight analyze() calls when a newer frame arrives,
        // so the analyzer never gets backlogged on slow devices.
        viewModelScope.launch(Dispatchers.Default) {
            metaWearableRepository.videoFrame.collectLatest { bitmap ->
                if (bitmap != null && _uiState.value.cameraMode == CameraMode.META_GLASSES) {
                    visionAnalyzer.analyze(bitmap, _uiState.value)
                }
            }
        }

        // Expert-AR is compiled in but constructed lazily: ARCore and the scan pipeline are
        // only set up the first time a user actually enters the AR camera flow. See the
        // CycleCameraMode interception in processEvent + ensureArModuleLoaded().

        // No saved-scan restore at launch: the camera always starts OFF, so the scan (and the
        // location lookup that vets it) is only needed when AR is turned on — see
        // restoreSavedScanForAr().

        // Collect Wrist Wearable state
        viewModelScope.launch {
            wristWearableRepository.wearableState.collect { wearableState ->
                onEvent(MainScreenEvent.WearableStateUpdated(wearableState))
            }
        }

        // Start/stop the watch's stroke-consistency trainer session in lockstep with the
        // ConsistencyOverlay's own visibility condition (EXPERT/BEGINNER mode, not on the
        // table-scan screen) — see ProtractorScreen's ConsistencyOverlay onscreen block.
        viewModelScope.launch {
            _uiState
                .map { it.experienceMode to it.showTableScanScreen }
                .distinctUntilChanged()
                .collect { (mode, showTableScanScreen) ->
                    val shouldTrain = !showTableScanScreen &&
                        (mode == ExperienceMode.EXPERT || mode == ExperienceMode.BEGINNER)
                    if (shouldTrain) {
                        wristWearableRepository.startTrainerSession()
                    } else {
                        wristWearableRepository.stopTrainerSession()
                    }
                }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        eventChannel.trySend(event)
    }

    /**
     * Loads the persisted state and commits it as the first state. Runs on the event loop
     * coroutine before any event is processed, then queues the version check, so a
     * network response cannot be overwritten by the saved-state load either.
     */
    private suspend fun loadInitialState() {
        try {
            val savedState = userPreferencesRepository.stateFlow.first()
            val savedFeltSamples = tableScanRepository.loadFeltSamples()
            // Height and zoom always open at the defaults, not where the last session left them:
            // a table lifted or zoomed for one game shouldn't greet the next one.
            // Camera always opens OFF: a restored AR mode has no ARCore session or loaded module
            // behind it, so the world-tracked projection never gets a matrix and nothing draws.
            val initialState = (savedState ?: CueDetatState()).copy(
                cameraMode = CameraMode.OFF,
                showTableScanScreen = false,
                experienceMode = _uiState.value.experienceMode,
                savedFeltSamples = savedFeltSamples,
                tableZOffset = com.hereliesaz.cuedetat.domain.DEFAULT_TABLE_Z_OFFSET,
                zoomSliderPosition = com.hereliesaz.cuedetat.domain.DEFAULT_ZOOM_SLIDER,
            )
            processAndEmitState(initialState, UpdateType.FULL)
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            android.util.Log.e("MainViewModel", "Initial state load failed", t)
        }
        onEvent(MainScreenEvent.CheckForUpdate)
    }

    /**
     * Construct the Expert-AR engine if it isn't already, driving the
     * [CueDetatState.arModuleState] lifecycle so the UI can show progress / retry.
     * Idempotent: a no-op once READY or while a load is already in flight.
     * `arController.ensureLoaded()` confirms the feature with the delivery layer, then
     * constructs the compiled-in implementation.
     */
    private fun ensureArModuleLoaded() {
        if (_uiState.value.arModuleState == ArModuleState.READY) return
        if (arModuleLoadJob?.isActive == true) return
        arModuleLoadJob = viewModelScope.launch {
            onEvent(MainScreenEvent.ArModuleLoadStarted)
            // Don't use runCatching: it would swallow CancellationException and
            // (e.g. on Retry, which cancels this job) post a spurious
            // ArModuleLoadFailed that races the freshly started load.
            var thrown: String? = null
            val loaded = try {
                arController.ensureLoaded()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (t: Throwable) {
                android.util.Log.e("ExpertAR", "Expert AR load threw", t)
                thrown = "${t.javaClass.simpleName}: ${t.message ?: "(no message)"}"
                false
            }
            if (loaded) {
                onEvent(MainScreenEvent.DepthCapabilityDetected(arController.probeCapability()))
                onEvent(MainScreenEvent.ArModuleLoadSucceeded)
            } else {
                onEvent(MainScreenEvent.ArModuleLoadFailed(thrown ?: arController.lastLoadError))
            }
        }
    }

    /**
     * On entering AR, reuse the saved table scan instead of forcing a rescan — but only if it
     * could be the table in front of the user (SavedScanPlausibility: used within the last two
     * hours, and not > 100 m away when both fixes exist). The reducer has already opened the scan
     * screen; a plausible scan is loaded and the screen skipped. Otherwise the scan proceeds.
     */
    private fun restoreSavedScanForAr() {
        viewModelScope.launch {
            val savedModel = withContext(Dispatchers.IO) { tableScanRepository.load() } ?: return@launch
            val now = System.currentTimeMillis()
            if (now - SavedScanPlausibility.lastUsedAt(savedModel) !in 0..SavedScanPlausibility.MAX_AGE_MS) return@launch
            val current = if (savedModel.scanLatitude != null && savedModel.scanLongitude != null) {
                tableScanRepository.getCurrentLocation()
            } else null
            if (!SavedScanPlausibility.isPlausiblySameTable(savedModel, now, current)) return@launch
            // The user may have finished a fresh scan or left AR while the location resolved.
            val state = _uiState.value
            if (state.cameraMode != CameraMode.AR_SETUP || state.tableScanModel != null) return@launch
            onEvent(MainScreenEvent.LoadTableScan(savedModel))
            onEvent(MainScreenEvent.SeedRelocaliser(null))
            onEvent(MainScreenEvent.StartArTracking)
        }
    }

    private fun processEvent(event: MainScreenEvent) {
        // Lock View: sensor-driven transforms are dropped before they reach the reducers.
        // Lock View freezes screen-space sensor-driven transforms by design.
        // It is distinct from AR table Lock (a physical world anchor).
        if (_uiState.value.isViewLocked &&
            com.hereliesaz.cuedetat.domain.SensorLock.blocks(event)
        ) return
        if (event is MainScreenEvent.ScreenGestureStarted || event is MainScreenEvent.LogicalGestureStarted) {
            warningManager.dismissWarning()
        }

        // Lazy Expert-AR delivery. Entering the AR camera flow (CycleCameraMode
        // from OFF) is the trigger to fetch/load the on-demand module; the reducer
        // still performs the camera-mode transition, and the UI shows a start-up
        // overlay (with Cancel) while arModuleState == LOADING. Retry re-runs the same load.
        if (event is MainScreenEvent.CycleCameraMode && _uiState.value.cameraMode == CameraMode.OFF) {
            ensureArModuleLoaded()
            restoreSavedScanForAr()
        }
        // Lock: hand ARCore the virtual table's corners as they sit on screen, so the anchors land
        // under what the user lined up. If the felt fit is sure enough, snap to it first and lock
        // the fitted outline instead. pitchMatrix maps logical -> screen pixels.
        if (event is MainScreenEvent.LockArTable) {
            val state = _uiState.value
            val c = state.table.corners // TL, TR, BR, BL
            val fit = state.tableFit?.takeIf { it.iou >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.LOCK_MIN_IOU }
            if (fit != null && state.arTableMatrix == null) {
                // The reducer moves the table onto the fit for this same event (ControlReducer).
                arController.lockTable(screenCorners = fit.viewQuad, logicalCorners = c)
            } else if (state.pitchMatrix != null) {
                val pts = FloatArray(8).also { a -> c.forEachIndexed { i, p -> a[i * 2] = p.x; a[i * 2 + 1] = p.y } }
                state.pitchMatrix.mapPoints(pts)
                arController.lockTable(
                    screenCorners = List(4) { i -> android.graphics.PointF(pts[i * 2], pts[i * 2 + 1]) },
                    logicalCorners = c,
                )
            }
        }
        if (event is MainScreenEvent.UnlockArTable) {
            arController.unlockTable()
        }
        when (event) {
            is MainScreenEvent.NudgeTableRange -> arController.nudgeTableRange(event.factor)
            is MainScreenEvent.SaveCloudPlacement -> arController.saveCloudPlacement()
            is MainScreenEvent.RestoreCloudPlacement ->
                arController.restoreCloudPlacement(_uiState.value.table.corners)
            is MainScreenEvent.ForgetCloudPlacement -> arController.forgetCloudPlacement()
            else -> Unit
        }
        if (event is MainScreenEvent.RetryArModuleLoad) {
            arModuleLoadJob?.cancel()
            arModuleLoadJob = null
            ensureArModuleLoaded()
            return
        }

        // Written outside stateReducer: no reducer handles ToggleExperienceModeSelection (it needs
        // the debounce Job below, which a pure reducer cannot own). It still commits through
        // commit() on this loop, so it does not race other writers. ApplyPendingExperienceMode,
        // sent after the debounce, is reduced by ToggleReducer.
        if (event is MainScreenEvent.ToggleExperienceModeSelection) {
            experienceModeUpdateJob?.cancel()
            val currentState = _uiState.value
            val nextMode = currentState.pendingExperienceMode?.next() ?: currentState.experienceMode?.next() ?: ExperienceMode.EXPERT
            commit(currentState.copy(pendingExperienceMode = nextMode))
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
        // State written here, outside stateReducer (StateReducer.kt KDoc lists it): SnapReducer and
        // the AWAITING_CUE promotion below. Moving the promotion into a reducer is a domain change;
        // it stays here until then. SetTopDownBitmap is reduced by ToggleReducer and is no longer
        // re-copied here.
        val finalState = when (logicalEvent) {
            is MainScreenEvent.CvDataUpdated -> {
                val stateAfterSnap = snapReducer.reduce(reducedState, logicalEvent.visionData)
                // Camera on, not "table scanned": detections exist without a scan.
                if (stateAfterSnap.cameraMode != CameraMode.OFF &&
                    stateAfterSnap.experienceMode == ExperienceMode.EXPERT &&
                    stateAfterSnap.ballSelectionPhase == BallSelectionPhase.NONE &&
                    stateAfterSnap.snapCandidates?.any { it.isConfirmed } == true
                ) {
                    stateAfterSnap.copy(ballSelectionPhase = BallSelectionPhase.AWAITING_CUE)
                } else {
                    stateAfterSnap
                }
            }
            else -> reducedState
        }

        // Side-effects for Top-Down view
        if (logicalEvent is MainScreenEvent.ToggleTopDownView) {
            if (finalState.isTopDownViewActive) {
                visionRepository.captureRectifiedSnapshot(finalState)
            }
        }

        val updateType = determineUpdateType(currentState, finalState, logicalEvent)

        processAndEmitState(finalState, updateType)

        handleSingleEvents(logicalEvent)
        tableSnapSideEffects(logicalEvent, currentState, _uiState.value)
        timedSideEffects(logicalEvent)
    }

    /**
     * Timers a pure reducer cannot own. [MainScreenEvent.ToggleOrientationLock] only stages
     * `pendingOrientationLock`; after the user stops cycling, [MainScreenEvent.ApplyPendingOrientationLock]
     * commits it, which MainActivity's `requestedOrientation` effect observes. Spin paths fade via
     * [MainScreenEvent.SpinPathTick] from release until alpha reaches 0.
     */
    private fun timedSideEffects(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ToggleOrientationLock -> {
                orientationLockApplyJob?.cancel()
                orientationLockApplyJob = viewModelScope.launch {
                    delay(ORIENTATION_LOCK_DEBOUNCE_MS)
                    onEvent(MainScreenEvent.ApplyPendingOrientationLock)
                }
            }
            is MainScreenEvent.SpinSelectionEnded -> startSpinPathDecay()
            // A fresh spin (alpha back to 1) or a cleared/closed dial ends any running fade.
            is MainScreenEvent.SpinApplied, is MainScreenEvent.ClearSpinState,
            is MainScreenEvent.ToggleSpinControl, is MainScreenEvent.ToggleMasseMode -> {
                spinDecayJob?.cancel()
                spinDecayJob = null
            }
            else -> Unit
        }
    }

    /** Bounded: ticks every [SPIN_DECAY_TICK_MS] and stops once `spinPathsAlpha` hits 0 (about 20 ticks). */
    private fun startSpinPathDecay() {
        spinDecayJob?.cancel()
        spinDecayJob = viewModelScope.launch {
            while (_uiState.value.spinPathsAlpha > 0f) {
                delay(SPIN_DECAY_TICK_MS)
                onEvent(MainScreenEvent.SpinPathTick)
            }
        }
    }

    // --- Table snap and pose memory (see TableSnapPolicy, TablePosePrior) --------------------

    private var lastUserTableAdjustMs = 0L
    private var lastPoseRecordMs = 0L
    @Volatile private var cachedLocation: Pair<Double, Double>? = null
    @Volatile private var locationSamples: List<com.hereliesaz.cuedetat.domain.TablePoseSample>? = null
    @Volatile private var lastTableSamples: List<com.hereliesaz.cuedetat.domain.TablePoseSample>? = null

    init {
        // The fit asks for the remembered orientation at the phone's current heading.
        visionRepository.tablePrior = { yaw, pitch -> tablePrior(yaw, pitch)?.prediction }
    }

    private fun tablePrior(yaw: Float, pitch: Float) = com.hereliesaz.cuedetat.domain.TablePosePrior.choose(
        session = tablePoseStore.sessionSample,
        atLocation = locationSamples,
        lastTable = lastTableSamples,
        yawDeg = yaw,
        pitchDeg = pitch,
        nowMs = System.currentTimeMillis(),
    )

    private fun tableSnapSideEffects(event: MainScreenEvent, before: CueDetatState, after: CueDetatState) {
        val now = System.currentTimeMillis()
        when (event) {
            // Any hand on the table (or the screen) holds the pull off.
            is MainScreenEvent.PanView, is MainScreenEvent.ZoomSliderChanged, is MainScreenEvent.ZoomScaleChanged,
            is MainScreenEvent.TableRotationChanged, is MainScreenEvent.TableRotationApplied,
            is MainScreenEvent.TableScaleChanged, is MainScreenEvent.NudgeTableRange,
            is MainScreenEvent.ScreenGestureStarted, is MainScreenEvent.LogicalGestureStarted ->
                lastUserTableAdjustMs = now

            is MainScreenEvent.TableFitUpdated -> {
                // World-fixed preview cannot be pulled by a noisy frame-by-frame felt fit.
                if (after.cameraMode == CameraMode.AR_ACTIVE && after.arTableMatrix != null) return
                val fit = event.fit ?: return
                val current = poseOf(after)
                com.hereliesaz.cuedetat.domain.TableSnapPolicy.pullStep(
                    current, fit.pose.let { com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose(it.offsetX, it.offsetY, it.rotationDeg, it.zoom) },
                    fit.iou, now - lastUserTableAdjustMs,
                )?.let { onEvent(MainScreenEvent.ApplyTablePose(it.offsetX, it.offsetY, it.rotationDeg, it.zoom)) }
                if (fit.iou >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.RECORD_MIN_IOU &&
                    now - lastPoseRecordMs >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.RECORD_INTERVAL_MS
                ) {
                    // The fit can't tell the ends apart; record it turned to match the table on screen.
                    val shown = after.worldRotationDegrees
                    val fitRot = shown + com.hereliesaz.cuedetat.domain.TableOrientationLearner.normalize180(fit.pose.rotationDeg - shown)
                    recordPose(after, fit.pose.let { com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose(it.offsetX, it.offsetY, fitRot, it.zoom) })
                }
            }

            // The user placed the table: a scan's placement or a lock. These fix which end is which.
            is MainScreenEvent.ArTableLockResult -> if (event.locked) recordPose(after, poseOf(after), anchor = true)
            is MainScreenEvent.ApplyQuickAlign -> recordPose(after, poseOf(after), anchor = true)

            else -> Unit
        }

        // Camera just came on: load what is remembered here and seed the table with it.
        if (before.cameraMode == CameraMode.OFF && after.cameraMode != CameraMode.OFF) {
            val startedMs = now
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val loc = runCatching { tableScanRepository.getCurrentLocation() }.getOrNull()
                cachedLocation = loc
                locationSamples = tablePoseStore.samplesAt(loc?.first, loc?.second)
                lastTableSamples = tablePoseStore.lastTableSamples()
                val state = _uiState.value
                val prior = tablePrior(state.currentOrientation.yaw, state.currentOrientation.pitch)?.prediction
                // A GPS fix can take seconds; if the user has touched the table since, leave it.
                if (prior != null && prior.confidence >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.PRIOR_APPLY_CONFIDENCE &&
                    lastUserTableAdjustMs <= startedMs && !state.isBeginnerViewLocked && !state.isArTableLocked
                ) {
                    onEvent(MainScreenEvent.ApplyTablePose(state.viewOffset.x, state.viewOffset.y, prior.rotationDeg, prior.zoom))
                }
            }
        }
    }

    private fun poseOf(state: CueDetatState): com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose {
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(state.experienceMode, state.isBeginnerViewLocked)
        return com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose(
            state.viewOffset.x, state.viewOffset.y, state.worldRotationDegrees,
            ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom),
        )
    }

    private fun recordPose(state: CueDetatState, pose: com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose, anchor: Boolean = false) {
        lastPoseRecordMs = System.currentTimeMillis()
        val o = state.currentOrientation
        val sample = com.hereliesaz.cuedetat.domain.TablePoseSample(
            yawDeg = o.yaw, pitchDeg = o.pitch, rollDeg = o.roll,
            rotationDeg = pose.rotationDeg, zoom = pose.zoom,
            offsetX = pose.offsetX, offsetY = pose.offsetY,
            timestampMs = lastPoseRecordMs,
            anchor = anchor,
        )
        val loc = cachedLocation
        val size = state.table.size.name
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            tablePoseStore.record(sample, loc?.first, loc?.second, size)
            locationSamples = tablePoseStore.samplesAt(loc?.first, loc?.second)
            lastTableSamples = tablePoseStore.lastTableSamples()
        }
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

        commit(derivedState)
        visionAnalyzer.updateUiState(derivedState)
        arController.updateUiState(derivedState)
        arController.setTableZOffsetLogical(derivedState.tableZOffset)

        // Start only on the transition into META_GLASSES: this function runs on every sensor
        // tick, and calling startStreaming() each time spawned a new session per tick while the
        // first was still CONNECTING.
        if (derivedState.cameraMode == CameraMode.META_GLASSES) {
            if (previousState.cameraMode != CameraMode.META_GLASSES) metaWearableRepository.startStreaming()
        } else if (previousState.cameraMode == CameraMode.META_GLASSES) {
            metaWearableRepository.stopStreaming()
        }

        if (type != UpdateType.SPIN_ONLY) {
            val isHighPriority = type == UpdateType.FULL ||
                               state.tableScanModel != previousState.tableScanModel ||
                               state.viewOffset != previousState.viewOffset

            // Throttle, not debounce: a low-priority emit leaves an already-pending save in
            // place and only refreshes the state it will write. Cancelling on every emit meant
            // the continuous sensor stream (MATRICES_ONLY) starved AIMING-type saves forever.
            pendingSaveState = derivedState
            if (!isHighPriority && saveJob?.isActive == true) return
            saveJob?.cancel()
            saveJob = viewModelScope.launch {
                try {
                    if (!isHighPriority) delay(2000L)
                    val toSave = pendingSaveState ?: derivedState
                    userPreferencesRepository.saveState(toSave)
                    tableScanRepository.saveFeltSamples(toSave.savedFeltSamples)
                } catch (t: Throwable) {
                    // saveJob is cancelled and replaced by high-priority emits,
                    // so CancellationException is normal here — rethrow to keep
                    // structured concurrency intact and avoid log spam.
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    // Persistence failures (gson serialization, datastore IO) must not
                    // crash the app via the global uncaught-exception handler.
                    android.util.Log.e("MainViewModel", "saveState failed", t)
                }
            }
        }
    }

    /**
     * Publishes a state reduced on the event loop. The loop is the single writer of every
     * field except [CueDetatState.latestVersionName], which the update check writes from its
     * own coroutine; that one field is taken from the live value inside a CAS so a state
     * reduced from an older read cannot erase it.
     */
    private fun commit(state: CueDetatState) {
        _uiState.update { live ->
            if (live.latestVersionName == state.latestVersionName) state
            else state.copy(latestVersionName = live.latestVersionName)
        }
    }

    private fun determineUpdateType(
        oldState: CueDetatState,
        newState: CueDetatState,
        event: MainScreenEvent
    ): UpdateType {
        return when (event) {
            // Compass follow (SystemReducer.followCompass) may have turned the table: that is a
            // rotation like TableRotationChanged and needs the same full recompute.
            is MainScreenEvent.FullOrientationChanged ->
                if (newState.worldRotationDegrees != oldState.worldRotationDegrees) UpdateType.FULL
                else UpdateType.MATRICES_ONLY

            // World-anchored table pose arrives ~30-60Hz from the ARCore GL thread. It must
            // recompute the matrices each frame so the overlay tracks as the user walks; routing it
            // to AIMING (the default) would silently freeze the table at its first pose.
            is MainScreenEvent.ArTableMatrixUpdated, is MainScreenEvent.NudgeTableRange -> UpdateType.MATRICES_ONLY

            is MainScreenEvent.SizeChanged, is MainScreenEvent.ZoomScaleChanged, is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.PanView, is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.TableRotationApplied, is MainScreenEvent.TableScaleChanged,
            is MainScreenEvent.SetTableFitScale,
            is MainScreenEvent.SetExperienceMode, is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.SetTableSize,
            // MoveTableZ (table-height slider) feeds the rendering matrices in
            // UpdateStateUseCase.updateMatricesAndTransforms (screen-space vertical lift). Without a
            // FULL recompute the new tableZOffset is stored but never applied, so the table doesn't
            // move until some other event happens to recompute the matrices.
            is MainScreenEvent.MoveTableZ, is MainScreenEvent.SetTableZ,
            // Snap / remembered pose moves the table like a pan-rotate-zoom does.
            is MainScreenEvent.ApplyTablePose,
            // Lock may snap the table onto the felt fit first (ControlReducer).
            is MainScreenEvent.LockArTable -> UpdateType.FULL

            is MainScreenEvent.Reset, is MainScreenEvent.LogicalGestureStarted, is MainScreenEvent.LogicalDragApplied,
            is MainScreenEvent.GestureEnded, is MainScreenEvent.AddObstacleBall -> UpdateType.AIMING

            is MainScreenEvent.SpinApplied -> UpdateType.SPIN_ONLY

            else -> UpdateType.AIMING
        }
    }

    /**
     * Side effects for the few events that have one. Most events (sensor, vision and AR
     * ticks at 30-60 Hz) have none, so they return before a coroutine is launched.
     */
    private fun handleSingleEvents(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.CheckForUpdate -> viewModelScope.launch {
                // One source-aware check serves both Play and GitHub installs.
                // A null result means no available update (or a logged backend
                // failure), not permission to fall through to the other store.
                try {
                    val available = appUpdater.checkForUpdate()
                    _updateInfo.value = available
                    if (available != null) {
                        _uiState.update { it.copy(latestVersionName = available.versionName) }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "CheckForUpdate failed", e)
                    warningManager.triggerWarning(
                        arrayOf(appContext.getString(R.string.update_check_failed)),
                        viewModelScope
                    )
                }
            }

            // Forget scan: the in-memory model goes through ControlReducer; this drops the saved
            // copy too, or restoreSavedScanForAr would resurrect it on the next AR entry.
            is MainScreenEvent.ClearTableScan -> viewModelScope.launch(Dispatchers.IO) {
                tableScanRepository.clear()
            }
            // The Support rail item. MainActivity owns the sheet's visibility and opens it on
            // this event; no reducer handles it.
            is MainScreenEvent.ToggleSupportSheet -> emitSingle(SingleEvent.ShowSupportSheet)

            is MainScreenEvent.SingleEventConsumed -> emitSingle(null)
            is MainScreenEvent.SetExperienceMode -> {
                if (event.mode == ExperienceMode.HATER) emitSingle(SingleEvent.InitiateHaterMode)
            }

            is MainScreenEvent.Shake -> emitSingle(SingleEvent.HaterShake)

            else -> Unit // State-changing events have no side effect here.
        }
    }

    private fun emitSingle(event: SingleEvent?) {
        viewModelScope.launch { _singleEvent.emit(event) }
    }
}

/** Pause after the last orientation-lock tap before the lock is applied. */
private const val ORIENTATION_LOCK_DEBOUNCE_MS = 1000L

/** Interval between [MainScreenEvent.SpinPathTick]s; the reducer removes 0.05 alpha per tick. */
private const val SPIN_DECAY_TICK_MS = 50L
