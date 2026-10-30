package com.hereliesaz.cuedetat.feature.expert.ar

import android.graphics.Matrix
import com.hereliesaz.cuedetat.utils.toMat
import org.opencv.core.Mat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.media.Image
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.hereliesaz.cuedetat.data.CaptureCamera
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
 * The GL thread delivers [Frame] objects. This processor acquires the CPU image, samples the
 * centre felt colour, copies the image into a Mat and closes it, all on the GL thread (a few ms).
 * Ball detection then runs on [worker], so the camera feed never stalls behind the CV pass.
 *
 * Because ARCore owns the camera for the whole AR flow, felt-colour capture (which previously came
 * from the CameraX [TableScanAnalyzer]) is sourced here instead, exposed via [latestFeltHsv].
 *
 * [updateUiState] is called from the main thread; [processFrame] from the GL thread; detection
 * on [worker]. The [AtomicReference]/[StateFlow] hand-offs keep a consistent snapshot without
 * blocking any of them.
 */
/** Manually constructed by ArControllerImpl (lives in the on-demand module; no Hilt here). */
class ArFrameProcessor(
    private val visionRepository: VisionRepository,
) {
    private val stateRef = AtomicReference<CueDetatState?>(null)

    // Time-based throttling. The physical world moves, but the balls don't move that fast.
    private val lastProcessTime = AtomicLong(0L)

    /** One background thread for detection; frames arriving while it is busy are skipped. */
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "ar-ball-detection") }
    private val workerBusy = AtomicBoolean(false)

    private val _latestFeltHsv = MutableStateFlow<FloatArray?>(null)
    /** Mean HSV (H 0-360, S/V 0-1) of the centre crop of the latest sampled AR camera image. */
    val latestFeltHsv: StateFlow<FloatArray?> = _latestFeltHsv.asStateFlow()

    fun updateUiState(state: CueDetatState) {
        stateRef.set(state)
    }

    /**
     * Called from the GL thread for every ARCore frame. Samples felt colour and hands a copy of
     * the camera image to [worker] for ball detection; the ARCore image itself is closed here.
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
        // Claim the worker before copying: a busy worker means this frame is skipped for
        // detection (felt sampling still runs).
        val claimed = workerBusy.compareAndSet(false, true)
        var copy: Mat? = null
        var mapping: Matrix? = null
        var camera: CaptureCamera? = null
        try {
            sampleCenterHsv(cpuImage)?.let { _latestFeltHsv.value = it }
            if (claimed) {
                copy = cpuImage.toMat(Mat())
                mapping = imageToView(frame, cpuImage.width, cpuImage.height)
                camera = cameraOf(frame, cpuImage.width, cpuImage.height)
            }
        } catch (_: Exception) {
            // A bad frame is skipped; the next one gets a fresh image.
            copy?.release()
            copy = null
        } finally {
            cpuImage.close()
        }

        val mat = copy ?: run {
            // Claimed but failed to copy: give the worker back.
            if (claimed) workerBusy.set(false)
            return
        }
        val frameToView = mapping
        val frameCamera = camera
        val uprightRotation = uprightRotationDegrees(frameToView)
        try {
            worker.execute {
                try {
                    visionRepository.processArFrame(mat, uprightRotation, state, frameToView, frameCamera)
                } catch (_: Exception) {
                    // Skip the frame.
                } finally {
                    mat.release()
                    workerBusy.set(false)
                }
            }
        } catch (_: Exception) {
            // Executor shut down or rejected: nothing will release the copy but us.
            mat.release()
            workerBusy.set(false)
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

    /**
     * Clockwise degrees (0/90/180/270) that turn the CPU image upright on screen, read off ARCore's
     * own image -> view mapping: where the image's +x axis lands in the view. It was a fixed 90, true
     * only for a portrait-held phone; in landscape the ball boxes, the zoom-to-ball ratio and the
     * rotated training JPEGs were all turned a quarter. With no mapping, 90 (portrait-primary).
     */
    private fun uprightRotationDegrees(mapping: Matrix?): Int {
        if (mapping == null) return 90
        val xAxis = floatArrayOf(1f, 0f)
        mapping.mapVectors(xAxis)
        val degrees = Math.toDegrees(kotlin.math.atan2(xAxis[1].toDouble(), xAxis[0].toDouble()))
        return ((Math.round(degrees / 90.0).toInt() * 90) % 360 + 360) % 360
    }

    /**
     * Intrinsics of the CPU image and the camera's world pose, for training capture. ARCore gives
     * intrinsics for its own image size; they are scaled to the CPU image's.
     */
    private fun cameraOf(frame: Frame, width: Int, height: Int): CaptureCamera? = try {
        val cam = frame.camera
        val k = cam.imageIntrinsics
        val dims = k.imageDimensions
        val sx = if (dims[0] > 0) width.toFloat() / dims[0] else 1f
        val sy = if (dims[1] > 0) height.toFloat() / dims[1] else 1f
        val f = k.focalLength
        val c = k.principalPoint
        val pose = FloatArray(7)
        cam.pose.getTranslation(pose, 0)
        cam.pose.getRotationQuaternion(pose, 3)
        CaptureCamera(f[0] * sx, f[1] * sy, c[0] * sx, c[1] * sy, width, height, pose)
    } catch (_: Exception) {
        null
    }

    /** Mean HSV of the centre 10% of the image (the felt-capture reticle). */
    private fun sampleCenterHsv(image: Image): FloatArray? = sampleYuvHsv(
        image,
        (image.width * 0.45f).toInt(), (image.height * 0.45f).toInt(),
        (image.width * 0.55f).toInt(), (image.height * 0.55f).toInt(),
    )
}
