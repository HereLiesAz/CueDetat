package com.hereliesaz.cuedetat.feature.expert.ar

import android.content.Context
import android.graphics.Matrix
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.domain.FeltColorMatch
import com.hereliesaz.cuedetat.domain.TableFrameHomography
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.hypot
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
        return try {
            val s = Session(context)
            supportsDepth = s.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            val config = Config(s).apply {
                depthMode = if (supportsDepth) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
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



    /** Update editable table controls without ever re-anchoring on camera-pose updates. */
    fun updatePreviewInput(state: CueDetatState) {
        if (state.cameraMode != CameraMode.AR_ACTIVE || !state.table.isVisible) {
            previewInput.set(null)
            return
        }
        val (minZoom, maxZoom) = ZoomMapping.getZoomRange(
            state.experienceMode, state.isBeginnerViewLocked)
        previewInput.set(PreviewInput(
            corners = state.table.corners.map { Pt(it.x, it.y) },
            panX = state.viewOffset.x,
            panY = state.viewOffset.y,
            rotationDegrees = state.userRotationDegrees,
            zoom = ZoomMapping.sliderToZoom(state.zoomSliderPosition, minZoom, maxZoom),
        ))
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
        unlockRequested.set(true)
    }

    /** Detaches the corner anchors (rescan / cancel). */
    fun clearAnchors() {
        tableAnchors?.forEach { runCatching { it.detach() } }
        tableAnchors = null
        idealCorners = emptyList()
    }

    /**
     * [matrix]: logical->screen homography from the locked table or metric preview.
     * [lockResult]: non-null only on the frame that served a [requestLock] (true = locked).
     */
    data class FrameUpdate(val matrix: Matrix?, val lockResult: Boolean?)

    /**
     * Per-frame update on the GL thread: serves any queued lock/unlock, then fits the
     * logical->screen homography from the corner anchors.
     */
    fun computeFrameUpdate(frame: Frame, vpW: Int, vpH: Int): FrameUpdate {
        if (unlockRequested.getAndSet(false)) {
            adoptLockedPoseAsPreview()
            clearAnchors()
        }

        val view = FloatArray(16)
        val proj = FloatArray(16)
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)

        if (tableAnchors == null) updatePreview(frame, view, proj, vpW, vpH)
        val lockResult = pendingLock.getAndSet(null)?.let { performLock(frame, it, view, proj, vpW, vpH) }
        return FrameUpdate(buildTableMatrix(view, proj, vpW, vpH), lockResult)
    }

    /**
     * Create one stationary world-space anchor. Depth/plane hits supply real metres; without
     * either the 1.8m forward / 0.45m below-camera starting point is explicitly an estimate.
     * The app never claims that estimate is a recognized table.
     */
    private fun initializePreview(frame: Frame, input: PreviewInput, vpW: Int, vpH: Int) {
        val s = session ?: return
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
        }
    }

    /**
     * Integrate *only explicit control changes* against the current world pose. In particular,
     * zoom changes camera-to-table range with the pinhole 1/d law; it cannot scale the table
     * itself or deform its perspective as the camera pans.
     */
    private fun updatePreview(frame: Frame, view: FloatArray, proj: FloatArray, vpW: Int, vpH: Int) {
        val input = previewInput.get()
        if (input == null) {
            clearPreview()
            return
        }
        if (previewAnchor == null) initializePreview(frame, input, vpW, vpH)
        val anchor = previewAnchor ?: return
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
            val cam = frame.camera.pose
            val relX = pose.tx() + previewOffset.x - cam.tx()
            val relZ = pose.tz() + previewOffset.z - cam.tz()
            val currentRange = hypot(relX.toDouble(), relZ.toDouble()).toFloat()
            if (currentRange > 0.05f) {
                val desiredRange = (currentRange * previous.zoom / input.zoom)
                    .coerceIn(0.65f, 8f)
                previewOffset = Vec3(
                    previewOffset.x + relX * (desiredRange / currentRange - 1f),
                    previewOffset.y,
                    previewOffset.z + relZ * (desiredRange / currentRange - 1f),
                )
            }
        }
        previousPreviewInput = input
    }

    /** Unlock adopts the last locked corners; there is no screen-space teleport. */
    private fun adoptLockedPoseAsPreview() {
        val corners = tableAnchors?.map { anchorWorld(it) ?: return } ?: return
        val input = previewInput.get() ?: return
        if (corners.size != 4) return
        val center = Vec3(corners.sumOf { it.x.toDouble() }.toFloat() / 4f,
            corners.sumOf { it.y.toDouble() }.toFloat() / 4f,
            corners.sumOf { it.z.toDouble() }.toFloat() / 4f)
        val rx = corners[1].x - corners[0].x
        val rz = corners[1].z - corners[0].z
        val ax = corners[0].x - corners[3].x
        val az = corners[0].z - corners[3].z
        val r = hypot(rx.toDouble(), rz.toDouble()).toFloat()
        val a = hypot(ax.toDouble(), az.toDouble()).toFloat()
        if (r < 1e-4f || a < 1e-4f) return
        val anchor = try {
            session?.createAnchor(Pose(floatArrayOf(center.x, center.y, center.z),
                floatArrayOf(0f, 0f, 0f, 1f)))
        } catch (_: Exception) { null } ?: return
        clearPreview()
        previewAnchor = anchor
        previewRight = Vec3(rx / r, 0f, rz / r)
        previewAway = Vec3(ax / a, 0f, az / a)
        previewInitialRotation = input.rotationDegrees
        previousPreviewInput = input
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
        // The surface vote determines a plane, not a new physical table size.
        // The projected corners determine only centre and heading. Rebuild the four
        // positions from the selected table's exact metric dimensions so Lock cannot
        // silently stretch the playing bed when the fitted plane depth differs.
        val center = Vec3(
            worlds.sumOf { it.x.toDouble() }.toFloat() / 4f,
            worlds.sumOf { it.y.toDouble() }.toFloat() / 4f,
            worlds.sumOf { it.z.toDouble() }.toFloat() / 4f,
        )
        val dx = worlds[1].x - worlds[0].x
        val dz = worlds[1].z - worlds[0].z
        val width = hypot(dx.toDouble(), dz.toDouble()).toFloat()
        if (width < 0.05f) return false
        val right = Vec3(dx / width, 0f, dz / width)
        var away = Vec3(right.z, 0f, -right.x)
        // Keep the virtual TL→BL direction; a 180° table-end flip is not a lock.
        val alongX = worlds[0].x - worlds[3].x
        val alongZ = worlds[0].z - worlds[3].z
        if (away.x * alongX + away.z * alongZ < 0f) {
            away = Vec3(-away.x, 0f, -away.z)
        }
        val metricCorners = TableFrameHomography.metricRectangleCorners(
            req.logicalCorners, center, right, away, 0f,
        )
        val anchors = try {
            metricCorners.map { w ->
                s.createAnchor(Pose(floatArrayOf(w.x, w.y, w.z),
                    planePose.rotationQuaternion))
            }
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
        val locked = tableAnchors
        val cornersWorld: List<Vec3>
        val logical: List<Pt>
        if (locked != null) {
            if (locked.size != 4 || idealCorners.size != 4) return null
            cornersWorld = locked.map { anchorWorld(it) ?: return null }
            logical = idealCorners
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
                input.rotationDegrees - previewInitialRotation)
        }
        val h = TableFrameHomography.computeLogicalToScreen(
            view, proj, cornersWorld, logical, vpW, vpH, heightMeters = tableHeightMeters
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
        clearPreview()
        clearPlaneAnchor()
        previewInput.set(null)
        session?.close()
        session = null
        supportsDepth = false
    }
}
