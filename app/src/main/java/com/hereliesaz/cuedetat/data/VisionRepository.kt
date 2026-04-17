// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt
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
import com.hereliesaz.cuedetat.domain.PocketId
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
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import org.opencv.core.Rect as OCVRect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class VisionRepository @Inject constructor(
    @GenericDetector private val genericObjectDetector: ObjectDetector,
    private val poolDetector: MergedTFLiteDetector,
    private val myriadRepository: MyriadRepository
) {
    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    private var arFrameCounter = 0
    private val _arEvents = MutableSharedFlow<MainScreenEvent>(extraBufferCapacity = 16)
    val arEvents: SharedFlow<MainScreenEvent> = _arEvents.asSharedFlow()

    private var lastFrameTime = 0L
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

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, bitmap: android.graphics.Bitmap, state: CueDetatState) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < 100) {
            imageProxy.close()
            return
        }
        lastFrameTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            try {
                val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detectPool(bitmap)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }
                val originalMat = imageProxy.toMat(reusableFrameMat)

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
                    val roiX = (imageX - patchSize / 2).coerceIn(0, hsvMat.cols() - patchSize)
                    val roiY = (imageY - patchSize / 2).coerceIn(0, hsvMat.rows() - patchSize)
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

                val hsv = state.lockedHsvColor ?: hsvTuple?.first ?: run {
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

                val filteredDetectedObjects = detectedObjects.filter {
                    val box = it.boundingBox
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY().toFloat(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val refinedScreenPoints = filteredDetectedObjects.mapNotNull { detectedObject ->
                    refineBallCenter(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }
                
                val filteredCustomPoolBalls = customPoolBalls.filter {
                    val box = it.rect
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY(), state, imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val customScreenPoints = filteredCustomPoolBalls.mapNotNull { detectedObject ->
                    refineBallCenterPoolDetection(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray = floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }

                val detectedLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    refinedScreenPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else { emptyList() }
                
                val customLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    val tps = state.lensWarpTps
                    customScreenPoints.map { screenPoint ->
                        val logical = Perspective.screenToLogical(screenPoint, inverseMatrix)
                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                    }
                } else { emptyList() }
                
                // MYRIAD PREDICTION: Automatically track physical cue stick
                if (detectedCues.isNotEmpty() && filteredCustomPoolBalls.isNotEmpty()) {
                    if (currentTime - lastMyriadTime > 2000L) {
                        lastMyriadTime = currentTime
                        
                        // We use the first cue and the first pool ball to assemble a poke vector.
                        val cueRect = detectedCues.first()
                        val poolBall = filteredCustomPoolBalls.first()
                        
                        val cueCenter = PointF(cueRect.exactCenterX(), cueRect.exactCenterY())
                        val ballCenter = PointF(poolBall.rect.exactCenterX(), poolBall.rect.exactCenterY())
                        
                        val pokeDx = ballCenter.x - cueCenter.x
                        val pokeDy = ballCenter.y - cueCenter.y
                        
                        val currentMatrix = Matrix(imageToScreenMatrix)
                        val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                        val tps = state.lensWarpTps
                        val hasInverse = state.hasInverseMatrix
                        
                        // Make a copy of bitmap for network call
                        val bmpCopy = bitmap.copy(bitmap.config, false)
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = myriadRepository.fetchTrajectory(bmpCopy, ballCenter, PointF(pokeDx, pokeDy))
                            result.onSuccess { response ->
                                val mappedPoints = response.points.mapNotNull { pt ->
                                    val screenPoint = floatArrayOf(pt.x, pt.y)
                                    currentMatrix.mapPoints(screenPoint)
                                    val sp = PointF(screenPoint[0], screenPoint[1])
                                    if (hasInverse) {
                                        val logical = Perspective.screenToLogical(sp, inverseMatrix)
                                        if (tps != null) ThinPlateSpline.applyWarp(tps, logical) else logical
                                    } else sp
                                }
                                emitEvent(MainScreenEvent.MyriadTrajectoryReceived(mappedPoints))
                            }
                            bmpCopy.recycle()
                        }
                    }
                }

                val filteredBalls = if (state.table.isVisible) {
                    detectedLogicalPoints.filter { state.table.isPointInside(it) }
                } else { detectedLogicalPoints }
                
                val finalCustomBalls = if (state.table.isVisible) {
                    customLogicalPoints.filter { state.table.isPointInside(it) }
                } else { customLogicalPoints }

                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    customBalls = finalCustomBalls,
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
                imageProxy.close()
            }
        } else {
            imageProxy.close()
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
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < 100) return
        lastFrameTime = currentTime

        try {
            val imageToScreenMatrix = getTransformationMatrix(
                image.width, image.height,
                state.viewWidth, state.viewHeight
            )

            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(image, rotationDegrees)
            val bitmap = image.toBitmap() ?: return
            val detectedObjects = com.google.android.gms.tasks.Tasks.await(genericObjectDetector.process(inputImage))
            val rawDetections = poolDetector.detectPool(bitmap)
            val customPoolBalls = rawDetections.filter { it.classId == 1 }
            val detectedCues = rawDetections.filter { it.classId == 2 }.map {
                android.graphics.Rect(it.rect.left.toInt(), it.rect.top.toInt(), it.rect.right.toInt(), it.rect.bottom.toInt())
            }

            val originalMat = image.toMat(reusableFrameMat)
            var matToUse: Mat = originalMat

            when (rotationDegrees) {
                90 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE); matToUse = reusableRotatedMat }
                180 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_180); matToUse = reusableRotatedMat }
                270 -> { Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE); matToUse = reusableRotatedMat }
            }

            Imgproc.cvtColor(matToUse, reusableHsvMat, Imgproc.COLOR_BGR2HSV)
            val hsv = state.lockedHsvColor ?: run {
                val roiX = (reusableHsvMat.cols() - 50) / 2
                val roiY = (reusableHsvMat.rows() - 50) / 2
                val center = reusableHsvMat.submat(OCVRect(roiX, roiY, 50, 50))
                val mean = Core.mean(center)
                center.release()
                floatArrayOf(mean.`val`[0].toFloat(), mean.`val`[1].toFloat(), mean.`val`[2].toFloat())
            }

            val filteredObjects = detectedObjects.filter {
                val box = it.boundingBox
                val er = getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
                (box.width() * box.height()) <= 2 * Math.PI * er * er
            }

            val refinedScreenPoints = filteredObjects.mapNotNull { obj ->
                refineBallCenter(obj, matToUse, state, imageToScreenMatrix)
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

            val filteredBalls = if (state.table.isVisible) logicalPoints.filter { state.table.isPointInside(it) } else logicalPoints

            _visionDataFlow.value = VisionData(
                genericBalls = filteredBalls,
                detectedHsvColor = hsv,
                detectedBoundingBoxes = filteredObjects.map { it.boundingBox },
                sourceImageWidth = image.width,
                sourceImageHeight = image.height,
                sourceImageRotation = rotationDegrees,
            )

            val depth = state.depthPlane
            if (depth != null && depth.confidence > 0.6f && state.tableScanModel == null) {
                runDepthOnlyScaleUpdate(depth, state)
            }

        } catch (e: Exception) {
            e.printStackTrace()
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
        return runEdgeFallback(mat, state, imageWidth, imageHeight, rotationDegrees)
    }

    private fun runEdgeFallback(
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
        Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        mask.release()
        kernel.release()

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
        val alpha = 0.2f
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

        h.release()

        val delta = hypot((blendedTranslation.x - state.viewOffset.x).toDouble(), (blendedTranslation.y - state.viewOffset.y).toDouble())
        if (delta > 0.5) {
            emitEvent(MainScreenEvent.UpdateArPose(blendedTranslation, blendedRotation, blendedScale))
        }

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

        val pixels = IntArray(w * h)
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

        val bmp = arOutputBitmap?.takeIf { it.width == w && it.height == h }
            ?: android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888).also { arOutputBitmap = it }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
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
}
