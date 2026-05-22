// app/src/main/java/com/hereliesaz/cuedetat/data/ArFrameProcessor.kt
package com.hereliesaz.cuedetat.data

import com.google.ar.core.Frame
import com.hereliesaz.cuedetat.domain.CueDetatState
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges ARCore frames to [VisionRepository].
 *
 * The GL thread delivers [Frame] objects. This processor acquires the CPU image,
 * feeds it to the vision pipeline (ball detection, AR tracking), and closes it promptly.
 *
 * [updateUiState] is called from the main thread; [processFrame] from the GL thread.
 * Using [AtomicReference] ensures a consistent state snapshot without blocking either thread.
 */
@Singleton
class ArFrameProcessor @Inject constructor(
    private val visionRepository: VisionRepository,
) {
    private val stateRef = AtomicReference<CueDetatState?>(null)

    // Time-based throttling. The physical world moves, but the balls don't move that fast.
    private val lastProcessTime = AtomicLong(0L)

    fun updateUiState(state: CueDetatState) {
        stateRef.set(state)
    }

    /**
     * Called from the GL thread for every ARCore frame.
     * Acquires the camera CPU image, runs the CV pipeline, and closes the image.
     */
    fun processFrame(frame: Frame) {
        val state = stateRef.get() ?: return

        val now = System.currentTimeMillis()
        // Throttle to ~10 FPS (100ms interval) to stop the CPU from melting.
        if (now - lastProcessTime.get() < 100L) return
        lastProcessTime.set(now)

        try {
            val cpuImage = frame.acquireCameraImage()
            // ARCore's CPU image sensor orientation matches the display orientation configured
            // via session.setDisplayGeometry(); for portrait-primary Android apps this is 90°.
            val rotation = 90
            visionRepository.processArCpuImage(cpuImage, rotation, state)
            cpuImage.close()
        } catch (_: Exception) {
            // acquireCameraImage can fail if a previous image is still open; skip frame.
        }
    }
}