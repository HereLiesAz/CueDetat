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
import com.hereliesaz.cuedetat.data.MetaWearableRepository
import com.hereliesaz.cuedetat.data.FullOrientation
import com.hereliesaz.cuedetat.data.SensorRepository
import com.hereliesaz.cuedetat.data.TableScanRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.hereliesaz.cuedetat.data.VisionAnalyzer
import com.hereliesaz.cuedetat.data.VisionRepository
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
    val arDepthSession: ArDepthSession,
    val arFrameProcessor: ArFrameProcessor,
    private val entitlementRepository: com.hereliesaz.cuedetat.billing.EntitlementRepository,
    private val integrityRepository: com.hereliesaz.cuedetat.data.IntegrityRepository,
    private val shotAdvisor: com.hereliesaz.cuedetat.domain.advisor.ShotAdvisor,
    private val appUpdater: com.hereliesaz.cuedetat.update.AppUpdater,
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

    /**
     * Forces a re-query of the user's Play subscription status. Called from
     * MainActivity.onResume so we catch state changes (cancel, refund,
     * just-completed purchase) that happened while the app was backgrounded.
     */
    private var lastEntitlementRefresh = 0L

    fun refreshEntitlement() {
        // Battery/network: this fires on every foreground. A just-completed purchase is
        // reflected immediately via the billing client's purchaseUpdates flow, so the
        // foreground re-verification only needs to run occasionally. Coalesce calls within
        // the TTL to avoid a redundant Play query on every resume.
        val now = System.currentTimeMillis()
        if (now - lastEntitlementRefresh < ENTITLEMENT_REFRESH_TTL_MS) return
        lastEntitlementRefresh = now
        viewModelScope.launch {
            runCatching { entitlementRepository.refresh() }
        }
    }

    /** Guards [attemptTesterAutoUnlock] so we only try the silent picker once per process. */
    private var attemptedTesterAutoUnlock = false

    /**
     * Best-effort, no-prompt tester unlock on app start. If the user's Google
     * account is already authorized for this app and is on the build-baked
     * tester allowlist, this grants Expert Mode without them ever having to
     * open the paywall and walk through the tester section. Previously the
     * silent resolve only ran when the paywall sheet was opened, so an
     * allowlisted tester who never opened it never got unlocked.
     *
     * Credential Manager requires an Activity, so this is Activity-bound and
     * invoked from MainActivity. Safe to call on every resume — it self-guards
     * (already entitled / already attempted) and never shows UI.
     */
    fun attemptTesterAutoUnlock(activity: android.app.Activity) {
        if (attemptedTesterAutoUnlock) return
        if (entitlementRepository.entitlement.value.active) return
        attemptedTesterAutoUnlock = true
        viewModelScope.launch {
            runCatching { entitlementRepository.silentlyResolveTesterLicense(activity) }
        }
    }

    /**
     * Performs a Play Integrity check to ensure the app is genuine and the
     * environment is secure. In a production environment, the retrieved
     * token is sent to a secure backend which verifies it against Google's
     * servers to decide whether to grant 'Expert' entitlements.
     */
    private fun performIntegrityCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            val projectNumber = if (com.hereliesaz.cuedetat.BuildConfig.FLAVOR == "play") {
                com.hereliesaz.cuedetat.BuildConfig.GOOGLE_CLOUD_PROJECT_NUMBER
            } else 0L

            val token = if (projectNumber != 0L) {
                android.util.Log.i("MainViewModel", "Using Standard Integrity API with project $projectNumber")
                val requestHash = java.util.UUID.randomUUID().toString()
                integrityRepository.fetchStandardToken(projectNumber, requestHash)
            } else {
                android.util.Log.i("MainViewModel", "Using Snapshot Integrity API (no project number)")
                val nonce = java.util.UUID.randomUUID().toString()
                integrityRepository.fetchSnapshotToken(nonce)
            }

            if (token != null) {
                android.util.Log.i("MainViewModel", "Play Integrity token retrieved successfully.")
                // In a production app, we would send this token to our server here.
                // The server would verify the token and return a cryptographically 
                // signed verdict that the app can trust.
            } else {
                android.util.Log.w("MainViewModel", "Play Integrity check failed to retrieve a token.")
            }
        }
    }

    private var experienceModeUpdateJob: Job? = null
    private var saveJob: Job? = null
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
            // Pull the entitlement live rather than from the JSON snapshot. The
            // entitlement event may already have set _uiState.isExpertEntitled
            // by the time this load completes; in any case the StateFlow is the
            // source of truth.
            val liveEntitled = entitlementRepository.entitlement.value.active
            // Tutorial-seen flags live in their own DataStore keys; the
            // STATE_JSON copy may be stale if the debounced state save was
            // cancelled by a sensor event right after the user finished a
            // tutorial. The dedicated keys are written synchronously on
            // transition, so they're the authoritative source.
            val tutorialSeen = userPreferencesRepository.readTutorialSeenFlags()
            val initialState = (savedState ?: CueDetatState()).copy(
                experienceMode = currentExperienceMode,
                savedFeltSamples = savedFeltSamples,
                isExpertEntitled = liveEntitled,
                hasSeenBeginnerTutorial = tutorialSeen.beginner,
                hasSeenDynamicBeginnerTutorial = tutorialSeen.dynamicBeginner,
                hasSeenExpertTutorial = tutorialSeen.expert,
            )
            processAndEmitState(initialState, UpdateType.FULL)

            // Fire the version check only after the initial state has been
            // committed, so a network response can't be overwritten by the
            // saved-state load.
            onEvent(MainScreenEvent.CheckForUpdate)
            
            // Perform security integrity check on startup.
            performIntegrityCheck()
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

        // Collect entitlement updates and propagate into the reducer. Gated on process
        // lifecycle so it isn't live while backgrounded; entitlement is a StateFlow, so
        // re-collecting on the next foreground immediately replays the current value.
        viewModelScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                entitlementRepository.entitlement.collect { entitlement ->
                    onEvent(MainScreenEvent.EntitlementChanged(entitlement))
                }
            }
        }

        // Onboarding paywall: fire every time the splash is shown and every
        // time the user enters HATER mode while not entitled. Entitled users
        // (including FOSS, which is permanently active) never see it.
        viewModelScope.launch {
            if (entitlementRepository.entitlement.value.active) return@launch
            _uiState
                .map { it.experienceMode to it.isExpertEntitled }
                .distinctUntilChanged()
                .filter { (mode, entitled) ->
                    !entitled && (mode == null || mode == ExperienceMode.HATER)
                }
                .collect {
                    // Re-check live entitlement: _uiState.isExpertEntitled may
                    // still be false on app start if the EntitlementChanged
                    // event hasn't propagated, but the repository's StateFlow
                    // is always current.
                    if (!entitlementRepository.entitlement.value.active) {
                        onEvent(
                            MainScreenEvent.ShowPaywall(
                                com.hereliesaz.cuedetat.billing.PaywallTrigger.ONBOARDING
                            )
                        )
                    }
                }
        }
    }

    fun onEvent(event: MainScreenEvent) {
        eventChannel.trySend(event)
    }

    private fun processEvent(event: MainScreenEvent) {
        if (event is MainScreenEvent.ScreenGestureStarted || event is MainScreenEvent.LogicalGestureStarted) {
            warningManager.dismissWarning()
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

        // Intercept the apply step: if the cycle landed on EXPERT and the user
        // is not entitled, surface the paywall and clear the pending mode so
        // the next cycle starts fresh. The reducer would otherwise silently
        // refuse the transition and the user would see no feedback.
        if (event is MainScreenEvent.ApplyPendingExperienceMode) {
            val target = _uiState.value.pendingExperienceMode
            if (target == ExperienceMode.EXPERT && !_uiState.value.isExpertEntitled) {
                _uiState.value = _uiState.value.copy(pendingExperienceMode = null)
                viewModelScope.launch {
                    _singleEvent.emit(
                        SingleEvent.ShowPaywall(
                            com.hereliesaz.cuedetat.billing.PaywallTrigger.EXPERT_TOGGLE_TAP
                        )
                    )
                }
                return
            }
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
                if (stateAfterSnap.tableScanModel != null &&
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

        // Persist tutorial-seen flags the moment they flip. They live in
        // dedicated DataStore keys (not STATE_JSON) so they're not subject to
        // the 2-second debounce on the state save, which can be cancelled
        // indefinitely by the sensor-driven FullOrientationChanged stream
        // before it ever lands. Without this, the user finishes a tutorial,
        // sensor events keep cancelling the save, the app gets killed, and
        // next launch the tutorial fires again.
        val seenChanged =
            derivedState.hasSeenBeginnerTutorial != previousState.hasSeenBeginnerTutorial ||
            derivedState.hasSeenDynamicBeginnerTutorial != previousState.hasSeenDynamicBeginnerTutorial ||
            derivedState.hasSeenExpertTutorial != previousState.hasSeenExpertTutorial
        if (seenChanged) {
            viewModelScope.launch {
                runCatching {
                    userPreferencesRepository.setTutorialSeenFlags(
                        beginner = derivedState.hasSeenBeginnerTutorial,
                        dynamicBeginner = derivedState.hasSeenDynamicBeginnerTutorial,
                        expert = derivedState.hasSeenExpertTutorial,
                    )
                }.onFailure {
                    android.util.Log.e("MainViewModel", "tutorial-seen flag save failed", it)
                }
            }
        }

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
                        _uiState.update { it.copy(latestVersionName = versionName) }
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
                    } else if (event.mode == ExperienceMode.EXPERT && !_uiState.value.isExpertEntitled) {
                        _singleEvent.emit(SingleEvent.ShowPaywall(com.hereliesaz.cuedetat.billing.PaywallTrigger.SPLASH_SCREEN))
                    }
                }

                is MainScreenEvent.Shake -> _singleEvent.emit(SingleEvent.HaterShake)

                is MainScreenEvent.ShowPaywall -> {
                    _singleEvent.emit(SingleEvent.ShowPaywall(event.trigger))
                }

                else -> { /* Do nothing for state-changing events */ }
            }
        }
    }

    private companion object {
        // Coalesce foreground entitlement re-verifications. Purchases still reflect
        // immediately via the billing client's purchaseUpdates flow.
        const val ENTITLEMENT_REFRESH_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }
}
