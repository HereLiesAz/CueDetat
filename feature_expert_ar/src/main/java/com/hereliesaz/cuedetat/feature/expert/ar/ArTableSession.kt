package com.hereliesaz.cuedetat.feature.expert.ar

import android.content.Context
import android.graphics.Matrix
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
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/** Height and derived pitch for the camera's position above the detected table plane. */
data class CameraAbovePlane(val pitchDegrees: Float, val heightM: Float)

/**
 * Owns the ARCore [Session] and the world anchors that define the table in expert mode.
 *
 * The table is defined by four world [Anchor]s at its corner pockets (TL, TR, BR, BL). ARCore's
 * visual-inertial tracking keeps them fixed as the user walks around, and every frame
 * [computeFrameUpdate] fits the logical->screen homography the 2D Canvas renderer applies.
 *
 * Pocket tapping (hit-testing the screen centre to drop each anchor) has been removed. Nothing
 * supplies [tableAnchors] yet: the matrix stays null until the table-lock step (virtual table
 * placed over the real one, then locked) provides them.
 *
 * The ARCore Depth API is intentionally disabled — corner anchors only need plane-finding and
 * hit-testing, and the depth stream was a per-frame battery cost feeding only a pitch fallback.
 *
 * Threading: [createSession]/[close]/[pause]/[resume] run on the main thread.
 * [computeFrameUpdate] runs on the GL thread.
 */
/** Manually constructed by ArControllerImpl (lives in the on-demand module; no Hilt here). */
class ArTableSession(
    private val context: Context,
) {
    private var session: Session? = null
    private var tableAnchor: Anchor? = null

    // Corner anchors ordered TL, TR, BR, BL. Null until the table is locked.
    @Volatile private var tableAnchors: List<Anchor>? = null

    // Ideal logical corner positions (TL, TR, BR, BL) matching [tableAnchors].
    @Volatile private var idealCorners: List<Pt> = emptyList()

    // Height of the table plane above the corner anchors, in metres, driven by the tableZOffset
    // slider.
    @Volatile private var tableHeightMeters: Float = 0f

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

    /** Sets the table-plane lift above the corner anchors (tableZOffset slider, in metres). */
    fun setTableHeightMeters(meters: Float) {
        tableHeightMeters = meters
    }

    /** Detaches the corner anchors (rescan / cancel). */
    fun clearAnchors() {
        tableAnchors?.forEach { runCatching { it.detach() } }
        tableAnchors = null
        idealCorners = emptyList()
    }

    /**
     * Per-frame update on the GL thread: fits the logical->screen homography from the corner
     * anchors, or returns null when the table is not locked or tracking is lost.
     */
    fun computeFrameUpdate(frame: Frame, vpW: Int, vpH: Int): Matrix? {
        val view = FloatArray(16)
        val proj = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)
        return buildTableMatrix(view, proj, vpW, vpH)
    }

    private fun buildTableMatrix(view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int): Matrix? {
        val ordered = tableAnchors ?: return null
        if (ordered.size != 4 || idealCorners.size != 4) return null
        if (ordered.any { it.trackingState != TrackingState.TRACKING }) return null
        val cornersWorld = ordered.map { anchorWorld(it) ?: return null }
        val h = TableFrameHomography.computeLogicalToScreen(
            view, proj, cornersWorld, idealCorners, vpW, vpH, heightMeters = tableHeightMeters
        ) ?: return null
        return Matrix().apply { setValues(h) }
    }

    private fun anchorWorld(anchor: Anchor): Vec3? {
        if (anchor.trackingState != TrackingState.TRACKING) return null
        val p = anchor.pose
        return Vec3(p.tx(), p.ty(), p.tz())
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
    fun close() {
        clearAnchors()
        clearPlaneAnchor()
        session?.close()
        session = null
    }
}
