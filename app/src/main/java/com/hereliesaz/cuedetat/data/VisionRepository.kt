// app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt
package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.domain.CameraMode
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.DepthCapability
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ThinPlateSpline
import com.hereliesaz.cuedetat.domain.decomposeHomography
import com.hereliesaz.cuedetat.ui.ZoomMapping
import com.hereliesaz.cuedetat.utils.toMat
import android.media.Image as MediaImage
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
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
import kotlin.math.pow
import org.opencv.core.Rect as OCVRect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap

@Singleton
class VisionRepository @Inject constructor(
    @GenericDetector private val genericObjectDetector: ObjectDetector,
    private val poolDetector: MergedTFLiteDetector,
    private val myriadRepository: MyriadRepository,
    private val relocaliserUseCase: com.hereliesaz.cuedetat.domain.RelocaliserUseCase
) {
    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    private var arFrameCounter = 0
    private val _arEvents = MutableSharedFlow<MainScreenEvent>(extraBufferCapacity = 16)
    val arEvents: SharedFlow<MainScreenEvent> = _arEvents.asSharedFlow()

    private var lastMyriadTime = 0L

    private var reusableMask: Mat? = null
    private val maskRingBuffer = Array(3) { Mat() }
    private var maskRingIndex = 0

    private val reusableFrameMat = Mat()
    private val reusableRotatedMat = Mat()
    private val reusableHsvMat = Mat()
    private val reusableGray = Mat()
    private val reusableEdges = Mat()
    private val reusableHierarchy = Mat()
    private val reusableMatrixValues = FloatArray(9)
    private val reusablePointArray = FloatArray(2)
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
        if (isProcessing.get()) {
            imageProxy?.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        // Battery: drop frames that arrive faster than the adaptive interval allows.
        if (!shouldAcceptFrame(currentTime)) {
            imageProxy?.close()
            return
        }
        isProcessing.set(true)
        lastFrameAcceptedTime = currentTime

        var scaledBitmap: android.graphics.Bitmap? = null
        val originalMat = Mat()

        try {
            val rotationDegrees = imageProxy?.imageInfo?.rotationDegrees ?: 0

            // Downsample the machine's eyes. The silicon torture stops here.
            val scaledWidth = (bitmap.width / 4).coerceAtLeast(1)
            val scaledHeight = (bitmap.height / 4).coerceAtLeast(1)
            scaledBitmap = scaleIntoPool(bitmap, scaledWidth, scaledHeight)
            val inputImage = InputImage.fromBitmap(scaledBitmap, rotationDegrees)

            val fullMat = if (imageProxy != null) imageProxy.toMat(reusableFrameMat) else {
                org.opencv.android.Utils.bitmapToMat(bitmap, reusableFrameMat)
                reusableFrameMat
            }
            Imgproc.resize(fullMat, originalMat, Size(scaledWidth.toDouble(), scaledHeight.toDouble()))

            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detectPool(scaledBitmap)
            // Battery: widen/narrow the frame interval based on detection stability.
            updateAdaptiveInterval(state, currentTime, rawDetections)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }

            var matToUse: Mat
            when (rotationDegrees) {
                90 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE); matToUse = reusableRotatedMat }
                180 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_180); matToUse = reusableRotatedMat }
                270 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE); matToUse = reusableRotatedMat }
                else -> { matToUse = originalMat }
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

            val hasTablePose = state.hasInverseMatrix && state.pitchMatrix != null
            val filteredDetectedObjects = detectedObjects.filter {
                val box = it.boundingBox
                if (!hasTablePose) {
                    val side = minOf(box.width(), box.height())
                    side in 10..400
                } else {
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY().toFloat(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }
            }

            val refinedScreenPoints = filteredDetectedObjects.map { detectedObject ->
                val box = detectedObject.boundingBox
                val fallback = PointF(box.exactCenterX(), box.exactCenterY())
                refineBallCenter(detectedObject, matToUse, state, imageToScreenMatrix) ?: fallback
            }.map { pointInImageCoords ->
                val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                imageToScreenMatrix.mapPoints(screenPointArray)
                PointF(screenPointArray[0], screenPointArray[1])
            }

            val filteredCustomPoolBalls = customPoolBalls.filter {
                val box = it.rect
                if (!hasTablePose) {
                    val side = minOf(box.width(), box.height())
                    side in 10f..400f
                } else {
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }
            }

            val customScreenPoints = filteredCustomPoolBalls.map { detectedObject ->
                val box = detectedObject.rect
                val fallback = PointF(box.centerX(), box.centerY())
                refineBallCenterPoolDetection(detectedObject, matToUse, state, imageToScreenMatrix) ?: fallback
            }.map { pointInImageCoords ->
                val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                imageToScreenMatrix.mapPoints(screenPointArray)
                PointF(screenPointArray[0], screenPointArray[1])
            }

            // MYRIAD PREDICTION: Automatically track physical cue stick
            if (detectedCues.isNotEmpty() && filteredCustomPoolBalls.isNotEmpty()) {
                if (currentTime - lastMyriadTime > 2000L) {
                    lastMyriadTime = currentTime

                    val cueRect = detectedCues.first()
                    val poolBall = filteredCustomPoolBalls.first()
                    val cueCenter = PointF(cueRect.centerX().toFloat(), cueRect.centerY().toFloat())
                    val ballCenter = PointF(poolBall.rect.centerX(), poolBall.rect.centerY())

                    val pokeDx = ballCenter.x - cueCenter.x
                    val pokeDy = ballCenter.y - cueCenter.y

                    val currentMatrix = Matrix(imageToScreenMatrix)
                    val invMat = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    val hasInverse = state.hasInverseMatrix

                    val bmpCopy = scaledBitmap.copy(scaledBitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888, false)

                    CoroutineScope(Dispatchers.IO).launch {
                        val result = myriadRepository.fetchTrajectory(bmpCopy, ballCenter, PointF(pokeDx, pokeDy))
                        result.onSuccess { response ->
                            val mappedPoints = response.points.mapNotNull { pt ->
                                val screenPoint = floatArrayOf(pt.x, pt.y)
                                currentMatrix.mapPoints(screenPoint)
                                val sp = PointF(screenPoint[0], screenPoint[1])
                                if (hasInverse) {
                                    val logical = Perspective.screenToLogical(sp, invMat)
                                    if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                                } else sp
                            }
                            emitEvent(MainScreenEvent.MyriadTrajectoryReceived(mappedPoints))
                        }
                        bmpCopy.recycle()
                    }
                }
            }

            val genericBallsStructured = refinedScreenPoints.mapIndexed { idx, sp ->
                val logical = if (state.hasInverseMatrix) {
                    val inv = state.inversePitchMatrix ?: Matrix()
                    val lp = Perspective.screenToLogical(sp, inv)
                    if (state.lensWarpTps != null) ThinPlateSpline.applyWarp(state.lensWarpTps, lp) else lp
                } else sp

                val box = filteredDetectedObjects[idx].boundingBox
                val ballType = classifyBallType(matToUse, box)
                DetectedBall(position = logical, type = ballType, confidence = 0.9f, boundingBox = box)
            }

            val customBallsStructured = customScreenPoints.mapIndexed { idx, sp ->
                val logical = if (state.hasInverseMatrix) {
                    val inv = state.inversePitchMatrix ?: Matrix()
                    val lp = Perspective.screenToLogical(sp, inv)
                    if (state.lensWarpTps != null) ThinPlateSpline.applyWarp(state.lensWarpTps, lp) else lp
                } else sp

                val box = filteredCustomPoolBalls[idx].rect
                val intBox = android.graphics.Rect(box.left.toInt(), box.top.toInt(), box.right.toInt(), box.bottom.toInt())
                val ballType = classifyBallType(matToUse, intBox)
                DetectedBall(position = logical, type = ballType, confidence = 0.9f, boundingBox = intBox)
            }

            val mlBalls = genericBallsStructured + customBallsStructured

            val feltMean = state.lockedHsvColor
                ?: autoFelt?.hsv
                ?: lastFeltDetection?.hsv
            val feltSd = state.lockedHsvStdDev
                ?: autoFelt?.stdDev
                ?: lastFeltDetection?.stdDev
                ?: floatArrayOf(8f, 40f, 50f)

            val cvDetections = if (feltMean != null) {
                val mlBoxSides = (filteredDetectedObjects.map {
                    minOf(it.boundingBox.width(), it.boundingBox.height())
                } + filteredCustomPoolBalls.map {
                    minOf(it.rect.width(), it.rect.height()).toInt()
                }).filter { it > 0 }
                val medianSide = if (mlBoxSides.isNotEmpty()) {
                    mlBoxSides.sorted()[mlBoxSides.size / 2].toFloat()
                } else 0f
                val expectedRadiusPx = medianSide / 2f
                cvBallDetector.detect(matToUse, hsvMat, feltMean, feltSd, expectedRadiusPx)
            } else emptyList()

            val cvScreenBalls = cvDetections.map { d ->
                val arr = floatArrayOf(d.center.x, d.center.y)
                imageToScreenMatrix.mapPoints(arr)
                val sp = PointF(arr[0], arr[1])
                val logical = if (state.hasInverseMatrix) {
                    val inv = state.inversePitchMatrix ?: Matrix()
                    val lp = Perspective.screenToLogical(sp, inv)
                    if (state.lensWarpTps != null) ThinPlateSpline.applyWarp(state.lensWarpTps, lp) else lp
                } else sp
                val box = android.graphics.Rect(
                    (d.center.x - d.radius).toInt(),
                    (d.center.y - d.radius).toInt(),
                    (d.center.x + d.radius).toInt(),
                    (d.center.y + d.radius).toInt(),
                )
                DetectedBall(position = logical, type = d.type, confidence = d.confidence, boundingBox = box)
            }

            // mlBalls.position and cvScreenBalls.position live in the same coord
            // system as each other (logical inches if hasInverseMatrix, screen pixels
            // otherwise). The merge threshold must live in that same system, not in
            // image-pixel units like CvBallDetector.radius. Pick the threshold to match.
            val mergeDistanceSq = if (state.hasInverseMatrix) {
                val r = LOGICAL_BALL_RADIUS * 1.5f
                r * r
            } else {
                imageToScreenMatrix.getValues(reusableMatrixValues)
                val imgToScreen = reusableMatrixValues[Matrix.MSCALE_X]
                val rImage = cvDetections.maxOfOrNull { it.radius } ?: 30f
                val rScreen = rImage * imgToScreen * 1.5f
                rScreen * rScreen
            }
            val cvFillIns = cvScreenBalls.filter { cvBall ->
                mlBalls.none { mlBall ->
                    val dx = (mlBall.position.x - cvBall.position.x).toDouble()
                    val dy = (mlBall.position.y - cvBall.position.y).toDouble()
                    (dx * dx + dy * dy) < mergeDistanceSq
                }
            }

            val allStructuredBalls = mlBalls + cvFillIns
            // genericBalls must use the same coord system as `balls`. Previously this
            // path emitted screen-pixel points, which mismatched consumer reducers
            // (ObstacleReducer, GestureReducer, SnapReducer) that compare against
            // event.logicalPoint. Use the structured positions, which are logical
            // when a pose exists and screen otherwise — consistent both ways.
            val genericBalls = allStructuredBalls.map { it.position }

            var finalVisionData = VisionData(
                genericBalls = genericBalls,
                balls = allStructuredBalls,
                detectedHsvColor = hsvTuple?.first ?: hsv,
                detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                detectedCues = detectedCues,
                cvMask = cvMask,
                sourceImageWidth = inputImage.width,
                sourceImageHeight = inputImage.height,
                sourceImageRotation = rotationDegrees
            )

            if (hsvTuple != null) {
                finalVisionData = finalVisionData.copy(
                    detectedHsvColor = hsvTuple.first,
                )
            }

            var currentConfidence = state.visionData?.tableOverlayConfidence ?: 0f
            if ((state.cameraMode == CameraMode.AR_ACTIVE || state.cameraMode == CameraMode.AR_SETUP) &&
                state.tableScanModel != null && state.depthCapability == DepthCapability.NONE) {
                arFrameCounter++
                if (arFrameCounter % 5 == 0) {
                    currentConfidence = runArTrackingPass(matToUse, state, inputImage.width, inputImage.height, rotationDegrees)
                }
            }

            finalVisionData = finalVisionData.copy(tableOverlayConfidence = currentConfidence)
            _visionDataFlow.value = finalVisionData

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            originalMat.release()
            // scaledBitmap is the pooled bitmap — do NOT recycle; it is reused next frame.
            imageProxy?.close()
            isProcessing.set(false)
        }
    }

    private fun refineBallCenter(
        detectedObject: DetectedObject,
        frame: Mat,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): PointF? {
        val box = detectedObject.boundingBox
        val roi = OCVRect(box.left, box.top, box.width(), box.height())

        if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > frame.cols() || roi.y + roi.height > frame.rows()) {
            return null
        }

        val expectedRadiusInImageCoords = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
        val tolerance = 0.5f
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        val roiMat = frame.submat(roi)
        Imgproc.morphologyEx(roiMat, roiMat, Imgproc.MORPH_OPEN, reusableMorphKernel)

        val refinedCenterInRoi = findBallByContour(
            roiMat, minRadius, maxRadius,
            state.cannyThreshold1.toDouble(), state.cannyThreshold2.toDouble()
        )

        roiMat.release()
        return refinedCenterInRoi?.let { PointF(it.x + roi.x, it.y + roi.y) }
    }

    private fun getExpectedRadiusAtImageY(
        imageY: Float,
        state: CueDetatState,
        imageToScreenMatrix: Matrix
    ): Float {
        val pitchMatrix = state.pitchMatrix
        if (!state.hasInverseMatrix || pitchMatrix == null) return LOGICAL_BALL_RADIUS

        reusablePointArray[0] = 0f
        reusablePointArray[1] = imageY
        imageToScreenMatrix.mapPoints(reusablePointArray)
        val screenY = reusablePointArray[1]

        val logicalTopY = if (state.table.isVisible) -state.table.logicalHeight / 2f else -200f
        val logicalBottomY = if (state.table.isVisible) state.table.logicalHeight / 2f else 200f

        val logicalTop = PointF(0f, logicalTopY)
        val logicalBottom = PointF(0f, logicalBottomY)

        val screenTopInfo = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalTop, LOGICAL_BALL_RADIUS, state, pitchMatrix
        )
        val screenBottomInfo = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalBottom, LOGICAL_BALL_RADIUS, state, pitchMatrix
        )

        val screenTopY = DrawingUtils.mapPoint(logicalTop, pitchMatrix).y
        val screenBottomY = DrawingUtils.mapPoint(logicalBottom, pitchMatrix).y

        val rangeY = screenBottomY - screenTopY
        if (abs(rangeY) < 1f) return screenBottomInfo.radius

        val fraction = ((screenY - screenTopY) / rangeY).coerceIn(0f, 1f)
        val interpolatedRadius = screenTopInfo.radius + fraction * (screenBottomInfo.radius - screenTopInfo.radius)

        imageToScreenMatrix.getValues(reusableMatrixValues)
        val scaleY = reusableMatrixValues[Matrix.MSCALE_Y]

        return if (scaleY > 0) interpolatedRadius / scaleY else LOGICAL_BALL_RADIUS
    }

    private fun findBallByContour(
        roiMat: Mat,
        minRadius: Float,
        maxRadius: Float,
        cannyT1: Double,
        cannyT2: Double
    ): PointF? {
        Imgproc.cvtColor(roiMat, reusableGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(reusableGray, reusableGray, Size(5.0, 5.0), 2.0, 2.0)
        Imgproc.Canny(reusableGray, reusableEdges, cannyT1, cannyT2)

        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            reusableEdges, contours, reusableHierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        var bestCenter: PointF? = null
        if (contours.isNotEmpty()) {
            val allPoints = MatOfPoint()
            Core.vconcat(contours as List<Mat>, allPoints)
            val allPoints2f = MatOfPoint2f(*allPoints.toArray())
            val centerArray = org.opencv.core.Point()
            val radiusArray = FloatArray(1)

            Imgproc.minEnclosingCircle(allPoints2f, centerArray, radiusArray)
            val radius = radiusArray[0]

            if (radius > minRadius && radius < maxRadius) {
                bestCenter = PointF(centerArray.x.toFloat(), centerArray.y.toFloat())
            }
            allPoints.release()
            allPoints2f.release()
        }

        contours.forEach { it.release() }
        return bestCenter
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun processArCpuImage(image: MediaImage, rotationDegrees: Int, state: CueDetatState) {
        if (isProcessing.get()) {
            return
        }
        isProcessing.set(true)

        var scaledBitmap: android.graphics.Bitmap? = null
        var fullBitmap: android.graphics.Bitmap? = null
        val originalMat = Mat()
        val rgbaMat = Mat()

        try {
            // Downsample the AR silicon torture chamber.
            fullBitmap = image.toBitmap() ?: return
            val scaledWidth = (fullBitmap.width / 4).coerceAtLeast(1)
            val scaledHeight = (fullBitmap.height / 4).coerceAtLeast(1)
            scaledBitmap = android.graphics.Bitmap.createScaledBitmap(fullBitmap, scaledWidth, scaledHeight, false)
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(scaledBitmap, rotationDegrees)

            org.opencv.android.Utils.bitmapToMat(scaledBitmap, rgbaMat)
            Imgproc.cvtColor(rgbaMat, originalMat, Imgproc.COLOR_RGBA2BGR)

            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            val detectedObjects = com.google.android.gms.tasks.Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detectPool(scaledBitmap)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }

            var matToUse: Mat = originalMat
            when (rotationDegrees) {
                90 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE); matToUse = reusableRotatedMat }
                180 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_180); matToUse = reusableRotatedMat }
                270 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE); matToUse = reusableRotatedMat }
            }

            Imgproc.cvtColor(matToUse, reusableHsvMat, Imgproc.COLOR_BGR2HSV)
            val hsvMat = reusableHsvMat
            val autoFelt = if (state.lockedHsvColor == null) {
                feltColorDetector.detect(hsvMat)?.also { lastFeltDetection = it }
            } else null
            val hsv = state.lockedHsvColor ?: autoFelt?.hsv ?: run {
                val roiX = (hsvMat.cols() - 50) / 2
                val roiY = (hsvMat.rows() - 50) / 2
                val center = hsvMat.submat(OCVRect(roiX, roiY, 50, 50))
                val mean = Core.mean(center)
                center.release()
                floatArrayOf(mean.`val`[0].toFloat(), mean.`val`[1].toFloat(), mean.`val`[2].toFloat())
            }

            val hasTablePose = state.hasInverseMatrix && state.pitchMatrix != null
            val filteredObjects = detectedObjects.filter {
                val box = it.boundingBox
                if (!hasTablePose) {
                    val side = minOf(box.width(), box.height())
                    side in 10..400
                } else {
                    val er = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
                    (box.width() * box.height()) <= 2 * Math.PI * er * er
                }
            }

            val refinedScreenPoints = filteredObjects.map { obj ->
                val box = obj.boundingBox
                val fallback = PointF(box.exactCenterX(), box.exactCenterY())
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix) ?: fallback
            }.map { pt ->
                val arr = floatArrayOf(pt.x, pt.y)
                imageToScreenMatrix.mapPoints(arr)
                android.graphics.PointF(arr[0], arr[1])
            }

            val logicalPoints = if (state.hasInverseMatrix) {
                val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                val tps = state.lensWarpTps
                refinedScreenPoints.map { sp ->
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (tps != null) com.hereliesaz.cuedetat.domain.ThinPlateSpline.applyWarp(tps, lp) else lp
                }
            } else emptyList()

            val mlBalls = refinedScreenPoints.mapIndexed { idx, sp ->
                val logical = if (state.hasInverseMatrix) {
                    val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (state.lensWarpTps != null) com.hereliesaz.cuedetat.domain.ThinPlateSpline.applyWarp(state.lensWarpTps, lp) else lp
                } else sp
                val box = filteredObjects[idx].boundingBox
                val ballType = classifyBallType(matToUse, box)
                com.hereliesaz.cuedetat.data.DetectedBall(position = logical, type = ballType, confidence = 0.9f, boundingBox = box)
            }

            val feltMean = state.lockedHsvColor ?: autoFelt?.hsv ?: lastFeltDetection?.hsv
            val feltSd = state.lockedHsvStdDev
                ?: autoFelt?.stdDev
                ?: lastFeltDetection?.stdDev
                ?: floatArrayOf(8f, 40f, 50f)
            val cvDetections = if (feltMean != null) {
                val sides = filteredObjects.map { minOf(it.boundingBox.width(), it.boundingBox.height()) }
                    .filter { it > 0 }
                val medianSide = if (sides.isNotEmpty()) sides.sorted()[sides.size / 2].toFloat() else 0f
                cvBallDetector.detect(matToUse, hsvMat, feltMean, feltSd, medianSide / 2f)
            } else emptyList()

            val cvScreenBalls = cvDetections.map { d ->
                val arr = floatArrayOf(d.center.x, d.center.y)
                imageToScreenMatrix.mapPoints(arr)
                val sp = android.graphics.PointF(arr[0], arr[1])
                val logical = if (state.hasInverseMatrix) {
                    val inv = state.inversePitchMatrix ?: android.graphics.Matrix()
                    val lp = com.hereliesaz.cuedetat.view.model.Perspective.screenToLogical(sp, inv)
                    if (state.lensWarpTps != null) com.hereliesaz.cuedetat.domain.ThinPlateSpline.applyWarp(state.lensWarpTps, lp) else lp
                } else sp
                val box = android.graphics.Rect(
                    (d.center.x - d.radius).toInt(),
                    (d.center.y - d.radius).toInt(),
                    (d.center.x + d.radius).toInt(),
                    (d.center.y + d.radius).toInt(),
                )
                com.hereliesaz.cuedetat.data.DetectedBall(position = logical, type = d.type, confidence = d.confidence, boundingBox = box)
            }

            // See processImage for the unit-mismatch context: ball positions live in
            // logical inches when a pose exists, screen pixels otherwise. The AR path
            // always has a pose, so use logical units, with a screen-pixel fallback
            // for the degenerate case.
            val mergeDistSq = if (state.hasInverseMatrix) {
                val r = LOGICAL_BALL_RADIUS * 1.5f
                r * r
            } else {
                imageToScreenMatrix.getValues(reusableMatrixValues)
                val imgToScreen = reusableMatrixValues[Matrix.MSCALE_X]
                val rImage = cvDetections.maxOfOrNull { it.radius } ?: 30f
                val rScreen = rImage * imgToScreen * 1.5f
                rScreen * rScreen
            }
            val cvFillIns = cvScreenBalls.filter { cvBall ->
                mlBalls.none { mlBall ->
                    val dx = (mlBall.position.x - cvBall.position.x).toDouble()
                    val dy = (mlBall.position.y - cvBall.position.y).toDouble()
                    (dx * dx + dy * dy) < mergeDistSq
                }
            }

            val allBalls = mlBalls + cvFillIns
            val cvFillInPositions = cvFillIns.map { it.position }
            val filteredBalls = if (state.table.isVisible) {
                (logicalPoints + cvFillInPositions).filter { state.table.isPointInside(it) }
            } else {
                logicalPoints + cvFillInPositions
            }

            _visionDataFlow.value = VisionData(
                genericBalls = filteredBalls,
                balls = allBalls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = filteredObjects.map { it.boundingBox },
                sourceImageWidth = inputImage.width,
                sourceImageHeight = inputImage.height,
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
                    currentConfidence = runArTrackingPass(matToUse, state, inputImage.width, inputImage.height, rotationDegrees)
                }
            }
            _visionDataFlow.value = _visionDataFlow.value.copy(tableOverlayConfidence = currentConfidence)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            rgbaMat.release()
            originalMat.release()
            // fullBitmap is the cached arOutputBitmap, do not recycle.
            scaledBitmap?.let { if (it !== fullBitmap) it.recycle() }
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

    private var arOutputBitmap: android.graphics.Bitmap? = null
    private var arPixelBuffer: IntArray? = null

    private fun android.media.Image.toBitmap(): android.graphics.Bitmap? {
        if (format != android.graphics.ImageFormat.YUV_420_888) return null

        val w = width
        val h = height
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        if (arPixelBuffer?.size != w * h) {
            arPixelBuffer = IntArray(w * h)
        }
        val pixels = arPixelBuffer!!

        for (row in 0 until h) {
            for (col in 0 until w) {
                val y = yBuf.get(row * yRowStride + col).toInt() and 0xFF
                val uvRow = row / 2
                val uvCol = col / 2
                val uvIdx = uvRow * uvRowStride + uvCol * uvPixelStride
                val u = (uBuf.get(uvIdx).toInt() and 0xFF) - 128
                val v = (vBuf.get(uvIdx).toInt() and 0xFF) - 128

                val r = (y + 1.370705f * v).toInt().coerceIn(0, 255)
                val g = (y - 0.698001f * v - 0.337633f * u).toInt().coerceIn(0, 255)
                val b = (y + 1.732446f * u).toInt().coerceIn(0, 255)
                pixels[row * w + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val bmp = arOutputBitmap?.takeIn(w, h)
            ?: android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888).also { arOutputBitmap = it }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun android.graphics.Bitmap.takeIn(w: Int, h: Int): android.graphics.Bitmap? {
        return if (width == w && height == h) this else null
    }

    private fun refineBallCenterPoolDetection(
        detectedObject: com.hereliesaz.cuedetat.data.PoolDetection,
        frame: org.opencv.core.Mat,
        state: CueDetatState,
        imageToScreenMatrix: android.graphics.Matrix
    ): android.graphics.PointF? {
        val box = detectedObject.rect
        val roi = OCVRect(box.left.toInt(), box.top.toInt(), box.width().toInt(), box.height().toInt())

        if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > frame.cols() || roi.y + roi.height > frame.rows()) {
            return null
        }

        val expectedRadiusInImageCoords = getExpectedRadiusAtImageY(box.centerY(), state, imageToScreenMatrix)
        val tolerance = 0.5f
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        val roiMat = frame.submat(roi)
        org.opencv.imgproc.Imgproc.morphologyEx(roiMat, roiMat, org.opencv.imgproc.Imgproc.MORPH_OPEN, reusableMorphKernel)

        val refinedCenterInRoi = findBallByContour(
            roiMat, minRadius, maxRadius,
            state.cannyThreshold1.toDouble(), state.cannyThreshold2.toDouble()
        )

        roiMat.release()
        return refinedCenterInRoi?.let { android.graphics.PointF(it.x.toFloat() + roi.x, it.y.toFloat() + roi.y) }
    }

    private fun classifyBallType(frame: Mat, box: android.graphics.Rect): BallType {
        val x = box.left.coerceIn(0, frame.cols() - 1)
        val y = box.top.coerceIn(0, frame.rows() - 1)
        val w = box.width().coerceIn(1, frame.cols() - x)
        val h = box.height().coerceIn(1, frame.rows() - y)

        val roi = OCVRect(x, y, w, h)
        val ballMat = frame.submat(roi)

        val hsvMat = Mat()
        Imgproc.cvtColor(ballMat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // Sample the ball's middle band (avoids glare/shadow at the poles) for
        // the whole-ball mean used to spot the cue and the 8 ball. A pure-white
        // cue ball reads as low saturation + high value across the entire ball;
        // a pure-black 8 ball reads as low value across the entire ball. Stripes
        // and solids both have a coloured band in the middle so they fall through.
        val midTop = (h * 0.30f).toInt().coerceIn(0, h - 1)
        val midH = (h * 0.40f).toInt().coerceAtLeast(1).coerceAtMost(h - midTop)
        val midHsv = hsvMat.submat(OCVRect(0, midTop, w, midH))
        val midMean = Core.mean(midHsv)
        midHsv.release()

        val isWhole8Ball = midMean.`val`[2] < 60.0
        val isWholeCueBall = midMean.`val`[1] < 40.0 && midMean.`val`[2] > 190.0

        if (isWhole8Ball) {
            ballMat.release()
            hsvMat.release()
            return BallType.EIGHT
        }
        if (isWholeCueBall) {
            ballMat.release()
            hsvMat.release()
            return BallType.CUE
        }

        val poleHeight = (h * 0.15f).toInt().coerceAtLeast(1)
        val topHsv = hsvMat.submat(OCVRect(0, 0, w, poleHeight))
        val bottomHsv = hsvMat.submat(OCVRect(0, h - poleHeight, w, poleHeight))

        val topMean = Core.mean(topHsv)
        val bottomMean = Core.mean(bottomHsv)

        topHsv.release()
        bottomHsv.release()
        ballMat.release()
        hsvMat.release()

        val isTopWhite = topMean.`val`[1] < 50.0 && topMean.`val`[2] > 180.0
        val isBottomWhite = bottomMean.`val`[1] < 50.0 && bottomMean.`val`[2] > 180.0

        return if (isTopWhite && isBottomWhite) BallType.STRIPE else BallType.SOLID
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