// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt

package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.utils.toMat
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.CvRefinementMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import kotlin.math.pow
import org.opencv.core.Rect as OCVRect

@Singleton
class VisionRepository @Inject constructor(
    @GenericDetector private val genericObjectDetector: ObjectDetector,
) {

    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    private var lastFrameTime = 0L
    private var reusableMask: Mat? = null

    // Reusable Mats to avoid allocation churn
    private val reusableFrameMat = Mat()
    private val reusableRotatedMat = Mat()
    private val reusableHsvMat = Mat()

    // Reusable Mats for refineBallCenter and other helpers
    private val reusableGray = Mat()
    private val reusableEdges = Mat()
    private val reusableHierarchy = Mat()
    private val reusableCircles = Mat()

    // Reusable arrays for calculations
    private val reusableMatrixValues = FloatArray(9)
    private val reusablePointArray = FloatArray(2)

    // Reusable Mats for HSV sampling
    private val reusableMean = MatOfDouble()
    private val reusableStdDev = MatOfDouble()

    // Lazily initialized kernel
    private val reusableMorphKernel by lazy {
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, state: CueDetatState) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < 33) { // ~30 FPS cap
            imageProxy.close()
            return
        }
        lastFrameTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage =
                InputImage.fromMediaImage(mediaImage, rotationDegrees)
            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            // Execute on the current (background) thread instead of using async callbacks
            // which default to the Main Thread. This prevents blocking the UI.
            try {
                val detectedObjects = Tasks.await(genericObjectDetector.process(inputImage))

                // Reuse reusableFrameMat for initial conversion
                val originalMat = imageProxy.toMat(reusableFrameMat)

                // Reuse reusableRotatedMat if needed
                var matToUse: Mat

                when (rotationDegrees) {
                    90 -> {
                        Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_CLOCKWISE)
                        matToUse = reusableRotatedMat
                    }

                    180 -> {
                        Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_180)
                        matToUse = reusableRotatedMat
                    }

                    270 -> {
                        Core.rotate(originalMat, reusableRotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                        matToUse = reusableRotatedMat
                    }

                    else -> {
                        matToUse = originalMat
                    }
                }

                // Note: We do NOT release originalMat here because it is reusableFrameMat (or points to it)

                // Reuse reusableHsvMat
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
                    // reusableMean and reusableStdDev are reused, no release needed

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
                        val lowerBound =
                            Scalar((hsv[0] - hueRange).coerceAtLeast(0.0), 100.0, 100.0)
                        val upperBound =
                            Scalar((hsv[0] + hueRange).coerceAtMost(180.0), 255.0, 255.0)
                        Core.inRange(hsvMat, lowerBound, upperBound, mask)
                    }
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, reusableMorphKernel)

                    // Clone the mask because it is being updated on a background thread
                    // while the UI might read it on the main thread.
                    mask.clone()
                } else {
                    reusableMask?.release()
                    reusableMask = null
                    null
                }

                val filteredDetectedObjects = detectedObjects.filter {
                    val box = it.boundingBox
                    val expectedRadius = getExpectedRadiusAtImageY(
                        box.centerY().toFloat(),
                        state,
                        imageToScreenMatrix
                    )
                    val maxAllowedArea = 2 * Math.PI * expectedRadius.pow(2)
                    (box.width() * box.height()) <= maxAllowedArea
                }

                val refinedScreenPoints = filteredDetectedObjects.mapNotNull { detectedObject ->
                    refineBallCenter(detectedObject, matToUse, state, imageToScreenMatrix)
                }.map { pointInImageCoords ->
                    val screenPointArray =
                        floatArrayOf(pointInImageCoords.x, pointInImageCoords.y)
                    imageToScreenMatrix.mapPoints(screenPointArray)
                    PointF(screenPointArray[0], screenPointArray[1])
                }

                val detectedLogicalPoints = if (state.hasInverseMatrix) {
                    val inverseMatrix = state.inversePitchMatrix ?: Matrix()
                    refinedScreenPoints.map { screenPoint ->
                        Perspective.screenToLogical(screenPoint, inverseMatrix)
                    }
                } else {
                    emptyList()
                }

                val filteredBalls = if (state.table.isVisible) {
                    detectedLogicalPoints.filter { state.table.isPointInside(it) }
                } else {
                    detectedLogicalPoints
                }

                var finalVisionData = VisionData(
                    genericBalls = filteredBalls,
                    detectedHsvColor = hsvTuple?.first ?: hsv,
                    detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
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

                _visionDataFlow.value = finalVisionData

                // Do NOT release matToUse or hsvMat as they are reusable

            } catch (e: Exception) {
                // Log exception if needed
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

        val expectedRadiusInImageCoords =
            getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
        val tolerance = 0.5f
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        val roiMat = frame.submat(roi)

        Imgproc.morphologyEx(roiMat, roiMat, Imgproc.MORPH_OPEN, reusableMorphKernel)

        val refinedCenterInRoi = when (state.cvRefinementMethod) {
            CvRefinementMethod.CONTOUR -> findBallByContour(
                roiMat,
                minRadius,
                maxRadius,
                state.cannyThreshold1.toDouble(),
                state.cannyThreshold2.toDouble()
            )
            CvRefinementMethod.HOUGH -> findBallByHough(
                roiMat,
                minRadius,
                maxRadius,
                state.houghP1.toDouble(),
                state.houghP2.toDouble()
            )
        }

        roiMat.release()

        return refinedCenterInRoi?.let {
            PointF(it.x + roi.x, it.y + roi.y)
        }
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
            logicalTop,
            LOGICAL_BALL_RADIUS,
            state,
            pitchMatrix
        )
        val screenBottomInfo = DrawingUtils.getPerspectiveRadiusAndLift(
            logicalBottom,
            LOGICAL_BALL_RADIUS,
            state,
            pitchMatrix
        )
        val screenTopY = DrawingUtils.mapPoint(logicalTop, pitchMatrix).y
        val screenBottomY = DrawingUtils.mapPoint(logicalBottom, pitchMatrix).y

        val rangeY = screenBottomY - screenTopY
        if (abs(rangeY) < 1f) return screenBottomInfo.radius

        val fraction = ((screenY - screenTopY) / rangeY).coerceIn(0f, 1f)
        val interpolatedRadius =
            screenTopInfo.radius + fraction * (screenBottomInfo.radius - screenTopInfo.radius)

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
        // reusableHierarchy is reused, no need to allocate
        Imgproc.findContours(
            reusableEdges,
            contours,
            reusableHierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
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

        // Release the individual contours created by findContours
        contours.forEach { it.release() }
        // Do not release reusableGray, reusableEdges, reusableHierarchy

        return bestCenter
    }

    private fun findBallByHough(
        roiMat: Mat,
        minRadius: Float,
        maxRadius: Float,
        houghP1: Double,
        houghP2: Double
    ): PointF? {
        Imgproc.cvtColor(roiMat, reusableGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(reusableGray, reusableGray, 5)

        // reusableCircles is reused
        Imgproc.HoughCircles(
            reusableGray,
            reusableCircles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            roiMat.rows().toDouble() / 8,
            houghP1,
            houghP2,
            minRadius.toInt(),
            maxRadius.toInt()
        )

        var center: PointF? = null
        if (reusableCircles.cols() > 0) {
            val circleData = reusableCircles[0, 0]
            if (circleData != null && circleData.isNotEmpty()) {
                center = PointF(circleData[0].toFloat(), circleData[1].toFloat())
            }
        }

        // Do not release reusableGray, reusableCircles
        return center
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
}