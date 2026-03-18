package com.hereliesaz.cuedetat.data

import android.content.Context
import android.media.Image
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.NotYetAvailableException
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.DepthPlane
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
class ArDepthSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var session: Session? = null
    var isDepthSupported: Boolean = false
        private set

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
                planeFindingMode = Config.PlaneFindingMode.DISABLED
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

    fun pause()  { session?.pause() }
    fun resume() { try { session?.resume() } catch (_: Exception) {} }
    fun close()  { session?.close(); session = null }
}
