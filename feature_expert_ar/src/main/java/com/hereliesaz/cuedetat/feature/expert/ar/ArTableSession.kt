package com.hereliesaz.cuedetat.feature.expert.ar

import android.content.Context
import com.hereliesaz.cuedetat.domain.CloudPlacementRecord
import android.graphics.Matrix
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HostCloudAnchorFuture
import com.google.ar.core.ResolveCloudAnchorFuture
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.domain.ArTrackingMessages
import com.hereliesaz.cuedetat.domain.FeltColorMatch
import com.hereliesaz.cuedetat.domain.QuadOverlap
import com.hereliesaz.cuedetat.domain.ScreenCornerSmoother
import com.hereliesaz.cuedetat.domain.TableReseedPolicy
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiConsumer
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos

/** Felt-vote sampling: a FELT_GRID x FELT_GRID grid across the virtual table. */
private const val FELT_GRID = 5
/** Grid inset from the virtual table's edges, as a fraction, to stay off the cushions. */
private const val FELT_INSET = 0.1f
/** Half-size of the image patch averaged at each grid point, in image pixels. */
private const val FELT_PATCH_PX = 4

/** How long a table stays suspect after tracking returns, a placement or a dropped lock. */
private const val SUSPECT_NANOS = 12_000_000_000L
/** A felt fit older than this describes a camera pose that is gone. */
private const val FIT_FRESH_NANOS = 1_500_000_000L
/** After the user last touched the table, how long before a fit may judge it. */
private const val USER_BUSY_NANOS = 2_000_000_000L
/** A locked anchor that isn't tracking is waited for this long before the lock is dropped. */
private const val LOCK_GRACE_NANOS = 5_000_000_000L
/** An unlock whose pose can't transfer gives up after this. */
private const val UNLOCK_GIVE_UP_NANOS = 3_000_000_000L
/** Fit quality ([com.hereliesaz.cuedetat.domain.TableFitQuality]) needed to place a table on a fit. */
private const val INIT_FIT_QUALITY = 0.75f
/** Below this orientation confidence a fit moves the table but does not turn it. */
private const val ORIENTATION_TRUST = 0.5f
/** The measured table size relative to nominal is believed only within this range. */
private const val SCALE_FIT_MIN = 0.8f
private const val SCALE_FIT_MAX = 1.25f
/** A saved-placement restore keeps looking this long, retrying after each no-match. */
private const val RESTORE_WINDOW_MS = 60_000L
private const val RESTORE_RETRY_DELAY_MS = 2_000L

/** Height and derived pitch for the camera's position above the detected table plane. */
data class CameraAbovePlane(val pitchDegrees: Float, val heightM: Float)

/**
 * Owns the ARCore [Session] and the world anchors that define the table in expert mode.
 *
 * A single ARCore [Anchor] tracks the table centre and yaw. All four corners are computed
 * from its pose and the selected table's immutable metric dimensions, preventing four
 * independent anchors from skewing or stretching a rigid playing surface. The projected
 * rectangle still follows the camera's full 6DoF pose every frame.
 *
 * Before Lock, a metric-sized table is placed once in world space using an ARCore depth hit,
 * horizontal-plane hit, or an explicitly estimated starting distance. Its pose survives camera
 * motion; manual pan, rotation and zoom update that world pose.
 *
 * Lock retains the existing felt-selected horizontal-plane anchoring, and Unlock converts the
 * locked world pose back to an editable preview. ARCore depth is enabled only when supported.
 *
 * Threading: [createSession]/[close]/[pause]/[resume] run on the main thread. [requestLock] and
 * [unlock] are called from the UI thread (atomic hand-off). [computeFrameUpdate] runs on the GL
 * thread and is the only place anchors are created.
 */
/** Manually constructed by ArControllerImpl (lives in the on-demand module; no Hilt here). */
class ArTableSession(
    private val context: Context,
) {
    private var session: Session? = null
    private var tableAnchor: Anchor? = null
    private var supportsDepth = false

    private data class PreviewInput(
        val corners: List<Pt>,
        val panX: Float,
        val panY: Float,
        val rotationDegrees: Float,
        val zoom: Float,
    )
    /** UI snapshots cross onto the GL thread without changing the ARCore anchor. */
    private val previewInput = AtomicReference<PreviewInput?>(null)
    private var previewAnchor: Anchor? = null
    private var previewOffset = Vec3(0f, 0f, 0f)
    private var previewRight = Vec3(1f, 0f, 0f)
    private var previewAway = Vec3(0f, 0f, -1f)
    private var previewInitialRotation = 0f
    private var previousPreviewInput: PreviewInput? = null

    // One locked world anchor, oriented so xAxis is table-right and -zAxis is
    // table-away. ARCore can refine its whole pose without changing the bed size.
    private var lockedTableAnchor: Anchor? = null
    /** True once the table is anchored; ends the plane / point-cloud setup feedback. GL thread. */
    val isTableLocked: Boolean get() = lockedTableAnchor != null
    // A screen-space corner order can have either plane handedness. ARCore anchor
    // rotation alone cannot represent a reflection; retain the calibration parity.
    private var lockedAwaySign = 1f

    // Cloud operations are never automatic. A deliberate user action in the UI
    // queues one operation for the GL thread (the owner of Session.update()).
    private sealed interface CloudRequest {
        data object Save : CloudRequest
        /**
         * [deadlineMs] (0 = not yet set), [notBeforeMs] and [attempt] carry a restore that keeps
         * looking: ARCore answers "no match" until the camera has seen enough of the place.
         */
        data class Restore(
            val logicalCorners: List<Pt>,
            val deadlineMs: Long = 0L,
            val notBeforeMs: Long = 0L,
            val attempt: Int = 1,
        ) : CloudRequest
        data object Forget : CloudRequest
    }
    data class CloudFeedback(val message: String, val restored: Boolean = false)
    private val cloudRequest = AtomicReference<CloudRequest?>(null)
    private val cloudFeedbackQueue = ConcurrentLinkedQueue<CloudFeedback>()
    private var cloudHostFuture: HostCloudAnchorFuture? = null
    private var cloudResolveFuture: ResolveCloudAnchorFuture? = null
    // Ignore results from operations cancelled by Forget or session shutdown.
    @Volatile private var cloudGeneration = 0L
    // Host success and user Forget must serialize their check/write/clear so
    // a cancelled callback cannot repopulate the persisted identifier.
    private val cloudPersistenceLock = Any()
    private val cloudPrefs by lazy {
        context.getSharedPreferences("cuedetat_cloud_table", Context.MODE_PRIVATE)
    }

    fun requestSaveCloudPlacement() { cloudRequest.set(CloudRequest.Save) }

    fun requestRestoreCloudPlacement(logicalCorners: List<Pt>) {
        cloudRequest.set(CloudRequest.Restore(logicalCorners.toList()))
    }

    fun forgetCloudPlacement() {
        // Forget must win over any concurrently completing host callback, even
        // when AR tracking is paused and the GL thread cannot service a request.
        val forgotten = synchronized(cloudPersistenceLock) {
            cloudGeneration++
            cloudPrefs.edit().clear().commit()
        }
        cloudRequest.set(CloudRequest.Forget)
        cloudMessage(
            if (forgotten) "Saved cloud placement forgotten on this device. Google's map expires separately."
            else "Could not persist forgetting this placement; please try again."
        )
    }

    private fun cloudMessage(message: String, restored: Boolean = false) {
        cloudFeedbackQueue.add(CloudFeedback(message, restored))
    }

    private fun cloudFeedback(): List<CloudFeedback> = buildList {
        while (true) add(cloudFeedbackQueue.poll() ?: break)
    }

    private fun savedPlacement(): CloudPlacementRecord? {
        val id = cloudPrefs.getString("id", null) ?: return null
        return CloudPlacementRecord(
            id = id,
            expiresAtMs = cloudPrefs.getLong("expires", 0L),
            logicalWidth = cloudPrefs.getFloat("width", Float.NaN),
            logicalLength = cloudPrefs.getFloat("length", Float.NaN),
            awaySign = cloudPrefs.getFloat("parity", 1f),
        )
    }

    private fun savePlacement(record: CloudPlacementRecord) {
        cloudPrefs.edit()
            .putString("id", record.id)
            .putLong("expires", record.expiresAtMs)
            .putFloat("width", record.logicalWidth)
            .putFloat("length", record.logicalLength)
            .putFloat("parity", record.awaySign)
            .apply()
    }

    /** Enable cloud capability only after explicit user action, never for normal AR. */
    private fun enableCloudMode(s: Session): Boolean = try {
        val config = s.config
        if (config.cloudAnchorMode != Config.CloudAnchorMode.ENABLED) {
            config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
            s.configure(config)
        }
        true
    } catch (e: Exception) {
        cloudMessage("Cloud AR unavailable: ${e.javaClass.simpleName}. Local table Lock still works.")
        false
    }

    private fun processCloudRequest() {
        val request = cloudRequest.getAndSet(null) ?: return
        if (request == CloudRequest.Forget) {
            cloudHostFuture?.cancel()
            cloudResolveFuture?.cancel()
            cloudHostFuture = null
            cloudResolveFuture = null
            return
        }
        val s = session ?: run {
            cloudMessage("ARCore session is not ready."); return
        }
        if (request is CloudRequest.Restore && System.currentTimeMillis() < request.notBeforeMs) {
            // A retry not due yet: put it back unless the user asked for something since.
            cloudRequest.compareAndSet(null, request)
            return
        }
        if (cloudHostFuture != null || cloudResolveFuture != null) {
            cloudMessage("An AR cloud operation is already in progress."); return
        }
        when (request) {
            CloudRequest.Save -> {
                val anchor = lockedTableAnchor
                if (anchor == null || anchor.trackingState != TrackingState.TRACKING ||
                    idealCorners.size != 4
                ) {
                    cloudMessage("Lock the table and wait for tracking before saving."); return
                }
                if (!enableCloudMode(s)) return
                val quality = try { s.estimateFeatureMapQualityForHosting(anchor.pose) }
                    catch (e: Exception) {
                        cloudMessage("Couldn't evaluate cloud map quality: ${e.javaClass.simpleName}")
                        return
                    }
                if (quality == Session.FeatureMapQuality.INSUFFICIENT) {
                    cloudMessage("Move slowly around the table for more visual detail, then save again.")
                    return
                }
                val width = idealCorners[1].x - idealCorners[0].x
                val length = idealCorners[2].y - idealCorners[1].y
                val parity = lockedAwaySign
                val generation = cloudGeneration
                val expiration = System.currentTimeMillis() + CloudPlacementRecord.TTL_MS
                try {
                    cloudHostFuture = s.hostCloudAnchorAsync(
                        anchor, CloudPlacementRecord.TTL_DAYS,
                        BiConsumer<String, Anchor.CloudAnchorState> { id, state ->
                            synchronized(cloudPersistenceLock) {
                                if (session !== s || generation != cloudGeneration) return@BiConsumer
                                cloudHostFuture = null
                                if (state == Anchor.CloudAnchorState.SUCCESS && !id.isNullOrBlank()) {
                                    savePlacement(CloudPlacementRecord(id, expiration, width, length, parity))
                                    cloudMessage("AR placement saved on this device for up to 24 hours.")
                                } else {
                                    cloudMessage("Couldn't save AR placement: $state")
                                }
                            }
                        },
                    )
                    cloudMessage("Saving AR placement to Google. Keep the table in view.")
                } catch (e: Exception) {
                    cloudMessage("Cloud save failed: ${e.javaClass.simpleName}")
                }
            }
            is CloudRequest.Restore -> {
                val saved = savedPlacement()
                val c = request.logicalCorners
                if (c.size != 4) {
                    cloudMessage("Select a valid table size before restoring."); return
                }
                val width = c[1].x - c[0].x
                val length = c[2].y - c[1].y
                if (saved == null || !saved.isUsableAt(System.currentTimeMillis(), width, length)) {
                    cloudMessage("No unexpired placement for this table size. Lock and save again.")
                    return
                }
                if (!enableCloudMode(s)) return
                val generation = cloudGeneration
                val deadline = if (request.deadlineMs > 0L) request.deadlineMs
                    else System.currentTimeMillis() + RESTORE_WINDOW_MS
                try {
                    cloudResolveFuture = s.resolveCloudAnchorAsync(
                        saved.id,
                        BiConsumer<Anchor, Anchor.CloudAnchorState> { anchor, state ->
                            synchronized(cloudPersistenceLock) {
                                if (session !== s || generation != cloudGeneration) {
                                    anchor?.detach()
                                    return@BiConsumer
                                }
                                cloudResolveFuture = null
                                if (state == Anchor.CloudAnchorState.SUCCESS && anchor != null) {
                                    clearAnchors()
                                    clearPreview()
                                    lockedAwaySign = saved.awaySign
                                    idealCorners = c.toList()
                                    lockedTableAnchor = anchor
                                    cloudMessage("Saved table placement restored. Check pocket alignment.", true)
                                } else {
                                    anchor?.detach()
                                    val now = System.currentTimeMillis()
                                    if (state.name in ArTrackingMessages.retryableResolveStates && now < deadline) {
                                        // Not seen yet is not "not here": keep looking while the user scans.
                                        if (request.attempt == 1) cloudMessage(ArTrackingMessages.RESTORE_STILL_LOOKING)
                                        cloudRequest.compareAndSet(
                                            null,
                                            CloudRequest.Restore(c, deadline, now + RESTORE_RETRY_DELAY_MS, request.attempt + 1),
                                        )
                                    } else {
                                        cloudMessage(ArTrackingMessages.resolveFailed(state.name))
                                    }
                                }
                            }
                        },
                    )
                    if (request.attempt == 1) cloudMessage("Looking for the saved table. Scan it from different angles.")
                } catch (e: Exception) {
                    cloudMessage("Cloud restore failed: ${e.javaClass.simpleName}")
                }
            }
            CloudRequest.Forget -> Unit
        }
    }

    /** Screen positions of the virtual table's corners plus their logical twins, awaiting a GL frame. */
    private class LockRequest(
        val screenCorners: List<Pt>,
        val logicalCorners: List<Pt>,
        val feltHsv: FloatArray?,
    )
    private val pendingLock = AtomicReference<LockRequest?>(null)
    private val unlockRequested = AtomicBoolean(false)

    // Ideal logical rectangle (TL, TR, BR, BL) calibrated against the one anchor.
    @Volatile private var idealCorners: List<Pt> = emptyList()

    // Height of the table plane above the corner anchors, in metres, driven by the tableZOffset
    // slider.
    @Volatile private var tableHeightMeters: Float = 0f

    /**
     * Steadies the projected corners (GL thread only). Reset whenever there is no matrix, and when
     * the table changes between preview and locked, so it never smooths across a discontinuity.
     */
    private val cornerSmoother = ScreenCornerSmoother()
    private var smoothedLocked: Boolean? = null

    /** Physical size of the table relative to nominal (CueDetatState.tableFitScale). */
    @Volatile private var tableScale: Float = 1f

    /**
     * Range nudges from the UI thread (ArController.nudgeTableRange), multiplied together until
     * the GL thread takes them. A ratio of apparent size: 2 puts the table at half the range.
     */
    private val nudgeLock = Any()
    private var pendingRangeNudge = 1f

    /** UI thread. Accumulates a relative range change for the next frame; see [takeRangeNudge]. */
    fun nudgeRange(factor: Float) {
        if (!factor.isFinite() || factor <= 0f) return
        lastUserChangeNanos = System.nanoTime()
        synchronized(nudgeLock) { pendingRangeNudge = (pendingRangeNudge * factor).coerceIn(0.05f, 20f) }
    }

    private fun takeRangeNudge(): Float = synchronized(nudgeLock) {
        pendingRangeNudge.also { pendingRangeNudge = 1f }
    }

    /**
     * What the felt fit last said (CueDetatState.tableFit), taken on the UI thread, read on the GL
     * thread. [identity] is the fit object itself: a new object is a new fit.
     */
    private class FitSnapshot(
        val identity: Any,
        val quad: List<Pt>,
        val quality: Float,
        val orientationConfidence: Float,
        val atNanos: Long,
    )
    @Volatile private var latestFit: FitSnapshot? = null
    @Volatile private var lastUserChangeNanos = 0L
    /** Lock View: nothing sensor-driven may move the table, and a reseed is one. */
    @Volatile private var viewLocked = false
    private var lastFitHandled: Any? = null

    // After tracking returns from a loss, a table is placed fresh, or a lock is dropped, the table
    // is suspect for a while: a sure felt fit that keeps disagreeing with it puts it back.
    private val reseedPolicy = TableReseedPolicy()
    private var suspectUntilNanos = 0L
    private var lastScreenQuad: List<Pt>? = null
    private var lockedBadSinceNanos = 0L
    @Volatile private var unlockWaitingSinceNanos = 0L

    /** Status lines for the UI, drained each frame ([FrameUpdate.notices]). */
    private val notices = ConcurrentLinkedQueue<String>()
    private var lastStoppedNoticeNanos = 0L
    private var lockLostMessage: String? = null
    private var suggestedScale: Float? = null

    /** True once ARCore world tracking is available on this device. */
    fun isArCoreAvailable(): Boolean = try {
        ArCoreApk.getInstance().checkAvailability(context).isSupported
    } catch (_: Exception) {
        false
    }

    /** Tracking availability and depth availability are independent capabilities. */
    val capability: DepthCapability
        get() = when {
            !isArCoreAvailable() -> DepthCapability.NONE
            supportsDepth -> DepthCapability.DEPTH_API
            else -> DepthCapability.WORLD_TRACKING
        }

    /**
     * Creates ARCore world tracking, enabling depth only where supported.
     * Must be called on the main thread before the GL surface is created. Returns null if ARCore
     * is unavailable.
     */
    fun createSession(): Session? {
        if (!isArCoreAvailable()) return null
        // A previous session (e.g. a composition that never disposed) is closed, not leaked:
        // ARCore holds the camera per Session.
        if (session != null) close()
        var s: Session? = null
        return try {
            val created = Session(context).also { s = it }
            val depth = created.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            val config = Config(created).apply {
                depthMode = if (depth) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                lightEstimationMode = Config.LightEstimationMode.DISABLED
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }
            created.configure(config)
            synchronized(frameLock) {
                supportsDepth = depth
                session = created
            }
            created
        } catch (_: Exception) {
            runCatching { s?.close() }
            null
        }
    }

    /**
     * Reports [DepthCapability] without disturbing a live session. If one is open its
     * capability is returned directly; otherwise a throwaway probe Session is created, queried
     * and closed. The shared [session] is never created or closed here.
     */
    fun probeCapability(): DepthCapability {
        if (!isArCoreAvailable()) return DepthCapability.NONE
        if (session != null) return capability
        val probe = try { Session(context) } catch (_: Exception) { return DepthCapability.NONE }
        return try {
            if (probe.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) DepthCapability.DEPTH_API
            else DepthCapability.WORLD_TRACKING
        } catch (_: Exception) {
            DepthCapability.WORLD_TRACKING
        } finally {
            runCatching { probe.close() }
        }
    }



    /** Update editable table controls without ever re-anchoring on camera-pose updates. */
    fun updatePreviewInput(state: CueDetatState) {
        // Size applies to the locked table too, so it is taken before the preview early-out.
        if (tableScale != state.tableFitScale) lastUserChangeNanos = System.nanoTime()
        tableScale = state.tableFitScale
        viewLocked = state.isViewLocked
        val fit = state.tableFit
        if (fit == null) latestFit = null
        else if (latestFit?.identity !== fit) {
            latestFit = FitSnapshot(
                fit, fit.viewQuad.map { Pt(it.x, it.y) }, fit.quality, fit.orientationConfidence, System.nanoTime(),
            )
        }
        if (state.cameraMode != CameraMode.AR_ACTIVE || !state.table.isVisible) {
            previewInput.set(null)
            return
        }
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode, state.isBeginnerViewLocked)
        val input = PreviewInput(
            corners = state.table.corners.map { Pt(it.x, it.y) },
            panX = state.viewOffset.x,
            panY = state.viewOffset.y,
            rotationDegrees = state.userRotationDegrees,
            zoom = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom),
        )
        // Any change to the controls is the user's hand on the table: no reseed while it moves.
        if (previewInput.getAndSet(input).let { it != null && it != input }) {
            lastUserChangeNanos = System.nanoTime()
        }
    }

    /**
     * Called on the GL thread when camera tracking comes back after a loss. The world may have
     * been re-found in the wrong place, so the table is suspect for [SUSPECT_NANOS]; the smoother
     * forgets the pre-loss corners so the table doesn't glide in from where it was.
     */
    fun onTrackingResumed() {
        beginSuspicion()
        cornerSmoother.reset()
    }

    private fun beginSuspicion() {
        suspectUntilNanos = System.nanoTime() + SUSPECT_NANOS
        reseedPolicy.reset()
        lastFitHandled = null
    }
    fun getSession(): Session? = session

    /** Sets the table-plane lift above the corner anchors (tableZOffset slider, in metres). */
    fun setTableHeightMeters(meters: Float) {
        tableHeightMeters = meters
    }

    /**
     * Queues a lock. [screenCorners] are where the virtual table's corners sit on screen right now
     * (TL, TR, BR, BL, in view pixels); [logicalCorners] are the same corners in logical space.
     * [feltHsv] is the captured felt colour (Android HSV); when present it picks the table's
     * plane (see [findTablePlanePose]). The next GL frame anchors them; the outcome arrives as
     * [FrameUpdate.lockResult].
     */
    fun requestLock(screenCorners: List<Pt>, logicalCorners: List<Pt>, feltHsv: FloatArray?) {
        if (screenCorners.size != 4 || logicalCorners.size != 4) return
        unlockRequested.set(false)
        pendingLock.set(LockRequest(screenCorners, logicalCorners, feltHsv))
    }

    /** Release Lock but retain its location as the editable metric preview. */
    fun unlock() {
        pendingLock.set(null)
        unlockWaitingSinceNanos = 0L
        unlockRequested.set(true)
    }

    /** Detaches the corner anchors (rescan / cancel). */
    fun clearAnchors() {
        lockedTableAnchor?.detach()
        lockedTableAnchor = null
        idealCorners = emptyList()
    }

    /**
     * [matrix]: logical->screen homography from the locked table or metric preview.
     * [lockResult]: non-null only on the frame that served a [requestLock] (true = locked).
     */
    data class FrameUpdate(
        val matrix: Matrix?,
        val lockResult: Boolean?,
        val cloudFeedback: List<CloudFeedback>,
        /** Status lines raised this frame, in order. */
        val notices: List<String> = emptyList(),
        /** Non-null when the locked table's anchor was lost for good and the lock dropped. */
        val lockLost: String? = null,
        /** The real table's size relative to nominal, measured when the table was re-seeded. */
        val suggestedScale: Float? = null,
    )

    /**
     * Per-frame update on the GL thread: serves any queued lock/unlock, then fits the
     * logical->screen homography from the corner anchors.
     */
    fun computeFrameUpdate(frame: Frame, vpW: Int, vpH: Int): FrameUpdate {
        val now = System.nanoTime()
        if (unlockRequested.getAndSet(false)) {
            val locked = lockedTableAnchor
            when {
                locked == null -> unlockWaitingSinceNanos = 0L
                adoptLockedPoseAsPreview() -> {
                    clearAnchors()
                    unlockWaitingSinceNanos = 0L
                }
                locked.trackingState == TrackingState.STOPPED ||
                    (unlockWaitingSinceNanos != 0L && now - unlockWaitingSinceNanos > UNLOCK_GIVE_UP_NANOS) -> {
                    // The pose will never transfer; the table is placed afresh rather than the
                    // unlock waiting forever on an anchor that is gone.
                    clearAnchors()
                    unlockWaitingSinceNanos = 0L
                    notices.add("The locked pose was lost. Placing the table again.")
                }
                else -> {
                    // ARCore can pause an anchor briefly while relocalizing; don't destroy the
                    // locked table until its pose has transferred, or until that clearly won't.
                    if (unlockWaitingSinceNanos == 0L) unlockWaitingSinceNanos = now
                    unlockRequested.set(true)
                }
            }
        }
        checkLockedAnchor(now)

        processCloudRequest()

        val view = FloatArray(16)
        val proj = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)

        // Taken every frame so a nudge made while locked is dropped, not applied on Unlock.
        val rangeNudge = takeRangeNudge()
        if (lockedTableAnchor == null) {
            updatePreview(frame, view, proj, vpW, vpH, rangeNudge)
            considerFit(frame, now, view, proj, vpW, vpH)
        }
        val lockResult = pendingLock.getAndSet(null)?.let { performLock(frame, it, view, proj, vpW, vpH) }
        val matrix = buildTableMatrix(view, proj, vpW, vpH, frame.timestamp)
        if (matrix == null) {
            cornerSmoother.reset()
            lastScreenQuad = null
        }
        val update = FrameUpdate(
            matrix, lockResult, cloudFeedback(),
            notices = buildList { while (true) add(notices.poll() ?: break) },
            lockLost = lockLostMessage,
            suggestedScale = suggestedScale,
        )
        lockLostMessage = null
        suggestedScale = null
        return update
    }

    /**
     * A locked table whose anchor ARCore has stopped (or has not tracked for [LOCK_GRACE_NANOS])
     * is not coming back: drop the lock and say so, instead of leaving a "locked" table that is
     * never drawn and an Unlock that cannot finish.
     */
    private fun checkLockedAnchor(now: Long) {
        val locked = lockedTableAnchor ?: run { lockedBadSinceNanos = 0L; return }
        when (locked.trackingState) {
            TrackingState.TRACKING -> lockedBadSinceNanos = 0L
            TrackingState.STOPPED -> loseLock()
            else -> {
                if (lockedBadSinceNanos == 0L) lockedBadSinceNanos = now
                else if (now - lockedBadSinceNanos > LOCK_GRACE_NANOS) loseLock()
            }
        }
    }

    private fun loseLock() {
        clearAnchors()
        lockedBadSinceNanos = 0L
        lockLostMessage = "Tracking lost the locked table. Line it up and lock again."
        beginSuspicion()
    }

    /**
     * Weighs a new felt fit against the table as projected. Only while the table is suspect, only
     * for a fit sure enough to believe, never while the user has a hand on the controls or Lock
     * View is on, and only through [TableReseedPolicy] (several fits in a row, a couple of times
     * at most). See that class for why the preview is not simply pulled to every fit.
     */
    private fun considerFit(frame: Frame, now: Long, view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int) {
        val fit = latestFit ?: return
        if (fit.identity === lastFitHandled) return
        lastFitHandled = fit.identity
        if (now > suspectUntilNanos || now - fit.atNanos > FIT_FRESH_NANOS) return
        val ours = lastScreenQuad ?: return
        val busy = viewLocked || now - lastUserChangeNanos < USER_BUSY_NANOS
        when (reseedPolicy.onFit(QuadOverlap.iou(ours, fit.quad), fit.quality, busy)) {
            TableReseedPolicy.Verdict.VERIFIED -> suspectUntilNanos = 0L
            TableReseedPolicy.Verdict.RESEED -> {
                val input = previewInput.get() ?: return
                seedPreviewFromQuad(frame, fit.quad, fit.orientationConfidence, input, view, proj, vpW, vpH)
                    ?.let { suggestedScale = it }
            }
            TableReseedPolicy.Verdict.NONE -> Unit
        }
    }

    /**
     * Puts the preview where the felt outline [quad] (view pixels, TL, TR, BR, BL) says the table
     * is: the outline's corners are cast onto the table plane (found by a depth or plane hit at
     * the outline's centre) and the table centred and turned to match. Returns the measured size
     * relative to nominal when it is believable (see [SCALE_FIT_MIN]), else null. A null return
     * does not mean nothing changed: if [previewAnchor] is new, the table was re-seeded.
     *
     * The felt can't tell the table's ends apart, so the heading closest to the current one is
     * taken; and when the fit isn't sure of the heading ([orientationConfidence] under
     * [ORIENTATION_TRUST]) the current heading stays and only the position moves.
     */
    private fun seedPreviewFromQuad(
        frame: Frame, quad: List<Pt>, orientationConfidence: Float, input: PreviewInput,
        view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int,
    ): Float? {
        val s = session ?: return null
        if (quad.size != 4) return null
        val viewProj = FloatArray(16)
        val inverse = FloatArray(16)
        android.opengl.Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        if (!android.opengl.Matrix.invertM(inverse, 0, viewProj, 0)) return null
        val cx = quad.sumOf { it.x.toDouble() }.toFloat() / 4f
        val cy = quad.sumOf { it.y.toDouble() }.toFloat() / 4f
        val hits = frame.hitTest(cx, cy)
        val hit = hits.firstOrNull { supportsDepth && it.trackable is DepthPoint }
            ?: hits.firstOrNull { h ->
                val p = h.trackable
                p is Plane && p.type == Plane.Type.HORIZONTAL_UPWARD_FACING && p.isPoseInPolygon(h.hitPose)
            }
        val planeY = hit?.hitPose?.ty() ?: previewAnchor?.pose?.ty() ?: return null
        val worlds = quad.map {
            TableFrameHomography.screenToPlane(
                inverse, it.x, it.y, vpW, vpH, Vec3(0f, planeY, 0f), Vec3(0f, 1f, 0f),
            ) ?: return null
        }
        val calibrated = TableFrameHomography.calibrateHorizontalRectangle(worlds) ?: return null
        // The preview's axes are always right = (-away.z, away.x); a mirrored reading is no table.
        if (calibrated.right.z * calibrated.away.x - calibrated.right.x * calibrated.away.z <= 0f) return null

        val (curRight, curAway) = TableFrameHomography.headingAfterYaw(
            previewRight, previewAway,
            TableFrameHomography.clockwiseToWorldYaw(input.rotationDegrees - previewInitialRotation),
        )
        val fitRight = calibrated.right
        val fitAway = calibrated.away
        val sameEnds = fitAway.x * curAway.x + fitAway.z * curAway.z >= 0f
        val (newRight, newAway) = when {
            orientationConfidence < ORIENTATION_TRUST -> curRight to curAway
            sameEnds -> fitRight to fitAway
            else -> Vec3(-fitRight.x, 0f, -fitRight.z) to Vec3(-fitAway.x, 0f, -fitAway.z)
        }
        val anchor = try {
            s.createAnchor(Pose(
                floatArrayOf(calibrated.center.x, calibrated.center.y, calibrated.center.z),
                floatArrayOf(0f, 0f, 0f, 1f),
            ))
        } catch (_: Exception) { null } ?: return null

        clearPreview()
        previewAnchor = anchor
        previewRight = newRight
        previewAway = newAway
        previewOffset = Vec3(0f, 0f, 0f)
        previewInitialRotation = input.rotationDegrees
        previousPreviewInput = input
        beginSuspicion()

        val measured = hypot((worlds[1].x - worlds[0].x).toDouble(), (worlds[1].z - worlds[0].z).toDouble()).toFloat()
        val nominal = (input.corners[1].x - input.corners[0].x) / TableFrameHomography.LOGICAL_UNITS_PER_METER
        val ratio = if (nominal > 0f) measured / nominal else return null
        return ratio.takeIf { it in SCALE_FIT_MIN..SCALE_FIT_MAX }
    }

    /**
     * Create one stationary world-space anchor. Depth/plane hits supply real metres; without
     * either the 1.8m forward / 0.45m below-camera starting point is explicitly an estimate.
     * The app never claims that estimate is a recognized table.
     */
    private fun initializePreview(
        frame: Frame, input: PreviewInput, view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int,
    ) {
        val s = session ?: return
        // A fit from just before (the table was lost, not first placed) puts it back on the felt.
        latestFit?.takeIf {
            System.nanoTime() - it.atNanos < FIT_FRESH_NANOS && it.quality >= INIT_FIT_QUALITY
        }?.let { fit ->
            val ratio = seedPreviewFromQuad(frame, fit.quad, fit.orientationConfidence, input, view, proj, vpW, vpH)
            if (previewAnchor != null) {
                ratio?.let { suggestedScale = it }
                return
            }
        }
        val camera = frame.camera.pose
        val cameraBack = camera.zAxis
        val horizontal = hypot(cameraBack[0].toDouble(), cameraBack[2].toDouble()).toFloat()
        if (horizontal < 0.05f) return
        val away = Vec3(-cameraBack[0] / horizontal, 0f, -cameraBack[2] / horizontal)
        val right = Vec3(-away.z, 0f, away.x)
        val hits = frame.hitTest(vpW / 2f, vpH / 2f)
        val hit = hits.firstOrNull { supportsDepth && it.trackable is DepthPoint }
            ?: hits.firstOrNull { h ->
                val p = h.trackable
                p is Plane && p.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                    && p.isPoseInPolygon(h.hitPose)
            }
        val origin = if (hit != null) {
            Vec3(hit.hitPose.tx(), hit.hitPose.ty(), hit.hitPose.tz())
        } else {
            Vec3(camera.tx() + away.x * 1.8f, camera.ty() - 0.45f,
                camera.tz() + away.z * 1.8f)
        }
        previewAnchor = try {
            s.createAnchor(Pose(floatArrayOf(origin.x, origin.y, origin.z),
                floatArrayOf(0f, 0f, 0f, 1f)))
        } catch (_: Exception) { null }
        if (previewAnchor != null) {
            previewRight = right
            previewAway = away
            previewOffset = Vec3(0f, 0f, 0f)
            previewInitialRotation = input.rotationDegrees
            previousPreviewInput = input
            // Placed on a guess or a hit, not on the felt: the first sure fit may correct it.
            beginSuspicion()
        }
    }

    /**
     * Integrate *only explicit control changes* against the current world pose. In particular,
     * zoom changes camera-to-table range with the pinhole 1/d law; it cannot scale the table
     * itself or deform its perspective as the camera pans.
     */
    private fun updatePreview(
        frame: Frame, view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int, rangeNudge: Float,
    ) {
        val input = previewInput.get()
        if (input == null) {
            clearPreview()
            return
        }
        if (previewAnchor == null) initializePreview(frame, input, view, proj, vpW, vpH)
        val anchor = previewAnchor ?: return
        if (anchor.trackingState == TrackingState.STOPPED) {
            // ARCore has dropped this anchor for good; it will never be TRACKING again. Without
            // this the table stays hidden for the rest of the session.
            clearPreview()
            val now = System.nanoTime()
            // Said once in a while, not every frame if anchors keep failing.
            if (now - lastStoppedNoticeNanos > 3_000_000_000L) {
                lastStoppedNoticeNanos = now
                notices.add("Tracking lost the table's place. Placing it again.")
            }
            return
        }
        if (anchor.trackingState != TrackingState.TRACKING) return
        val previous = previousPreviewInput ?: input
        val pose = anchor.pose
        val center = Vec3(pose.tx() + previewOffset.x,
            pose.ty() + previewOffset.y, pose.tz() + previewOffset.z)
        val dx = input.panX - previous.panX
        val dy = input.panY - previous.panY

        if (abs(dx) + abs(dy) > 0.001f) {
            val vp = FloatArray(16)
            val inverse = FloatArray(16)
            android.opengl.Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
            if (android.opengl.Matrix.invertM(inverse, 0, vp, 0)) {
                val normal = Vec3(0f, 1f, 0f)
                val from = TableFrameHomography.screenToPlane(
                    inverse, vpW / 2f, vpH / 2f, vpW, vpH, center, normal)
                val to = TableFrameHomography.screenToPlane(
                    inverse, vpW / 2f + dx, vpH / 2f + dy,
                    vpW, vpH, center, normal)
                if (from != null && to != null) {
                    val shiftX = to.x - from.x
                    val shiftZ = to.z - from.z
                    // Reject a nearly horizontal ray's pathological intersection distance.
                    if (hypot(shiftX.toDouble(), shiftZ.toDouble()) <= 0.5) {
                        previewOffset = Vec3(previewOffset.x + shiftX,
                            previewOffset.y, previewOffset.z + shiftZ)
                    }
                }
            }
        }
        if (previous.zoom > 0f && input.zoom > 0f && abs(input.zoom - previous.zoom) > 1e-5f) {
            dollyPreview(frame, pose, previous.zoom / input.zoom)
        }
        if (rangeNudge != 1f) dollyPreview(frame, pose, 1f / rangeNudge)
        previousPreviewInput = input
    }

    /**
     * Moves the preview along the camera's line to the table so its range becomes [rangeRatio]
     * times what it is now, within 0.65-8 m. The table keeps its size and heading.
     */
    private fun dollyPreview(frame: Frame, anchorPose: Pose, rangeRatio: Float) {
        val cam = frame.camera.pose
        val relX = anchorPose.tx() + previewOffset.x - cam.tx()
        val relZ = anchorPose.tz() + previewOffset.z - cam.tz()
        val currentRange = hypot(relX.toDouble(), relZ.toDouble()).toFloat()
        if (currentRange <= 0.05f) return
        val desiredRange = (currentRange * rangeRatio).coerceIn(0.65f, 8f)
        previewOffset = Vec3(
            previewOffset.x + relX * (desiredRange / currentRange - 1f),
            previewOffset.y,
            previewOffset.z + relZ * (desiredRange / currentRange - 1f),
        )
    }

    /** Transfer the locked pose to an editable preview without changing its size or heading. */
    private fun adoptLockedPoseAsPreview(): Boolean {
        val locked = lockedTableAnchor ?: return false
        if (locked.trackingState != TrackingState.TRACKING) return false
        val input = previewInput.get() ?: return false
        val anchor = try { session?.createAnchor(locked.pose) } catch (_: Exception) { null }
            ?: return false
        clearPreview()
        previewAnchor = anchor
        val right = anchor.pose.xAxis
        val zAxis = anchor.pose.zAxis
        previewRight = Vec3(right[0], 0f, right[2])
        previewAway = Vec3(-zAxis[0] * lockedAwaySign, 0f, -zAxis[2] * lockedAwaySign)
        previewInitialRotation = input.rotationDegrees
        previousPreviewInput = input
        return true
    }

    private fun clearPreview() {
        previewAnchor?.detach()
        previewAnchor = null
        previewOffset = Vec3(0f, 0f, 0f)
        previousPreviewInput = null
    }

    /**
     * Casts each virtual corner onto the table plane and anchors it. Casting onto the infinite
     * plane (rather than hit-testing each corner) matters: felt is too plain for ARCore to map
     * edge to edge, so the corners usually fall outside the detected polygon.
     */
    private fun performLock(
        frame: Frame, req: LockRequest,
        view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int,
    ): Boolean {
        val s = session ?: return false
        val planePose = findTablePlanePose(frame, req, vpW, vpH) ?: return false

        val viewProj = FloatArray(16)
        val invViewProj = FloatArray(16)
        android.opengl.Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        if (!android.opengl.Matrix.invertM(invViewProj, 0, viewProj, 0)) return false

        val planePoint = Vec3(planePose.tx(), planePose.ty(), planePose.tz())
        val y = planePose.yAxis
        val planeNormal = Vec3(y[0], y[1], y[2])

        val worlds = req.screenCorners.map { c ->
            TableFrameHomography.screenToPlane(invViewProj, c.x, c.y, vpW, vpH, planePoint, planeNormal)
                ?: return false
        }
        // Depth/felt votes select a physical plane; projected points supply centre
        // and heading only. The playing bed always uses exact logical dimensions.
        val calibrated = TableFrameHomography.calibrateHorizontalRectangle(worlds)
            ?: return false
        val yaw = -atan2(calibrated.right.z, calibrated.right.x)
        val half = yaw / 2f
        val lockedPose = Pose(
            floatArrayOf(calibrated.center.x, calibrated.center.y, calibrated.center.z),
            floatArrayOf(0f, sin(half), 0f, cos(half)),
        )
        val anchor = try { s.createAnchor(lockedPose) } catch (_: Exception) { return false }
        clearAnchors()
        clearPreview()
        lockedAwaySign = TableFrameHomography.awayHandedness(calibrated)
        idealCorners = req.logicalCorners.toList()
        lockedTableAnchor = anchor
        return true
    }

    /**
     * The plane the table sits on, in order of trust:
     * 1. Felt vote. Points spread across the virtual table are kept only where the camera image
     *    there is felt-coloured ([FeltColorMatch]); each is hit-tested, and the horizontal plane
     *    hit most often wins. This is what keeps the table off the floor: ARCore finds the floor
     *    and chairs too, but only the table is felt.
     * 2. Whatever horizontal plane lies under the virtual table's centre.
     * 3. The first horizontal plane ARCore found ([tableAnchor]).
     */
    private fun findTablePlanePose(frame: Frame, req: LockRequest, vpW: Int, vpH: Int): Pose? {
        fun planeHit(x: Float, y: Float) = frame.hitTest(x, y).firstOrNull { hit ->
            val t = hit.trackable
            t is Plane && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING && t.isPoseInPolygon(hit.hitPose)
        }

        req.feltHsv?.let { felt -> feltVotedPlanePose(frame, req.screenCorners, felt, vpW, vpH, ::planeHit) }
            ?.let { return it }

        val cx = req.screenCorners.sumOf { it.x.toDouble() }.toFloat() / 4f
        val cy = req.screenCorners.sumOf { it.y.toDouble() }.toFloat() / 4f
        planeHit(cx, cy)?.let { return it.hitPose }

        return tableAnchor?.takeIf { it.trackingState == TrackingState.TRACKING }?.pose
    }

    private fun feltVotedPlanePose(
        frame: Frame, corners: List<Pt>, felt: FloatArray, vpW: Int, vpH: Int,
        planeHit: (Float, Float) -> HitResult?,
    ): Pose? {
        val image = try { frame.acquireCameraImage() } catch (_: Exception) { return null }
        try {
            val votes = mutableMapOf<Plane, MutableList<Pose>>()
            val view = FloatArray(2)
            val img = FloatArray(2)
            for (i in 0 until FELT_GRID) for (j in 0 until FELT_GRID) {
                // Bilinear point inside the quad TL, TR, BR, BL, inset off the cushions.
                val u = FELT_INSET + (1f - 2 * FELT_INSET) * i / (FELT_GRID - 1)
                val v = FELT_INSET + (1f - 2 * FELT_INSET) * j / (FELT_GRID - 1)
                val (tl, tr, br, bl) = corners
                val x = (1 - v) * ((1 - u) * tl.x + u * tr.x) + v * ((1 - u) * bl.x + u * br.x)
                val y = (1 - v) * ((1 - u) * tl.y + u * tr.y) + v * ((1 - u) * bl.y + u * br.y)
                if (x < 0f || y < 0f || x >= vpW || y >= vpH) continue

                view[0] = x; view[1] = y
                frame.transformCoordinates2d(Coordinates2d.VIEW, view, Coordinates2d.IMAGE_PIXELS, img)
                val px = img[0].toInt(); val py = img[1].toInt()
                val hsv = sampleYuvHsv(
                    image, px - FELT_PATCH_PX, py - FELT_PATCH_PX, px + FELT_PATCH_PX, py + FELT_PATCH_PX,
                    samplesPerSide = 4,
                ) ?: continue
                if (!FeltColorMatch.matches(hsv, felt)) continue

                val hit = planeHit(x, y) ?: continue
                votes.getOrPut(hit.trackable as Plane) { mutableListOf() }.add(hit.hitPose)
            }
            return votes.maxByOrNull { it.value.size }?.value?.first()
        } finally {
            image.close()
        }
    }

    private fun buildTableMatrix(
        view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int, frameNanos: Long,
    ): Matrix? {
        val locked = lockedTableAnchor
        if (smoothedLocked != (locked != null)) {
            smoothedLocked = locked != null
            cornerSmoother.reset()
        }
        val cornersWorld: List<Vec3>
        val logical: List<Pt>
        if (locked != null) {
            if (locked.trackingState != TrackingState.TRACKING || idealCorners.size != 4) return null
            val pose = locked.pose
            val xAxis = pose.xAxis
            val zAxis = pose.zAxis
            val center = Vec3(pose.tx(), pose.ty(), pose.tz())
            logical = idealCorners
            cornersWorld = TableFrameHomography.metricRectangleCorners(
                logical, center,
                Vec3(xAxis[0], 0f, xAxis[2]),
                Vec3(-zAxis[0] * lockedAwaySign, 0f, -zAxis[2] * lockedAwaySign),
                0f,
                tableScale,
            )
        } else {
            val anchor = previewAnchor ?: return null
            val input = previewInput.get() ?: return null
            if (anchor.trackingState != TrackingState.TRACKING) return null
            val pose = anchor.pose
            val center = Vec3(pose.tx() + previewOffset.x,
                pose.ty() + previewOffset.y, pose.tz() + previewOffset.z)
            logical = input.corners
            cornersWorld = TableFrameHomography.metricRectangleCorners(
                logical, center, previewRight, previewAway,
                TableFrameHomography.clockwiseToWorldYaw(
                    input.rotationDegrees - previewInitialRotation),
                tableScale)
        }
        val h = TableFrameHomography.computeLogicalToScreen(
            view, proj, cornersWorld, logical, vpW, vpH, heightMeters = tableHeightMeters,
            smooth = { corners -> cornerSmoother.filter(corners, frameNanos).also { lastScreenQuad = it } },
        ) ?: return null
        return Matrix().apply { setValues(h) }
    }

     // --- Plane-anchor pitch fallback (used while the table is not locked) ---

    /**
     * Searches updated trackables for a horizontal plane matching pool-table dimensions and anchors
     * to it. Used to warm tracking and provide a pitch estimate before the table is locked.
     */
    fun findAndAnchorTablePlane(frame: Frame) {
        if (tableAnchor != null) return
        val plane = frame.getUpdatedTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
            .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
        plane?.let {
            try {
                tableAnchor = it.createAnchor(it.centerPose)
            } catch (_: Exception) { /* retry next frame */ }
        }
    }

    /**
     * Computes the camera's viewing elevation above the anchored table plane (real geometry, not
     * raw device tilt). Returns null if no anchor is established or tracking is lost.
     */
    fun computeCameraAbovePlane(frame: Frame): CameraAbovePlane? {
        val anchor = tableAnchor ?: return null
        if (anchor.trackingState != TrackingState.TRACKING) return null

        val cameraInAnchor = anchor.pose.inverse().compose(frame.camera.pose)
        val heightM = cameraInAnchor.ty()
        if (heightM <= 0f) return null

        val horizontal = sqrt(cameraInAnchor.tx().pow(2) + cameraInAnchor.tz().pow(2))
        if (horizontal < 0.01f) return null

        val pitchDeg = Math.toDegrees(atan2(heightM.toDouble(), horizontal.toDouble()))
            .toFloat().coerceIn(5f, 85f)
        return CameraAbovePlane(pitchDegrees = pitchDeg, heightM = heightM)
    }

    fun clearPlaneAnchor() {
        tableAnchor?.detach()
        tableAnchor = null
    }

    fun pause() { session?.pause() }
    fun resume() { try { session?.resume() } catch (_: Exception) {} }
    /**
     * Held by the GL renderer for each whole frame (ArCoreBackground.onDrawFrame) and by
     * [close] and the session swap in [createSession], so the Session is never closed or
     * replaced while `session.update()` is running.
     */
    val frameLock = Any()

    fun close(): Unit = synchronized(frameLock) {
        synchronized(cloudPersistenceLock) { cloudGeneration++ }
        cloudRequest.set(null)
        cloudHostFuture?.cancel()
        cloudResolveFuture?.cancel()
        cloudHostFuture = null
        cloudResolveFuture = null
        cloudFeedbackQueue.clear()
        clearAnchors()
        clearPreview()
        clearPlaneAnchor()
        previewInput.set(null)
        session?.close()
        session = null
        supportsDepth = false
    }
}
