package com.hereliesaz.cuedetat.feature.expert.ar

import android.content.Context
import android.graphics.Matrix
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.FeltColorMatch
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/** Felt-vote sampling: a FELT_GRID x FELT_GRID grid across the virtual table. */
private const val FELT_GRID = 5
/** Grid inset from the virtual table's edges, as a fraction, to stay off the cushions. */
private const val FELT_INSET = 0.1f
/** Half-size of the image patch averaged at each grid point, in image pixels. */
private const val FELT_PATCH_PX = 4

/** Height and derived pitch for the camera's position above the detected table plane. */
data class CameraAbovePlane(val pitchDegrees: Float, val heightM: Float)

/**
 * Owns the ARCore [Session] and the world anchors that define the table in expert mode.
 *
 * The table is defined by four world [Anchor]s at its corner pockets (TL, TR, BR, BL). ARCore's
 * visual-inertial tracking keeps them fixed as the user walks around, and every frame
 * [computeFrameUpdate] fits the logical->screen homography the 2D Canvas renderer applies.
 *
 * The anchors come from the Lock button: the user lines the virtual table up over the real one,
 * taps Lock, and [requestLock] hands over the on-screen positions of the virtual table's four
 * corners. On the next GL frame each is cast onto the detected horizontal plane and anchored
 * there. From then on ARCore holds the table in place; [unlock] lets go.
 *
 * The ARCore Depth API is intentionally disabled — corner anchors only need plane-finding and
 * hit-testing, and the depth stream was a per-frame battery cost feeding only a pitch fallback.
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

    // Corner anchors ordered TL, TR, BR, BL. Null until the table is locked.
    @Volatile private var tableAnchors: List<Anchor>? = null

    /** Screen positions of the virtual table's corners plus their logical twins, awaiting a GL frame. */
    private class LockRequest(
        val screenCorners: List<Pt>,
        val logicalCorners: List<Pt>,
        val feltHsv: FloatArray?,
    )
    private val pendingLock = AtomicReference<LockRequest?>(null)
    private val unlockRequested = AtomicBoolean(false)

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

    /** Queues release of the locked table; the overlay returns to the sensor-driven pose. */
    fun unlock() {
        pendingLock.set(null)
        unlockRequested.set(true)
    }

    /** Detaches the corner anchors (rescan / cancel). */
    fun clearAnchors() {
        tableAnchors?.forEach { runCatching { it.detach() } }
        tableAnchors = null
        idealCorners = emptyList()
    }

    /**
     * [matrix]: logical->screen homography, null while unlocked or tracking is lost.
     * [lockResult]: non-null only on the frame that served a [requestLock] (true = locked).
     */
    data class FrameUpdate(val matrix: Matrix?, val lockResult: Boolean?)

    /**
     * Per-frame update on the GL thread: serves any queued lock/unlock, then fits the
     * logical->screen homography from the corner anchors.
     */
    fun computeFrameUpdate(frame: Frame, vpW: Int, vpH: Int): FrameUpdate {
        if (unlockRequested.getAndSet(false)) clearAnchors()

        val view = FloatArray(16)
        val proj = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)

        val lockResult = pendingLock.getAndSet(null)?.let { performLock(frame, it, view, proj, vpW, vpH) }
        return FrameUpdate(buildTableMatrix(view, proj, vpW, vpH), lockResult)
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
        val anchors = try {
            worlds.map { w -> s.createAnchor(Pose(floatArrayOf(w.x, w.y, w.z), planePose.rotationQuaternion)) }
        } catch (_: Exception) {
            return false
        }
        clearAnchors()
        idealCorners = req.logicalCorners
        tableAnchors = anchors
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
