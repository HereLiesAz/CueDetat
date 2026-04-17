package com.hereliesaz.cuedetat.data

import com.google.ar.core.Frame
import com.hereliesaz.cuedetat.domain.CueDetatState
import java.util.concurrent.atomic.AtomicInteger
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
    // Process every 2nd AR frame — halves CV load on the GL thread with no visible quality loss.
    private val frameCounter = AtomicInteger(0)

    fun updateUiState(state: CueDetatState) {
        stateRef.set(state)
    }

    /**
     * Called from the GL thread for every ARCore frame.
     * Acquires the camera CPU image, runs the CV pipeline, and closes the image.
     */
    fun processFrame(frame: Frame) {
        val state = stateRef.get() ?: return
        // Skip odd frames — AR tracking is still updated by ARCore internally at full rate,
        // only our CV pipeline (ball detection) runs at half rate.
        if (frameCounter.getAndIncrement() % 2 != 0) return
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
