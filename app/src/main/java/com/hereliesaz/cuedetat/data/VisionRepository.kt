// app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt
package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CameraViewMapping
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.decomposeHomography
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.utils.toMat
import com.hereliesaz.cuedetat.view.model.Perspective
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import org.opencv.core.Rect as OCVRect

import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class VisionRepository @Inject constructor(
    private val poolDetector: MergedTFLiteDetector,
    private val relocaliserUseCase: com.hereliesaz.cuedetat.domain.RelocaliserUseCase
) {
    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    // Fire-once on-demand model fetch (Play). The aiming/ball-detection path
    // can be reached without opening table-scan, so the first processed frame
    // also kicks off model delivery. No-op for foss (model bundled) and after
    // the first call. detectPool() degrades gracefully until the model loads.
    private val modelRequested = AtomicBoolean(false)
    private fun ensureModelOnce() {
        if (modelRequested.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { poolDetector.ensureModelReady() }
            }
        }
    }

    private var arFrameCounter = 0
    private val _arEvents = MutableSharedFlow<MainScreenEvent>(extraBufferCapacity = 16)
    val arEvents: SharedFlow<MainScreenEvent> = _arEvents.asSharedFlow()

    private var reusableMask: Mat? = null
    private val maskRingBuffer = Array(3) { Mat() }
    private var maskRingIndex = 0

    private val reusableFrameMat = Mat()
    private val reusableRotatedMat = Mat()
    private val reusableFullHsvMat = Mat()
    private val reusableHsvMat = Mat()
    private val reusableMean = MatOfDouble()
    private val reusableStdDev = MatOfDouble()

    private val reusableMorphKernel by lazy {
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    }

    // Battery: pooled destination for the per-frame downscaled ML bitmap, plus the Canvas,
    // Paint and Rects to scale into it — replaces a createScaledBitmap allocation (and a
    // recycle) every frame. Safe to reuse: both ML consumers read it synchronously before
    // processImage returns.
    private var pooledScaledBitmap: android.graphics.Bitmap? = null
    private val scaleCanvas = android.graphics.Canvas()
    private val scalePaint = android.graphics.Paint().apply { isFilterBitmap = false }
    private val scaleSrcRect = android.graphics.Rect()
    private val scaleDstRect = android.graphics.Rect()

    private fun scaleIntoPool(src: android.graphics.Bitmap, w: Int, h: Int): android.graphics.Bitmap {
        val dst = pooledScaledBitmap?.takeIf { it.width == w && it.height == h && !it.isRecycled }
            ?: android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                .also { pooledScaledBitmap = it }
        scaleSrcRect.set(0, 0, src.width, src.height)
        scaleDstRect.set(0, 0, w, h)
        scaleCanvas.setBitmap(dst)
        scaleCanvas.drawBitmap(src, scaleSrcRect, scaleDstRect, scalePaint)
        return dst
    }

    private val feltColorDetector = FeltColorDetector()
    private val cvBallDetector = CvBallDetector()
    private var lastFeltDetection: FeltColorDetector.Result? = null

    private val isProcessing = AtomicBoolean(false)
    private var relocaliserFailFrames = 0

    // --- Adaptive battery throttle ---------------------------------------------------
    // The heavy CV/ML pass used to run on every delivered camera frame (~30fps). It now
    // runs at ACTIVE_INTERVAL while the scene is moving and widens toward IDLE_INTERVAL
    // once detections have been stable for MOTION_HOLD_MS, so a static table costs far
    // less without hurting live aiming. AR tracking never widens.
    private var lastFrameAcceptedTime = 0L
    private var adaptiveIntervalMs = ACTIVE_INTERVAL_MS
    private var lastMotionTime = 0L
    private var lastBallSignature = Long.MIN_VALUE

    private companion object {
        const val ACTIVE_INTERVAL_MS = 33L    // ~30fps while the scene is moving / AR tracking
        const val IDLE_INTERVAL_MS = 80L      // ~12fps once detections have been stable
        const val MOTION_HOLD_MS = 1500L      // stay responsive this long after the last change
        const val SIGNATURE_EPSILON = 6.0f    // scaled px a detection must move to count as motion
    }

    /**
     * Cheap, allocation-free gate the analyzer calls *before* the expensive YUV→bitmap
     * conversion. Returns true when enough time has elapsed to process this frame under
     * the current adaptive interval. Read-only: it does not advance the clock — only a
     * committed [processImage] call does.
     */
    fun shouldAcceptFrame(now: Long): Boolean =
        now - lastFrameAcceptedTime >= adaptiveIntervalMs

    /**
     * Recompute the adaptive interval from a cheap fingerprint of the current detections.
     * Any change (a ball moved / appeared / vanished), or being in AR tracking, resets the
     * motion timer and keeps us at the responsive rate; a scene that has been still for
     * MOTION_HOLD_MS drops to the idle rate.
     */
    private fun updateAdaptiveInterval(state: CueDetatState, now: Long, detections: List<PoolDetection>) {
        val isArTracking = state.cameraMode == CameraMode.AR_ACTIVE || state.cameraMode == CameraMode.AR_SETUP
        var sig = 1L
        for (d in detections) {
            sig = sig * 31 + (d.rect.centerX() / SIGNATURE_EPSILON).toLong()
            sig = sig * 31 + (d.rect.centerY() / SIGNATURE_EPSILON).toLong()
        }
        if (sig != lastBallSignature || isArTracking) {
            lastBallSignature = sig
            lastMotionTime = now
        }
        adaptiveIntervalMs = if (isArTracking || now - lastMotionTime < MOTION_HOLD_MS) {
            ACTIVE_INTERVAL_MS
        } else {
            IDLE_INTERVAL_MS
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy?, bitmap: android.graphics.Bitmap, state: CueDetatState) {
        ensureModelOnce()
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy?.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        // Battery: drop frames that arrive faster than the adaptive interval allows.
        if (!shouldAcceptFrame(currentTime)) {
            imageProxy?.close()
            isProcessing.set(false)
            return
        }
        lastFrameAcceptedTime = currentTime

        val originalMat = Mat()

        try {
            val rotationDegrees = imageProxy?.imageInfo?.rotationDegrees ?: 0

            // Full-resolution raw (sensor-orientation) BGR frame: balls are found here. At a
            // quarter scale a ball is two pixels across and nothing can find it.
            val fullMat = if (imageProxy != null) imageProxy.toMat(reusableFrameMat) else {
                org.opencv.android.Utils.bitmapToMat(bitmap, reusableFrameMat)
                Imgproc.cvtColor(reusableFrameMat, reusableFrameMat, Imgproc.COLOR_RGBA2BGR)
                reusableFrameMat
            }

            // Quarter-scale copy for the cheap whole-frame work (motion throttle, felt colour,
            // debug mask, edge tracking, snapshot).
            val scaledWidth = (fullMat.cols() / 4).coerceAtLeast(1)
            val scaledHeight = (fullMat.rows() / 4).coerceAtLeast(1)
            val scaledBitmap = scaleIntoPool(bitmap, scaledWidth, scaledHeight)
            Imgproc.resize(fullMat, originalMat, Size(scaledWidth.toDouble(), scaledHeight.toDouble()))

            // detectPool() (HEAD_POOL_PIVOT / segment 2) is trained on the 3-class
            // table/hole/side scheme documented in PoolDetection.kt and ml/metadata.yaml;
            // there is no ball/cue head. Its output is only a cheap motion signal for the
            // adaptive frame-rate throttle.
            // TODO(ml): if a ball-trained head is ever added, merge its output with the CV balls.
            val rawDetections = poolDetector.detectPool(scaledBitmap)
            updateAdaptiveInterval(state, currentTime, rawDetections)

            val matToUse: Mat
            when (rotationDegrees) {
                90 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE); matToUse = reusableRotatedMat }
                180 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_180); matToUse = reusableRotatedMat }
                270 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE); matToUse = reusableRotatedMat }
                // rotationDegrees == 0: no rotation needed, but captureRectifiedSnapshot()
                // unconditionally reads reusableRotatedMat and bails if it's still empty
                // from a prior frame. Keep it populated regardless of this frame's
                // rotation so the top-down snapshot feature works on devices that never
                // hit the 90/180/270 branches above.
                else -> { originalMat.copyTo(reusableRotatedMat); matToUse = originalMat }
            }

            Imgproc.cvtColor(matToUse, reusableHsvMat, Imgproc.COLOR_BGR2HSV)
            val hsvMat = reusableHsvMat

            val hsvTuple = state.colorSamplePoint?.let {
                val imageX = (it.x * (hsvMat.cols() / state.viewWidth.toFloat())).toInt()
                val imageY = (it.y * (hsvMat.rows() / state.viewHeight.toFloat())).toInt()

                val patchSize = 5
                // Coerce both axes — the original code only clamped X, so a sample
                // point near the bottom of the frame could send submat() out of bounds.
                val maxX = (hsvMat.cols() - patchSize).coerceAtLeast(0)
                val maxY = (hsvMat.rows() - patchSize).coerceAtLeast(0)
                val roiX = (imageX - patchSize / 2).coerceIn(0, maxX)
                val roiY = (imageY - patchSize / 2).coerceIn(0, maxY)
                val roi = OCVRect(roiX, roiY, patchSize, patchSize)

                val sampleRegion = hsvMat.submat(roi)
                Core.meanStdDev(sampleRegion, reusableMean, reusableStdDev)

                val meanArray = floatArrayOf(
                    reusableMean.get(0, 0)[0].toFloat(),
                    reusableMean.get(1, 0)[0].toFloat(),
                    reusableMean.get(2, 0)[0].toFloat()
                )
                val stddevArray = floatArrayOf(
                    reusableStdDev.get(0, 0)[0].toFloat(),
                    reusableStdDev.get(1, 0)[0].toFloat(),
                    reusableStdDev.get(2, 0)[0].toFloat()
                )

                sampleRegion.release()
                Pair(meanArray, stddevArray)
            }

            val autoFelt = if (state.lockedHsvColor == null && hsvTuple == null) {
                feltColorDetector.detect(hsvMat)?.also { lastFeltDetection = it }
            } else null

            val hsv = state.lockedHsvColor ?: hsvTuple?.first ?: autoFelt?.hsv ?: run {
                val roiWidth = 50
                val roiHeight = 50
                val roiX = (hsvMat.cols() - roiWidth) / 2
                val roiY = (hsvMat.rows() - roiHeight) / 2
                val centerRoi = hsvMat.submat(OCVRect(roiX, roiY, roiWidth, roiHeight))
                val meanColor = Core.mean(centerRoi)
                centerRoi.release()
                floatArrayOf(
                    meanColor.`val`[0].toFloat(),
                    meanColor.`val`[1].toFloat(),
                    meanColor.`val`[2].toFloat()
                )
            }

            val cvMask = if (state.showCvMask) {
                if (reusableMask == null) {
                    reusableMask = Mat()
                }
                val mask = reusableMask!!
                if (state.lockedHsvColor != null && state.lockedHsvStdDev != null) {
                    val mean = state.lockedHsvColor
                    val stdDev = state.lockedHsvStdDev
                    val stdDevMultiplier = 2.0
                    val lowerBound = Scalar(
                        (mean[0] - stdDevMultiplier * stdDev[0]).coerceAtLeast(0.0),
                        (mean[1] - stdDevMultiplier * stdDev[1]).coerceAtLeast(40.0),
                        (mean[2] - stdDevMultiplier * stdDev[2]).coerceAtLeast(40.0)
                    )
                    val upperBound = Scalar(
                        (mean[0] + stdDevMultiplier * stdDev[0]).coerceAtMost(180.0),
                        (mean[1] + stdDevMultiplier * stdDev[1]).coerceAtMost(255.0),
                        (mean[2] + stdDevMultiplier * stdDev[2]).coerceAtMost(255.0)
                    )
                    Core.inRange(hsvMat, lowerBound, upperBound, mask)
                } else {
                    val hueRange = 10.0
                    val lowerBound = Scalar((hsv[0] - hueRange).coerceAtLeast(0.0), 100.0, 100.0)
                    val upperBound = Scalar((hsv[0] + hueRange).coerceAtMost(180.0), 255.0, 255.0)
                    Core.inRange(hsvMat, lowerBound, upperBound, mask)
                }

                Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, reusableMorphKernel)

                val outputMask = maskRingBuffer[maskRingIndex]
                mask.copyTo(outputMask)
                maskRingIndex = (maskRingIndex + 1) % 3
                outputMask
            } else {
                reusableMask?.release()
                reusableMask = null
                null
            }

            val frameToView = Matrix().apply {
                setValues(
                    CameraViewMapping.fillCenter(
                        fullMat.cols(), fullMat.rows(), rotationDegrees, state.viewWidth, state.viewHeight
                    )
                )
            }
            val balls = detectBallsInFrame(fullMat, frameToView, state, autoFelt)

            var finalVisionData = VisionData(
                // genericBalls and balls share one coord system (logical when a pose exists,
                // screen otherwise), which the snap/obstacle/gesture reducers compare against.
                genericBalls = balls.map { it.position },
                balls = balls,
                detectedHsvColor = hsvTuple?.first ?: hsv,
                detectedBoundingBoxes = balls.mapNotNull { it.boundingBox },
                // detectedCues intentionally left at its default (empty): there is no
                // trained cue-detecting head — see the note above where rawDetections
                // is computed.
                cvMask = cvMask,
                // Boxes are in raw full-frame pixels; the renderer rotates and centre-crops
                // them with these.
                sourceImageWidth = fullMat.cols(),
                sourceImageHeight = fullMat.rows(),
                sourceImageRotation = rotationDegrees
            )

            var currentConfidence = state.visionData?.tableOverlayConfidence ?: 0f
            if ((state.cameraMode == CameraMode.AR_ACTIVE || state.cameraMode == CameraMode.AR_SETUP) &&
                state.tableScanModel != null && state.depthCapability == DepthCapability.NONE) {
                arFrameCounter++
                if (arFrameCounter % 5 == 0) {
                    currentConfidence = runArTrackingPass(matToUse, state, matToUse.cols(), matToUse.rows(), rotationDegrees)
                }
            }

            finalVisionData = finalVisionData.copy(tableOverlayConfidence = currentConfidence)
            _visionDataFlow.value = finalVisionData

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            originalMat.release()
            // The scaled bitmap is pooled — do NOT recycle; it is reused next frame.
            imageProxy?.close()
            isProcessing.set(false)
        }
    }

    /**
     * Balls in one full-resolution raw frame, for both camera paths.
     *
     * @param frame BGR frame in sensor orientation, full resolution
     * @param frameToView maps [frame] pixels to view pixels (rotation and crop included)
     * @param autoFelt this frame's auto-detected felt colour, if any
     * @return balls with positions in logical table space when a pose exists, view pixels
     *   otherwise; bounding boxes in [frame] pixels
     */
    private fun detectBallsInFrame(
        frame: Mat,
        frameToView: Matrix,
        state: CueDetatState,
        autoFelt: FeltColorDetector.Result?,
    ): List<DetectedBall> {
        val feltMean = state.lockedHsvColor ?: autoFelt?.hsv ?: lastFeltDetection?.hsv ?: return emptyList()
        val feltSd = state.lockedHsvStdDev
            ?: autoFelt?.stdDev
            ?: lastFeltDetection?.stdDev
            ?: floatArrayOf(8f, 40f, 50f)

        Imgproc.cvtColor(frame, reusableFullHsvMat, Imgproc.COLOR_BGR2HSV)

        val viewToFrame = Matrix()
        val pose = if (frameToView.invert(viewToFrame)) tablePoseInFrame(state, frameToView, viewToFrame) else null

        val detections = cvBallDetector.detect(
            frame, reusableFullHsvMat, feltMean, feltSd,
            tablePolygon = pose?.polygon,
            radiusAt = pose?.radiusAt,
        )

        val inverse = state.inversePitchMatrix?.takeIf { state.hasInverseMatrix }
        val pt = FloatArray(2)
        return detections.map { d ->
            pt[0] = d.center.x; pt[1] = d.center.y
            frameToView.mapPoints(pt)
            val screen = PointF(pt[0], pt[1])
            val position = if (inverse != null) {
                val lp = Perspective.screenToLogical(screen, inverse)
                state.lensWarpTps?.let { ThinPlateSpline.applyWarp(it, lp) } ?: lp
            } else screen
            DetectedBall(
                position = position,
                type = d.type,
                confidence = d.confidence,
                boundingBox = android.graphics.Rect(
                    (d.center.x - d.radius).toInt(), (d.center.y - d.radius).toInt(),
                    (d.center.x + d.radius).toInt(), (d.center.y + d.radius).toInt(),
                ),
            )
        }
    }

    /** The playing surface and ball size in frame pixels, derived from the table pose. */
    private class FramePose(val polygon: List<PointF>, val radiusAt: (Float, Float) -> Float)

    /**
     * Projects the table into the frame: its corners give the region balls can be in, and a
     * ball-radius step in logical space at any frame point gives the ball's size there
     * (perspective makes far balls smaller). Null without a pose or a visible table.
     */
    private fun tablePoseInFrame(state: CueDetatState, frameToView: Matrix, viewToFrame: Matrix): FramePose? {
        val pitch = state.pitchMatrix ?: return null
        val inverse = state.inversePitchMatrix ?: return null
        if (!state.hasInverseMatrix || !state.table.isVisible) return null

        val corners = FloatArray(8)
        state.table.corners.forEachIndexed { i, c -> corners[i * 2] = c.x; corners[i * 2 + 1] = c.y }
        pitch.mapPoints(corners)
        viewToFrame.mapPoints(corners)
        val polygon = List(4) { i -> PointF(corners[i * 2], corners[i * 2 + 1]) }

        val radiusAt = { x: Float, y: Float ->
            val p = floatArrayOf(x, y)
            frameToView.mapPoints(p)
            inverse.mapPoints(p)
            val r = LOGICAL_BALL_RADIUS
            // Centre, one radius along logical x, one along logical y, back into the frame.
            val q = floatArrayOf(p[0], p[1], p[0] + r, p[1], p[0], p[1] + r)
            pitch.mapPoints(q)
            viewToFrame.mapPoints(q)
            // A sphere is round from any angle; foreshortening only squashes the table, so the
            // longer of the two steps is the ball's radius.
            max(hypot(q[2] - q[0], q[3] - q[1]), hypot(q[4] - q[0], q[5] - q[1]))
        }
        return FramePose(polygon, radiusAt)
    }

    /**
     * AR-path detection on one frame. Call off the GL thread: this is the whole CV pass.
     *
     * @param fullMat ARCore CPU image as BGR, sensor orientation, full resolution. Read only;
     *   the caller owns and releases it.
     * @param rotationDegrees clockwise rotation to upright; used only when [frameToView] is null
     * @param frameToView maps [fullMat] pixels to view pixels, from ARCore's own
     *   `Frame.transformCoordinates2d` (its crop differs from CameraX's); null falls back to a
     *   centre-crop estimate
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun processArFrame(fullMat: Mat, rotationDegrees: Int, state: CueDetatState, frameToView: Matrix? = null) {
        ensureModelOnce()
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }

        val smallMat = Mat()

        try {
            // Quarter-scale HSV, for felt auto-detection only.
            Imgproc.resize(fullMat, smallMat, Size((fullMat.cols() / 4).toDouble(), (fullMat.rows() / 4).toDouble()))
            Imgproc.cvtColor(smallMat, reusableHsvMat, Imgproc.COLOR_BGR2HSV)
            val autoFelt = if (state.lockedHsvColor == null) {
                feltColorDetector.detect(reusableHsvMat)?.also { lastFeltDetection = it }
            } else null
            val hsv = state.lockedHsvColor ?: autoFelt?.hsv ?: lastFeltDetection?.hsv

            val mapping = frameToView ?: Matrix().apply {
                setValues(
                    CameraViewMapping.fillCenter(
                        fullMat.cols(), fullMat.rows(), rotationDegrees, state.viewWidth, state.viewHeight
                    )
                )
            }
            var balls = detectBallsInFrame(fullMat, mapping, state, autoFelt)
            if (state.hasInverseMatrix && state.table.isVisible) {
                balls = balls.filter { state.table.isPointInside(it.position) }
            }

            _visionDataFlow.value = VisionData(
                genericBalls = balls.map { it.position },
                balls = balls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = balls.mapNotNull { it.boundingBox },
                sourceImageWidth = fullMat.cols(),
                sourceImageHeight = fullMat.rows(),
                sourceImageRotation = rotationDegrees,
            )

            val depth = state.depthPlane
            if (depth != null && depth.confidence > 0.6f && state.tableScanModel == null) {
                runDepthOnlyScaleUpdate(depth, state)
            }

            var currentConfidence = state.visionData?.tableOverlayConfidence ?: 0f
            if ((state.cameraMode == CameraMode.AR_ACTIVE || state.cameraMode == CameraMode.AR_SETUP) &&
                state.tableScanModel != null && state.depthCapability == DepthCapability.NONE) {
                arFrameCounter++
                if (arFrameCounter % 5 == 0) {
                    when (rotationDegrees) {
                        90 -> Core.rotate(smallMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE)
                        180 -> Core.rotate(smallMat, reusableRotatedMat, Core.ROTATE_180)
                        270 -> Core.rotate(smallMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                        else -> smallMat.copyTo(reusableRotatedMat)
                    }
                    currentConfidence = runArTrackingPass(
                        reusableRotatedMat, state, reusableRotatedMat.cols(), reusableRotatedMat.rows(), rotationDegrees
                    )
                }
            }
            _visionDataFlow.value = _visionDataFlow.value.copy(tableOverlayConfidence = currentConfidence)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            smallMat.release()
            isProcessing.set(false)
        }
    }

    private fun runDepthOnlyScaleUpdate(
        depth: com.hereliesaz.cuedetat.domain.DepthPlane,
        state: CueDetatState,
    ) {
        val clampedDist = depth.distanceMeters.coerceIn(0.5f, 3.0f)
        val normalised = (clampedDist - 0.5f) / 2.5f
        val targetSlider = 50f * (1f - normalised) - 25f
        val currentSlider = state.zoomSliderPosition
        val delta = abs(targetSlider - currentSlider)

        if (delta > 3f) {
            emitEvent(MainScreenEvent.UpdateArPose(
                translation = androidx.compose.ui.geometry.Offset(state.viewOffset.x, state.viewOffset.y),
                rotation = state.worldRotationDegrees,
                scale = com.hereliesaz.cuedetat.ui.ZoomMapping.sliderToZoom(
                    targetSlider,
                    com.hereliesaz.cuedetat.ui.ZoomMapping.getZoomRange(state.experienceMode).first,
                    com.hereliesaz.cuedetat.ui.ZoomMapping.getZoomRange(state.experienceMode).second,
                ),
            ))
        }
    }

    private fun emitEvent(event: MainScreenEvent) {
        _arEvents.tryEmit(event)
    }

    private fun runArTrackingPass(
        mat: Mat,
        state: CueDetatState,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ): Float {
        if (state.tableScanModel == null || !state.hasInverseMatrix) return 0f

        // ML Strategy eradicated.
        // The pocket detector is dead post-scan. Stop searching for things you have already found.
        // Edge fallback reigns.

        return runEdgeFallback(mat, state, imageWidth, imageHeight, rotationDegrees)
    }

    private fun applyPoseUpdate(rawT: Offset, rawR: Float, rawS: Float, state: CueDetatState) {
        val alpha = if (state.experienceMode == com.hereliesaz.cuedetat.domain.ExperienceMode.EXPERT) 0.05f else 0.2f

        val blendedTranslation = Offset(
            alpha * rawT.x + (1f - alpha) * state.viewOffset.x,
            alpha * rawT.y + (1f - alpha) * state.viewOffset.y
        )
        val blendedRotation = alpha * rawR + (1f - alpha) * state.worldRotationDegrees
        val currentZoom = ZoomMapping.sliderToZoom(
            state.zoomSliderPosition,
            ZoomMapping.getZoomRange(state.experienceMode).first,
            ZoomMapping.getZoomRange(state.experienceMode).second
        )
        val blendedScale = alpha * rawS + (1f - alpha) * currentZoom

        val delta = hypot((blendedTranslation.x - state.viewOffset.x).toDouble(), (blendedTranslation.y - state.viewOffset.y).toDouble())
        // Guard against NaN/Inf propagating from a degenerate homography.
        if (!delta.isFinite() || !blendedRotation.isFinite() || !blendedScale.isFinite()) return
        if (delta > 0.5) {
            emitEvent(MainScreenEvent.UpdateArPose(blendedTranslation, blendedRotation, blendedScale))
        }
    }


    private fun validatePocketSurrounds(
        currentFrame: Mat,
        model: com.hereliesaz.cuedetat.domain.TableScanModel,
        homography: Mat,
        imageWidth: Int,
        imageHeight: Int
    ): Boolean {
        val saved = model.pocketSurroundHistograms ?: return true
        if (saved.isEmpty()) return true

        val hInv = homography.inv()
        val currentHistograms = mutableMapOf<com.hereliesaz.cuedetat.domain.PocketId, List<Float>>()

        try {
            for (cluster in model.pockets) {
                val logicalPt = MatOfPoint2f(
                    org.opencv.core.Point(cluster.logicalPosition.x.toDouble(), cluster.logicalPosition.y.toDouble())
                )
                val imgPt = MatOfPoint2f()
                try {
                    Core.perspectiveTransform(logicalPt, imgPt, hInv)
                    val pt = imgPt.toList().firstOrNull() ?: continue

                    val px = pt.x.toInt().coerceIn(20, imageWidth - 20)
                    val py = pt.y.toInt().coerceIn(20, imageHeight - 20)
                    val surroundSize = 20

                    val roi = org.opencv.core.Rect(px - surroundSize, py - surroundSize, surroundSize * 2, surroundSize * 2)
                    if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > imageWidth ||
                        roi.y + roi.height > imageHeight) continue

                    val roiMat = currentFrame.submat(roi)
                    val hsvRoi = Mat()
                    val hist = Mat()
                    val histChannels = org.opencv.core.MatOfInt(2)
                    val histBins = org.opencv.core.MatOfInt(16)
                    val histRanges = org.opencv.core.MatOfFloat(0f, 256f)
                    val histMask = Mat()
                    try {
                        Imgproc.cvtColor(roiMat, hsvRoi, Imgproc.COLOR_BGR2HSV)
                        Imgproc.calcHist(
                            listOf(hsvRoi),
                            histChannels,
                            histMask,
                            hist,
                            histBins,
                            histRanges
                        )
                        Core.normalize(hist, hist)
                        val currentHist = (0 until 16).map { hist.get(it, 0)[0].toFloat() }
                        currentHistograms[cluster.identity] = currentHist
                    } finally {
                        histMask.release()
                        histRanges.release()
                        histBins.release()
                        histChannels.release()
                        hist.release()
                        hsvRoi.release()
                        roiMat.release()
                    }
                } finally {
                    imgPt.release()
                    logicalPt.release()
                }
            }
        } finally {
            hInv.release()
        }

        return relocaliserUseCase.validateHistograms(saved, currentHistograms)
    }

    internal fun runEdgeFallback(
        mat: Mat,
        state: CueDetatState,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ): Float {
        val model = state.tableScanModel ?: return 0f
        val hsv = floatArrayOf(model.feltColorHsv[0], model.feltColorHsv[1], model.feltColorHsv[2])
        val hsvMat = Mat()
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)

        val lower = Scalar(
            maxOf(0.0, hsv[0] - 20.0),
            maxOf(0.0, hsv[1] * 255 - 60.0),
            maxOf(0.0, hsv[2] * 255 - 60.0)
        )
        val upper = Scalar(
            minOf(180.0, hsv[0] + 20.0),
            minOf(255.0, hsv[1] * 255 + 60.0),
            minOf(255.0, hsv[2] * 255 + 60.0)
        )

        val mask = Mat()
        Core.inRange(hsvMat, lower, upper, mask)
        hsvMat.release()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(9.0, 9.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel)

        val contours = mutableListOf<MatOfPoint>()
        val contourHierarchy = Mat()
        try {
            Imgproc.findContours(mask, contours, contourHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        } finally {
            contourHierarchy.release()
            mask.release()
            kernel.release()
        }

        val largest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return 0f
        if (Imgproc.contourArea(largest) < mat.rows() * mat.cols() * 0.05) return 0f

        val approx = MatOfPoint2f()
        val contour2f = MatOfPoint2f(*largest.toArray())
        var epsilonCoeff = 0.01
        val perimeter = Imgproc.arcLength(contour2f, true)

        while (epsilonCoeff < 0.1) {
            Imgproc.approxPolyDP(contour2f, approx, epsilonCoeff * perimeter, true)
            if (approx.rows() == 4) break
            epsilonCoeff += 0.01
        }

        val pts: Array<org.opencv.core.Point>
        var confidence = 0.8f
        if (approx.rows() == 4) {
            pts = approx.toArray()
        } else {
            val rect = Imgproc.minAreaRect(contour2f)
            val rectPts = arrayOfNulls<org.opencv.core.Point>(4)
            rect.points(rectPts)
            pts = Array(4) { rectPts[it]!! }
            confidence = 0.6f
        }

        approx.release()
        contour2f.release()
        contours.forEach { it.release() }

        val imageCenter = org.opencv.core.Point(mat.cols() / 2.0, mat.rows() / 2.0)
        val sortedCorners = pts.sortedBy { Math.atan2(it.y - imageCenter.y, it.x - imageCenter.x) }

        val logicalWidth = state.table.logicalWidth
        val logicalHeight = state.table.logicalHeight
        val logicalCorners = listOf(
            org.opencv.core.Point(-logicalWidth / 2.0, -logicalHeight / 2.0),
            org.opencv.core.Point(logicalWidth / 2.0, -logicalHeight / 2.0),
            org.opencv.core.Point(logicalWidth / 2.0, logicalHeight / 2.0),
            org.opencv.core.Point(-logicalWidth / 2.0, logicalHeight / 2.0)
        )

        val srcMat = MatOfPoint2f(*sortedCorners.toTypedArray())
        val dstMat = MatOfPoint2f(*logicalCorners.toTypedArray())

        val h = Calib3d.findHomography(srcMat, dstMat, Calib3d.RANSAC, 5.0)
        srcMat.release()
        dstMat.release()

        if (h.empty()) return 0f

        val (rawT, rawR, rawS) = decomposeHomography(h, imageWidth.toFloat(), imageHeight.toFloat())

        applyPoseUpdate(rawT, rawR, rawS, state)

        if (state.relocaliserDeltaQ != null) {
            val isValidTable = validatePocketSurrounds(mat, model, h, imageWidth, imageHeight)
            if (isValidTable) {
                _arEvents.tryEmit(MainScreenEvent.ForceArActive)
                relocaliserFailFrames = 0
            } else {
                relocaliserFailFrames++
                if (relocaliserFailFrames >= 5) {
                    relocaliserFailFrames = 0
                    _arEvents.tryEmit(MainScreenEvent.SeedRelocaliser(null))
                }
            }
        }

        h.release()

        return confidence
    }

    private fun getTransformationMatrix(
        sourceWidth: Int, sourceHeight: Int,
        destWidth: Int, destHeight: Int
    ): Matrix {
        val matrix = Matrix()
        val sx = destWidth.toFloat() / sourceWidth.toFloat()
        val sy = destHeight.toFloat() / sourceHeight.toFloat()
        matrix.postScale(sx, sy)
        return matrix
    }

    fun captureRectifiedSnapshot(state: CueDetatState) {
        val mat = if (reusableRotatedMat.empty()) null else reusableRotatedMat
        if (mat == null) return

        val width = mat.cols()
        val height = mat.rows()

        val outW = 2048.0
        val outH = 1024.0

        val halfW = state.table.logicalWidth / 2f
        val halfH = state.table.logicalHeight / 2f

        val corners = arrayOf(
            PointF(-halfW, -halfH), // TL
            PointF(halfW, -halfH),  // TR
            PointF(halfW, halfH),   // BR
            PointF(-halfW, halfH)   // BL
        )

        val pitchMatrix = state.pitchMatrix ?: return
        val imageToScreen = Matrix().apply {
            postScale(state.viewWidth.toFloat() / width.toFloat(), state.viewHeight.toFloat() / height.toFloat())
        }
        val screenToImage = Matrix().apply { imageToScreen.invert(this) }

        val srcPointsList = mutableListOf<org.opencv.core.Point>()
        val pts = FloatArray(2)
        for (c in corners) {
            pts[0] = c.x
            pts[1] = c.y
            pitchMatrix.mapPoints(pts)      // Logical -> Screen
            screenToImage.mapPoints(pts)    // Screen -> Image
            srcPointsList.add(org.opencv.core.Point(pts[0].toDouble(), pts[1].toDouble()))
        }

        val dstPointsList = listOf(
            org.opencv.core.Point(0.0, 0.0),
            org.opencv.core.Point(outW, 0.0),
            org.opencv.core.Point(outW, outH),
            org.opencv.core.Point(0.0, outH)
        )

        val srcMat = MatOfPoint2f()
        srcMat.fromList(srcPointsList)
        val dstMat = MatOfPoint2f()
        dstMat.fromList(dstPointsList)

        val transform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        val outMat = Mat()
        Imgproc.warpPerspective(mat, outMat, transform, Size(outW, outH))

        val bitmap = createBitmap(outW.toInt(), outH.toInt())
        org.opencv.android.Utils.matToBitmap(outMat, bitmap)

        outMat.release()
        transform.release()
        srcMat.release()
        dstMat.release()

        CoroutineScope(Dispatchers.Main).launch {
            _arEvents.emit(MainScreenEvent.SetTopDownBitmap(bitmap))
        }
    }
}