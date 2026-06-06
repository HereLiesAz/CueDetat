package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/** Height and derived pitch for the camera's position above the detected table plane. */
data class CameraAbovePlane(val pitchDegrees: Float, val heightM: Float)

/**
 * Owns the ARCore [Session] and the world anchors that define the table in expert mode.
 *
 * Each corner pocket the user captures becomes a world [Anchor] dropped via a hit-test at the
 * screen centre. ARCore's visual-inertial tracking keeps those anchors fixed as the user walks
 * around, so the table persists and the overlay tracks in full 6DoF without any appearance-based
 * relocalisation. Every frame [computeFrameUpdate] re-projects the anchors to screen and fits the
 * logical->screen homography that the 2D Canvas renderer applies.
 *
 * The ARCore Depth API is intentionally disabled — corner anchors only need plane-finding and
 * hit-testing, and the depth stream was a per-frame battery cost feeding only a pitch fallback.
 *
 * Threading: [createSession]/[close]/[pause]/[resume] run on the main thread. [requestCapture]
 * is called from the UI thread (sets an atomic flag). [computeFrameUpdate] runs on the GL thread,
 * which is the only mutator of [anchors] and [orderedAnchors]. The exposed [StateFlow]s are
 * updated from the GL thread and observed on the main thread.
 */
@Singleton
class ArTableSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var session: Session? = null
    private var tableAnchor: Anchor? = null

    // Corner anchors in capture order (max 4). GL-thread only.
    private val anchors = mutableListOf<Anchor>()
    // Anchors reordered TL, TR, BR, BL once all four are captured. Fixes the anchor->ideal-corner
    // assignment so the homography stays consistent regardless of later viewpoint.
    private var orderedAnchors: List<Anchor>? = null

    private val captureRequested = AtomicBoolean(false)

    // Ideal logical corner positions (TL, TR, BR, BL), set when a scan begins.
    @Volatile private var idealCorners: List<Pt> = emptyList()

    private val _capturedCount = MutableStateFlow(0)
    /** Number of corner anchors captured so far (0..4). Observed by the scan UI. */
    val capturedCount: StateFlow<Int> = _capturedCount.asStateFlow()

    /** True once ARCore world tracking is available on this device. */
    fun isArCoreAvailable(): Boolean = try {
        ArCoreApk.getInstance().checkAvailability(context).isSupported
    } catch (_: Exception) {
        false
    }

    /**
     * Capability now reflects ARCore *availability* (the Depth API is disabled). Downstream gates
     * that previously keyed off DEPTH_API now mean "ARCore world tracking is available".
     */
    val capability: DepthCapability
        get() = if (isArCoreAvailable()) DepthCapability.DEPTH_API else DepthCapability.NONE

    /**
     * Creates and configures the ARCore session (Depth disabled, horizontal plane finding).
     * Must be called on the main thread before the GL surface is created. Returns null if ARCore
     * is unavailable.
     */
    fun createSession(): Session? {
        if (!isArCoreAvailable()) return null
        return try {
            val s = Session(context)
            val config = Config(s).apply {
                depthMode = Config.DepthMode.DISABLED
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                lightEstimationMode = Config.LightEstimationMode.DISABLED
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }
            s.configure(config)
            session = s
            s
        } catch (_: Exception) {
            null
        }
    }

    fun getSession(): Session? = session

    /** Sets the ideal logical corner layout (TL, TR, BR, BL) for the table being scanned. */
    fun setIdealCorners(corners: List<Pt>) {
        idealCorners = corners
    }

    /** Queues a corner capture; the next GL frame performs the hit-test at screen centre. */
    fun requestCapture() {
        captureRequested.set(true)
    }

    /** Detaches all corner anchors and resets capture progress (rescan / cancel). */
    fun clearAnchors() {
        anchors.forEach { runCatching { it.detach() } }
        anchors.clear()
        orderedAnchors = null
        captureRequested.set(false)
        _capturedCount.value = 0
    }

    /**
     * Result of a hit-test attempt requested via [requestCapture]: whether it landed on a plane,
     * and the new captured-corner count.
     */
    data class CaptureResult(val hit: Boolean, val count: Int)

    /**
     * Per-frame update on the GL thread. Performs a queued capture (if any), then re-projects the
     * captured anchors to screen and, once four are tracked, fits the logical->screen homography.
     *
     * @return [FrameUpdate] with the (possibly null) table matrix, the live projected corner
     *   positions for the capture-feedback line, and an optional [CaptureResult] for this frame.
     */
    data class FrameUpdate(
        val matrix: Matrix?,
        val capturedCorners: List<PointF>,
        val capture: CaptureResult?,
    )

    fun computeFrameUpdate(frame: Frame, vpW: Int, vpH: Int): FrameUpdate {
        var capture: CaptureResult? = null
        if (captureRequested.getAndSet(false) && anchors.size < 4) {
            capture = performCapture(frame, vpW, vpH)
        }

        val view = FloatArray(16)
        val proj = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)

        val capturedScreens = anchors.mapNotNull { anchor ->
            anchorWorld(anchor)?.let { w ->
                TableFrameHomography.worldToScreen(view, proj, w, vpW, vpH)
                    ?.let { PointF(it.x, it.y) }
            }
        }

        val matrix = buildTableMatrix(view, proj, vpW, vpH)
        return FrameUpdate(matrix, capturedScreens, capture)
    }

    private fun performCapture(frame: Frame, vpW: Int, vpH: Int): CaptureResult {
        val hit = frame.hitTest(vpW / 2f, vpH / 2f)
            .firstOrNull { result ->
                val t = result.trackable
                t is Plane && t.isPoseInPolygon(result.hitPose) &&
                        t.type == Plane.Type.HORIZONTAL_UPWARD_FACING
            }
        if (hit == null) return CaptureResult(hit = false, count = anchors.size)

        return try {
            anchors.add(hit.createAnchor())
            if (anchors.size == 4) {
                orderedAnchors = orderCornersTlTrBrBl(frame, vpW, vpH)
            }
            _capturedCount.value = anchors.size
            CaptureResult(hit = true, count = anchors.size)
        } catch (_: Exception) {
            CaptureResult(hit = false, count = anchors.size)
        }
    }

    /**
     * Fixes the anchor -> ideal-corner assignment once, by projecting the four anchors to screen
     * and sorting them clockwise from top-left. Done a single time at capture completion so the
     * per-frame homography stays consistent even after the user walks to the far side of the table.
     */
    private fun orderCornersTlTrBrBl(frame: Frame, vpW: Int, vpH: Int): List<Anchor> {
        val view = FloatArray(16)
        val proj = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)

        val withScreen = anchors.mapNotNull { a ->
            anchorWorld(a)?.let { w ->
                TableFrameHomography.worldToScreen(view, proj, w, vpW, vpH)?.let { s -> a to s }
            }
        }
        if (withScreen.size != 4) return anchors.toList()

        val cx = withScreen.sumOf { it.second.x.toDouble() }.toFloat() / 4f
        val cy = withScreen.sumOf { it.second.y.toDouble() }.toFloat() / 4f
        val top = withScreen.filter { it.second.y < cy }.sortedBy { it.second.x }
        val bottom = withScreen.filter { it.second.y >= cy }.sortedByDescending { it.second.x }
        // top: left, right ; bottom: right, left  ->  TL, TR, BR, BL
        return (top + bottom).map { it.first }.take(4)
    }

    private fun buildTableMatrix(view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int): Matrix? {
        val ordered = orderedAnchors ?: return null
        if (ordered.size != 4 || idealCorners.size != 4) return null
        if (ordered.any { it.trackingState != TrackingState.TRACKING }) return null
        val cornersWorld = ordered.map { anchorWorld(it) ?: return null }
        val h = TableFrameHomography.computeLogicalToScreen(
            view, proj, cornersWorld, idealCorners, vpW, vpH
        ) ?: return null
        return Matrix().apply { setValues(h) }
    }

    private fun anchorWorld(anchor: Anchor): Vec3? {
        if (anchor.trackingState != TrackingState.TRACKING) return null
        val p = anchor.pose
        return Vec3(p.tx(), p.ty(), p.tz())
    }

    // --- Plane-anchor pitch fallback (used before 4 corners are captured) ---

    /**
     * Searches updated trackables for a horizontal plane matching pool-table dimensions and anchors
     * to it. Used to warm tracking and provide a pitch estimate before the corners are captured.
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
    fun close() {
        clearAnchors()
        clearPlaneAnchor()
        session?.close()
        session = null
    }
}
