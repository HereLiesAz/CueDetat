package com.hereliesaz.cuedetat.feature.expert.ar

import android.graphics.Matrix
import android.media.Image
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.hereliesaz.cuedetat.data.VisionRepository
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridges ARCore frames to [VisionRepository] and samples the felt colour from the AR camera image.
 *
 * The GL thread delivers [Frame] objects. This processor acquires the CPU image, feeds it to the
 * vision pipeline (ball detection), samples the centre felt colour, and closes it promptly.
 *
 * Because ARCore owns the camera for the whole AR flow, felt-colour capture (which previously came
 * from the CameraX [TableScanAnalyzer]) is sourced here instead, exposed via [latestFeltHsv].
 *
 * [updateUiState] is called from the main thread; [processFrame] from the GL thread. The
 * [AtomicReference]/[StateFlow] hand-offs keep a consistent snapshot without blocking either thread.
 */
/** Manually constructed by ArControllerImpl (lives in the on-demand module; no Hilt here). */
class ArFrameProcessor(
    private val visionRepository: VisionRepository,
) {
    private val stateRef = AtomicReference<CueDetatState?>(null)

    // Time-based throttling. The physical world moves, but the balls don't move that fast.
    private val lastProcessTime = AtomicLong(0L)

    private val _latestFeltHsv = MutableStateFlow<FloatArray?>(null)
    /** Rolling mean HSV (H 0-360, S/V 0-1) of the centre crop of the AR camera image. */
    val latestFeltHsv: StateFlow<FloatArray?> = _latestFeltHsv.asStateFlow()

    fun updateUiState(state: CueDetatState) {
        stateRef.set(state)
    }

    /**
     * Called from the GL thread for every ARCore frame.
     * Acquires the camera CPU image, samples felt colour, runs the CV pipeline, and closes it.
     */
    fun processFrame(frame: Frame) {
        val state = stateRef.get() ?: return

        val now = System.currentTimeMillis()
        // Throttle to ~10 FPS (100ms interval) to stop the CPU from melting.
        if (now - lastProcessTime.get() < 100L) return
        lastProcessTime.set(now)

        val cpuImage = try {
            frame.acquireCameraImage()
        } catch (_: Exception) {
            // No image this frame (not yet available, or ARCore's small pool is exhausted).
            return
        }
        // ARCore lends a handful of CPU images at a time. One that is never closed is gone for
        // the session, and once the pool drains acquireCameraImage fails on every frame, which
        // silently ends all AR detection. So close it whatever happens below.
        try {
            sampleCenterHsv(cpuImage)?.let { _latestFeltHsv.value = it }
            // ARCore's CPU image sensor orientation matches the display orientation configured
            // via session.setDisplayGeometry(); for portrait-primary Android apps this is 90°.
            val rotation = 90
            visionRepository.processArCpuImage(
                cpuImage, rotation, state, imageToView(frame, cpuImage.width, cpuImage.height)
            )
        } catch (_: Exception) {
            // A bad frame is skipped; the next one gets a fresh image.
        } finally {
            cpuImage.close()
        }
    }

    /**
     * CPU-image pixels -> view pixels, straight from ARCore. Its CPU image and its on-screen
     * camera feed are cropped differently, so a guessed scale puts detections in the wrong place.
     * Rotation, scale and crop are affine, so three corners fix the whole mapping.
     */
    private fun imageToView(frame: Frame, width: Int, height: Int): Matrix? {
        val src = floatArrayOf(0f, 0f, width.toFloat(), 0f, 0f, height.toFloat())
        val dst = FloatArray(6)
        return try {
            frame.transformCoordinates2d(Coordinates2d.IMAGE_PIXELS, src, Coordinates2d.VIEW, dst)
            Matrix().takeIf { it.setPolyToPoly(src, 0, dst, 0, 3) }
        } catch (_: Exception) {
            null
        }
    }

    /** Mean HSV of the centre 10% of the image (the felt-capture reticle). */
    private fun sampleCenterHsv(image: Image): FloatArray? = sampleYuvHsv(
        image,
        (image.width * 0.45f).toInt(), (image.height * 0.45f).toInt(),
        (image.width * 0.55f).toInt(), (image.height * 0.55f).toInt(),
    )
}
