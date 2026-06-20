package com.hereliesaz.cuedetat.feature.expert.ar

import android.graphics.Color
import android.media.Image
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

        try {
            val cpuImage = frame.acquireCameraImage()
            sampleCenterHsv(cpuImage)?.let { _latestFeltHsv.value = it }
            // ARCore's CPU image sensor orientation matches the display orientation configured
            // via session.setDisplayGeometry(); for portrait-primary Android apps this is 90°.
            val rotation = 90
            visionRepository.processArCpuImage(cpuImage, rotation, state)
            cpuImage.close()
        } catch (_: Exception) {
            // acquireCameraImage can fail if a previous image is still open; skip frame.
        }
    }

    /**
     * Averages the YUV_420_888 centre crop and converts to HSV. Pure read of the image planes
     * (BT.601 full-range YUV->RGB, then Android RGBToHSV), so no OpenCV dependency.
     */
    private fun sampleCenterHsv(image: Image): FloatArray? {
        if (image.format != android.graphics.ImageFormat.YUV_420_888) return null
        val w = image.width
        val h = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        var rSum = 0.0; var gSum = 0.0; var bSum = 0.0; var count = 0

        val x0 = (w * 0.45f).toInt(); val x1 = (w * 0.55f).toInt()
        val y0 = (h * 0.45f).toInt(); val y1 = (h * 0.55f).toInt()
        val step = ((x1 - x0) / 16).coerceAtLeast(1)

        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val yIdx = y * yPlane.rowStride + x * yPlane.pixelStride
                val uvX = x / 2; val uvY = y / 2
                val uIdx = uvY * uPlane.rowStride + uvX * uPlane.pixelStride
                val vIdx = uvY * vPlane.rowStride + uvX * vPlane.pixelStride
                if (yIdx < yBuf.limit() && uIdx < uBuf.limit() && vIdx < vBuf.limit()) {
                    val yy = (yBuf.get(yIdx).toInt() and 0xFF).toDouble()
                    val uu = (uBuf.get(uIdx).toInt() and 0xFF) - 128.0
                    val vv = (vBuf.get(vIdx).toInt() and 0xFF) - 128.0
                    rSum += (yy + 1.402 * vv).coerceIn(0.0, 255.0)
                    gSum += (yy - 0.344136 * uu - 0.714136 * vv).coerceIn(0.0, 255.0)
                    bSum += (yy + 1.772 * uu).coerceIn(0.0, 255.0)
                    count++
                }
                x += step
            }
            y += step
        }
        if (count == 0) return null
        val hsv = FloatArray(3)
        Color.RGBToHSV((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt(), hsv)
        return hsv
    }
}
