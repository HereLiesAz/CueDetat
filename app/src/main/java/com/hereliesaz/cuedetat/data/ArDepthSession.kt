package com.hereliesaz.cuedetat.data

import android.content.Context
import android.media.Image
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.DepthPlane
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages the lifecycle of an ARCore [Session] and extracts table-plane depth information
 * from ARCore's Depth API on each frame.
 *
 * Depth source is fully abstracted by ARCore: the device may use ToF hardware, stereo cameras,
 * or structure-from-motion — all exposed through the same [Frame.acquireDepthImage16Bits] API.
 *
 * Thread safety: [createSession] and [close] are called from the main thread; [processFrame]
 * is called from the GL thread. [session] is only mutated from those two call sites, which
 * are sequential in practice (create → GL loop → close).
 */
/** Height and derived pitch for the camera's position above the detected table plane. */
data class CameraAbovePlane(val pitchDegrees: Float, val heightM: Float)

@Singleton
class ArDepthSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var session: Session? = null
    private var tableAnchor: Anchor? = null
    var isDepthSupported: Boolean = false
        private set

    private companion object {
        // Pool table minimum playing surface: ≈ 1.0m × 2.0m (7ft table)
        // ARCore extentX/extentZ are half-extents, so full area = (2*extentX)*(2*extentZ)
        const val MIN_TABLE_AREA_M2 = 0.8f
    }

    val capability: DepthCapability
        get() = if (isDepthSupported) DepthCapability.DEPTH_API else DepthCapability.NONE

    /** True if ARCore itself is available on this device. */
    fun isArCoreAvailable(): Boolean = try {
        ArCoreApk.getInstance().checkAvailability(context).isSupported
    } catch (_: Exception) {
        false
    }

    /**
     * Creates and configures the ARCore session.
     * Must be called from the main thread before the GL surface is created.
     * Returns null if ARCore is unavailable.
     */
    fun createSession(): Session? {
        if (!isArCoreAvailable()) return null
        return try {
            val s = Session(context)
            isDepthSupported = s.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            val config = Config(s).apply {
                depthMode = if (isDepthSupported) Config.DepthMode.AUTOMATIC
                            else Config.DepthMode.DISABLED
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                lightEstimationMode = Config.LightEstimationMode.DISABLED
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }
            s.configure(config)
            session = s
            s
        } catch (e: Exception) {
            null
        }
    }

    fun getSession(): Session? = session

    /**
     * Extracts a [DepthPlane] from the current ARCore frame.
     * Called from the GL thread. Returns null when depth is unavailable or not yet ready.
     */
    fun processFrame(frame: Frame): DepthPlane? {
        if (!isDepthSupported) return null
        return try {
            frame.acquireDepthImage16Bits().use { depthImage ->
                extractPlane(depthImage)
            }
        } catch (_: NotYetAvailableException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Estimates the table surface distance and confidence from the depth image.
     *
     * Strategy: sample the central 40% of the image (likely the table), take the median
     * depth, and use the IQR to estimate flatness (tight IQR = flat surface = high confidence).
     */
    private fun extractPlane(depthImage: Image): DepthPlane? {
        val width = depthImage.width
        val height = depthImage.height
        val plane = depthImage.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val startX = (width * 0.30f).toInt()
        val endX   = (width * 0.70f).toInt()
        val startY = (height * 0.30f).toInt()
        val endY   = (height * 0.70f).toInt()

        val samples = ArrayList<Int>(512)
        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val idx = y * rowStride + x * pixelStride
                if (idx + 1 < buffer.limit()) {
                    val lo = buffer[idx].toInt() and 0xFF
                    val hi = buffer[idx + 1].toInt() and 0xFF
                    val mm = (hi shl 8) or lo
                    if (mm in 100..5000) samples.add(mm)  // 10 cm … 5 m
                }
                x += 4
            }
            y += 4
        }

        if (samples.size < 10) return null
        samples.sort()

        val median = samples[samples.size / 2]
        val q1     = samples[samples.size / 4]
        val q3     = samples[samples.size * 3 / 4]
        val iqrMm  = q3 - q1

        // IQR < 150 mm on a 40% region of a pool table → very flat, high confidence
        val confidence = (1f - iqrMm / 300f).coerceIn(0f, 1f)
        if (confidence < 0.2f) return null

        return DepthPlane(
            distanceMeters = median / 1000f,
            confidence = confidence,
            capability = DepthCapability.DEPTH_API,
        )
    }

    /**
     * Searches updated trackables for a horizontal plane that matches pool-table dimensions
     * and anchors to it. Skips if an anchor is already established.
     *
     * Called every AR frame from the GL thread. [expectedDepthM] is used to prefer the plane
     * closest to the known table distance; pass 0f to accept any qualifying plane.
     */
    fun findAndAnchorTablePlane(frame: Frame, expectedDepthM: Float) {
        if (tableAnchor != null) return

        val cameraPose = frame.camera.pose
        val candidates = frame.getUpdatedTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
            .filter { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            // extentX/Z are half-extents; full area = (2*extentX)*(2*extentZ)
            .filter { 4f * it.extentX * it.extentZ >= MIN_TABLE_AREA_M2 }

        val best = if (expectedDepthM > 0f) {
            candidates.minByOrNull { plane ->
                val p = plane.centerPose
                val dx = cameraPose.tx() - p.tx()
                val dy = cameraPose.ty() - p.ty()
                val dz = cameraPose.tz() - p.tz()
                abs(sqrt(dx * dx + dy * dy + dz * dz) - expectedDepthM)
            }
        } else {
            candidates.firstOrNull()
        }

        best?.let { plane ->
            try {
                tableAnchor = plane.createAnchor(plane.centerPose)
            } catch (_: Exception) { /* retry next frame */ }
        }
    }

    /**
     * Computes the camera's geometric position above the anchored table plane.
     * Returns null if no anchor is established or tracking is lost.
     *
     * The resulting [CameraAbovePlane.pitchDegrees] is the actual viewing elevation angle
     * (atan2 of camera height above table ÷ horizontal distance from anchor center),
     * reflecting real-world geometry rather than raw device tilt.
     */
    fun computeCameraAbovePlane(frame: Frame): CameraAbovePlane? {
        val anchor = tableAnchor ?: return null
        if (anchor.trackingState != TrackingState.TRACKING) return null

        val cameraInAnchor = anchor.pose.inverse().compose(frame.camera.pose)
        val heightM = cameraInAnchor.ty()
        if (heightM <= 0f) return null  // camera below table plane — bad data

        val horizontal = sqrt(cameraInAnchor.tx().pow(2) + cameraInAnchor.tz().pow(2))
        if (horizontal < 0.01f) return null  // directly overhead — degenerate geometry

        val pitchDeg = Math.toDegrees(atan2(heightM.toDouble(), horizontal.toDouble()))
            .toFloat().coerceIn(5f, 85f)
        return CameraAbovePlane(pitchDegrees = pitchDeg, heightM = heightM)
    }

    /** Detaches the table plane anchor. Called when tracking is lost or the session closes. */
    fun clearAnchor() {
        tableAnchor?.detach()
        tableAnchor = null
    }

    fun pause()  { session?.pause() }
    fun resume() { try { session?.resume() } catch (_: Exception) {} }
    fun close()  { clearAnchor(); session?.close(); session = null }
}
