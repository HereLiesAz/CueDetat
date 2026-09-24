// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/MainViewModel.kt

package com.hereliesaz.cuedetat.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.R
import com.hereliesaz.cuedetat.arfeature.ArController
import com.hereliesaz.cuedetat.data.GithubRepository
import com.hereliesaz.cuedetat.data.MetaWearableRepository
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.ArModuleState
import com.hereliesaz.cuedetat.domain.BallSelectionPhase
import com.hereliesaz.cuedetat.domain.CameraMode
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
    val visionAnalyzer: VisionAnalyzer,
    val metaWearableRepository: MetaWearableRepository,
    val arController: ArController,
    private val shotAdvisor: com.hereliesaz.cuedetat.domain.advisor.ShotAdvisor,
    private val appUpdater: com.hereliesaz.cuedetat.update.AppUpdater,
    val wristWearableRepository: com.hereliesaz.cuedetat.data.WristWearableRepository,
    private val tablePoseStore: com.hereliesaz.cuedetat.data.TablePoseStore,
) : ViewModel() {

    /**
     * FOSS self-update. Non-null when a newer GitHub release is available; the
     * UI shows a one-tap "download & install" popup. Always null in Play
     * (store-managed updates).
     */
    private val _updateInfo = kotlinx.coroutines.flow.MutableStateFlow<com.hereliesaz.cuedetat.update.UpdateInfo?>(null)
    val updateInfo: kotlinx.coroutines.flow.StateFlow<com.hereliesaz.cuedetat.update.UpdateInfo?> =
        _updateInfo.asStateFlow()

    init {
        if (appUpdater.isSupported) {
            viewModelScope.launch {
                runCatching { appUpdater.checkForUpdate() }.getOrNull()?.let { _updateInfo.value = it }
            }
        }
    }

    /** Download the available update APK and launch the system installer (FOSS only). */
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
    private var saveJob: Job? = null
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
        // enabled and the detected ball layout changes. collectLatest cancels superseded work;
        // the quantized key avoids recomputing on sub-pixel CV jitter.
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

        viewModelScope.launch {
            val savedState = userPreferencesRepository.stateFlow.first()
            val savedFeltSamples = tableScanRepository.loadFeltSamples()
            val currentExperienceMode = _uiState.value.experienceMode
            val initialState = (savedState ?: CueDetatState()).copy(
                experienceMode = currentExperienceMode,
                savedFeltSamples = savedFeltSamples,
            )
            processAndEmitState(initialState, UpdateType.FULL)

            // Fire the version check only after the initial state has been
            // committed, so a network response can't be overwritten by the
            // saved-state load.
            onEvent(MainScreenEvent.CheckForUpdate)
        }

        // Pipe WarningManager's timed messages into uiState.warningText.
        // update{} is a CAS so it does not race with writes from the event loop.
        viewModelScope.launch {
            warningManager.currentWarning.collect { warning ->
                _uiState.update { it.copy(warningText = warning) }
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
                val model = tableScanRepository.load()
                if (model != null) {
                    onEvent(MainScreenEvent.SeedRelocaliser(null))
                }
                // Hold until lifecycle leaves STARTED, so this fires again on next resume
                kotlinx.coroutines.awaitCancellation()
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

        // Expert-AR is delivered on demand and loaded lazily: the split is only
        // fetched the first time a user actually enters the AR camera flow. See the
        // CycleCameraMode interception in processEvent + ensureArModuleLoaded().

        viewModelScope.launch {
            val savedModel = tableScanRepository.load()
            if (savedModel != null) {
                onEvent(MainScreenEvent.LoadTableScan(savedModel))
                checkLocationAndPromptIfNeeded(savedModel)
            }
        }

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
     * Fetch + load the on-demand Expert-AR module if it isn't already, driving the
     * [CueDetatState.arModuleState] lifecycle so the UI can show progress / retry.
     * Idempotent: a no-op once READY or while a load is already in flight.
     * `arController.ensureLoaded()` installs the
     * split (play) before reflectively loading the implementation.
     */
    private fun ensureArModuleLoaded() {
        if (_uiState.value.arModuleState == ArModuleState.READY) return
        if (arModuleLoadJob?.isActive == true) return
        arModuleLoadJob = viewModelScope.launch {
            onEvent(MainScreenEvent.ArModuleLoadStarted)
            // Don't use runCatching: it would swallow CancellationException and
            // (e.g. on Retry, which cancels this job) post a spurious
            // ArModuleLoadFailed that races the freshly started load.
            val loaded = try {
                arController.ensureLoaded()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (t: Throwable) {
                false
            }
            if (loaded) {
                onEvent(MainScreenEvent.DepthCapabilityDetected(arController.probeCapability()))
                onEvent(MainScreenEvent.ArModuleLoadSucceeded)
            } else {
                onEvent(MainScreenEvent.ArModuleLoadFailed)
            }
        }
    }

    private fun processEvent(event: MainScreenEvent) {
        if (event is MainScreenEvent.ScreenGestureStarted || event is MainScreenEvent.LogicalGestureStarted) {
            warningManager.dismissWarning()
        }

        // Lazy Expert-AR delivery. Entering the AR camera flow (CycleCameraMode
        // from OFF) is the trigger to fetch/load the on-demand module; the reducer
        // still performs the camera-mode transition, and the UI shows a download
        // overlay while arModuleState == LOADING. Retry re-runs the same load.
        if (event is MainScreenEvent.CycleCameraMode && _uiState.value.cameraMode == CameraMode.OFF) {
            ensureArModuleLoaded()
        }
        // Lock: hand ARCore the virtual table's corners as they sit on screen, so the anchors land
        // under what the user lined up. If the felt fit is sure enough, snap to it first and lock
        // the fitted outline instead. pitchMatrix maps logical -> screen pixels.
        if (event is MainScreenEvent.LockArTable) {
            val state = _uiState.value
            val c = state.table.corners // TL, TR, BR, BL
            val fit = state.tableFit?.takeIf { it.iou >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.LOCK_MIN_IOU }
            if (fit != null) {
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
        if (event is MainScreenEvent.RetryArModuleLoad) {
            arModuleLoadJob?.cancel()
            arModuleLoadJob = null
            ensureArModuleLoaded()
            return
        }

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
            is MainScreenEvent.SetTopDownBitmap -> reducedState.copy(topDownBitmap = logicalEvent.bitmap)
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
            is MainScreenEvent.ScreenGestureStarted, is MainScreenEvent.LogicalGestureStarted ->
                lastUserTableAdjustMs = now

            is MainScreenEvent.TableFitUpdated -> {
                val fit = event.fit ?: return
                val current = poseOf(after)
                com.hereliesaz.cuedetat.domain.TableSnapPolicy.pullStep(
                    current, fit.pose.let { com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose(it.offsetX, it.offsetY, it.rotationDeg, it.zoom) },
                    fit.iou, now - lastUserTableAdjustMs,
                )?.let { onEvent(MainScreenEvent.ApplyTablePose(it.offsetX, it.offsetY, it.rotationDeg, it.zoom)) }
                if (fit.iou >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.RECORD_MIN_IOU &&
                    now - lastPoseRecordMs >= com.hereliesaz.cuedetat.domain.TableSnapPolicy.RECORD_INTERVAL_MS
                ) {
                    recordPose(after, fit.pose.let { com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose(it.offsetX, it.offsetY, it.rotationDeg, it.zoom) })
                }
            }

            is MainScreenEvent.ArTableLockResult -> if (event.locked) recordPose(after, poseOf(after))

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

    private fun recordPose(state: CueDetatState, pose: com.hereliesaz.cuedetat.domain.TableSnapPolicy.Pose) {
        lastPoseRecordMs = System.currentTimeMillis()
        val o = state.currentOrientation
        val sample = com.hereliesaz.cuedetat.domain.TablePoseSample(
            yawDeg = o.yaw, pitchDeg = o.pitch, rollDeg = o.roll,
            rotationDeg = pose.rotationDeg, zoom = pose.zoom,
            offsetX = pose.offsetX, offsetY = pose.offsetY,
            timestampMs = lastPoseRecordMs,
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

        _uiState.value = derivedState
        visionAnalyzer.updateUiState(derivedState)
        arController.updateUiState(derivedState)
        arController.setTableZOffsetLogical(derivedState.tableZOffset)

        if (derivedState.cameraMode == CameraMode.META_GLASSES) {
            metaWearableRepository.startStreaming()
        } else if (previousState.cameraMode == CameraMode.META_GLASSES) {
            metaWearableRepository.stopStreaming()
        }

        if (type != UpdateType.SPIN_ONLY) {
            val isHighPriority = type == UpdateType.FULL ||
                               state.tableScanModel != previousState.tableScanModel ||
                               state.viewOffset != previousState.viewOffset

            saveJob?.cancel()
            saveJob = viewModelScope.launch {
                try {
                    if (!isHighPriority) delay(2000L)
                    userPreferencesRepository.saveState(derivedState)
                    tableScanRepository.saveFeltSamples(derivedState.savedFeltSamples)
                } catch (t: Throwable) {
                    // saveJob is cancelled and replaced on every subsequent state emit,
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

    private fun determineUpdateType(
        oldState: CueDetatState,
        newState: CueDetatState,
        event: MainScreenEvent
    ): UpdateType {
        return when (event) {
            is MainScreenEvent.FullOrientationChanged -> UpdateType.MATRICES_ONLY

            // World-anchored table pose arrives ~30-60Hz from the ARCore GL thread. It must
            // recompute the matrices each frame so the overlay tracks as the user walks; routing it
            // to AIMING (the default) would silently freeze the table at its first pose.
            is MainScreenEvent.ArTableMatrixUpdated -> UpdateType.MATRICES_ONLY

            is MainScreenEvent.SizeChanged, is MainScreenEvent.ZoomScaleChanged, is MainScreenEvent.ZoomSliderChanged,
            is MainScreenEvent.PanView, is MainScreenEvent.TableRotationChanged,
            is MainScreenEvent.TableRotationApplied, is MainScreenEvent.SetExperienceMode, is MainScreenEvent.ToggleBankingMode,
            is MainScreenEvent.SetTableSize, is MainScreenEvent.RestoreState,
            // MoveTableZ (table-height slider) feeds the rendering matrices in
            // UpdateStateUseCase.updateMatricesAndTransforms (screen-space vertical lift). Without a
            // FULL recompute the new tableZOffset is stored but never applied, so the table doesn't
            // move until some other event happens to recompute the matrices.
            is MainScreenEvent.MoveTableZ,
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
                        _uiState.update { it.copy(latestVersionName = versionName) }
                    } else {
                        // Null here is ambiguous (network/API failure vs. a
                        // genuinely-empty response), but silently doing
                        // nothing leaves the user thinking "no update
                        // available" when the check may simply have failed.
                        // Reuse the existing transient-warning HUD surface
                        // (same one used for e.g. the stale-table-location
                        // notice) rather than adding a new UI mechanism.
                        android.util.Log.w(
                            "MainViewModel",
                            "CheckForUpdate: getLatestVersionName() returned null; " +
                                    "update check may have failed (see GithubRepository logs)"
                        )
                        warningManager.triggerWarning(
                            arrayOf("Couldn't check for updates — check your connection."),
                            viewModelScope
                        )
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

    private companion object {
    }
}
