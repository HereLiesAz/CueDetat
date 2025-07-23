// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt

package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.domain.LOGICAL_BALL_RADIUS
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.CvRefinementMethod
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
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

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, state: OverlayState) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < 33) { // ~30 FPS cap
            imageProxy.close()
            return
        }
        lastFrameTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val imageToScreenMatrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            genericObjectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    val mat = imageProxyToMat(imageProxy)
                    val hsvMat = Mat()
                    Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)

                    val hsvTuple = state.colorSamplePoint?.let {
                        val imageX = (it.x * (hsvMat.cols() / state.viewWidth.toFloat())).toInt()
                        val imageY = (it.y * (hsvMat.rows() / state.viewHeight.toFloat())).toInt()

                        val patchSize = 5
                        val roiX = (imageX - patchSize / 2).coerceIn(0, hsvMat.cols() - patchSize)
                        val roiY = (imageY - patchSize / 2).coerceIn(0, hsvMat.rows() - patchSize)
                        val roi = OCVRect(roiX, roiY, patchSize, patchSize)
                        val sampleRegion = hsvMat.submat(roi)

                        val mean = MatOfDouble()
                        val stddev = MatOfDouble()
                        Core.meanStdDev(sampleRegion, mean, stddev)

                        val meanArray = floatArrayOf(
                            mean.get(0, 0)[0].toFloat(),
                            mean.get(1, 0)[0].toFloat(),
                            mean.get(2, 0)[0].toFloat()
                        )
                        val stddevArray = floatArrayOf(
                            stddev.get(0, 0)[0].toFloat(),
                            stddev.get(1, 0)[0].toFloat(),
                            stddev.get(2, 0)[0].toFloat()
                        )

                        sampleRegion.release()
                        mean.release()
                        stddev.release()

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
                        val mask = Mat()
                        if (state.lockedHsvColor != null && state.lockedHsvStdDev != null) {
                            val mean = state.lockedHsvColor
                            val stdDev = state.lockedHsvStdDev
                            val stdDevMultiplier = 2.0 // Multiplier for range
                            val lowerBound = Scalar(
                                (mean[0] - stdDevMultiplier * stdDev[0]).coerceAtLeast(0.0),
                                (mean[1] - stdDevMultiplier * stdDev[1]).coerceAtLeast(40.0), // Min saturation
                                (mean[2] - stdDevMultiplier * stdDev[2]).coerceAtLeast(40.0)  // Min value/brightness
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
                        // Morphological closing to fill holes from specular highlights
                        val kernel =
                            Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
                        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
                        kernel.release()
                        mask
                    } else {
                        null
                    }

                    // --- FIX: Rotate the mask to match the display orientation ---
                    val finalCvMask = if (cvMask != null) {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        if (rotationDegrees == 0) {
                            cvMask
                        } else {
                            val rotatedMask = Mat()
                            val rotateCode = when (rotationDegrees) {
                                90 -> Core.ROTATE_90_CLOCKWISE
                                180 -> Core.ROTATE_180
                                270 -> Core.ROTATE_90_COUNTERCLOCKWISE
                                else -> -1 // Invalid, won't rotate
                            }
                            if (rotateCode != -1) {
                                Core.rotate(cvMask, rotatedMask, rotateCode)
                                cvMask.release() // Release the original un-rotated mask
                                rotatedMask      // The rotated mask is now the final one
                            } else {
                                cvMask // Fallback to the original if rotation code is invalid
                            }
                        }
                    } else {
                        null
                    }
                    // --- END FIX ---

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
                        refineBallCenter(detectedObject, mat, state, imageToScreenMatrix)
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

                    val finalVisionData = VisionData(
                        genericBalls = filteredBalls,
                        detectedHsvColor = hsvTuple?.first ?: hsv,
                        detectedBoundingBoxes = filteredDetectedObjects.map { it.boundingBox },
                        cvMask = finalCvMask,
                        sourceImageWidth = inputImage.width,
                        sourceImageHeight = inputImage.height
                    )

                    _visionDataFlow.value = finalVisionData
                    if (hsvTuple != null) {
                        _visionDataFlow.value = _visionDataFlow.value.copy(
                            detectedHsvColor = hsvTuple.first,
                        )
                    }


                    mat.release()
                    hsvMat.release()

                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun refineBallCenter(
        detectedObject: DetectedObject,
        frame: Mat,
        state: OverlayState,
        imageToScreenMatrix: Matrix
    ): PointF? {
        val box = detectedObject.boundingBox
        val roi = OCVRect(box.left, box.top, box.width(), box.height())

        if (roi.x < 0 || roi.y < 0 || roi.x + roi.width > frame.cols() || roi.y + roi.height > frame.rows()) {
            return null
        }

        val expectedRadiusInImageCoords =
            getExpectedRadiusAtImageY(box.centerY().toFloat(), state, imageToScreenMatrix)
        val tolerance = 0.5f // 50% tolerance
        val minRadius = expectedRadiusInImageCoords * (1 - tolerance)
        val maxRadius = expectedRadiusInImageCoords * (1 + tolerance)

        val roiMat = frame.submat(roi)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(roiMat, roiMat, Imgproc.MORPH_OPEN, kernel)
        kernel.release()

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
        state: OverlayState,
        imageToScreenMatrix: Matrix
    ): Float {
        val pitchMatrix = state.pitchMatrix
        if (!state.hasInverseMatrix || pitchMatrix == null) return LOGICAL_BALL_RADIUS // A fallback

        // 1. Convert image Y to screen Y
        val point = floatArrayOf(0f, imageY)
        imageToScreenMatrix.mapPoints(point)
        val screenY = point[1]

        // 2. Define logical points for interpolation range
        val logicalTopY = if (state.table.isVisible) -state.table.logicalHeight / 2f else -200f
        val logicalBottomY = if (state.table.isVisible) state.table.logicalHeight / 2f else 200f
        val logicalTop = PointF(0f, logicalTopY)
        val logicalBottom = PointF(0f, logicalBottomY)

        // 3. Get screen space info for these points
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

        // 4. Interpolate to find expected screen radius at the given screen Y
        val rangeY = screenBottomY - screenTopY
        if (abs(rangeY) < 1f) return screenBottomInfo.radius

        val fraction = ((screenY - screenTopY) / rangeY).coerceIn(0f, 1f)
        val interpolatedRadius =
            screenTopInfo.radius + fraction * (screenBottomInfo.radius - screenTopInfo.radius)

        // 5. Convert expected screen radius back to image coordinate space radius
        val values = FloatArray(9)
        imageToScreenMatrix.getValues(values)
        val scaleY = values[Matrix.MSCALE_Y]

        return if (scaleY > 0) interpolatedRadius / scaleY else LOGICAL_BALL_RADIUS
    }

    private fun findBallByContour(
        roiMat: Mat,
        minRadius: Float,
        maxRadius: Float,
        cannyT1: Double,
        cannyT2: Double
    ): PointF? {
        val gray = Mat()
        Imgproc.cvtColor(roiMat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 2.0, 2.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, cannyT1, cannyT2)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges,
            contours,
            hierarchy,
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

        contours.forEach { it.release() }
        gray.release()
        edges.release()
        hierarchy.release()

        return bestCenter
    }

    private fun findBallByHough(
        roiMat: Mat,
        minRadius: Float,
        maxRadius: Float,
        houghP1: Double,
        houghP2: Double
    ): PointF? {
        val gray = Mat()
        Imgproc.cvtColor(roiMat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(gray, gray, 5) // Hough is sensitive to noise

        val circles = Mat()
        Imgproc.HoughCircles(
            gray,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0, // dp: inverse ratio of accumulator resolution
            roiMat.rows().toDouble() / 8, // minDist
            houghP1, // param1: Canny higher threshold
            houghP2, // param2: accumulator threshold
            minRadius.toInt(),
            maxRadius.toInt()
        )

        var center: PointF? = null
        if (circles.cols() > 0) {
            val circleData = circles[0, 0]
            if (circleData != null && circleData.isNotEmpty()) {
                center = PointF(circleData[0].toFloat(), circleData[1].toFloat())
            }
        }

        gray.release()
        circles.release()
        return center
    }

    /**
     * Converts a YUV_420_888 ImageProxy to a BGR Mat.
     *
     * This function is optimized to avoid intermediate byte array copies for
     * the common NV21 format by creating Mat headers that point directly to
     * the image plane buffers.
     *
     * @param imageProxy The ImageProxy from CameraX.
     * @return A BGR Mat.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun imageProxyToMat(imageProxy: ImageProxy): Mat {
        val image = imageProxy.image ?: return Mat()
        val planes = image.planes
        val width = image.width
        val height = image.height

        val bgrMat = Mat()

        // Y plane
        val yPlane = planes[0]
        val yBuffer = yPlane.buffer
        val yMat = Mat(height, width, CvType.CV_8UC1, yBuffer, yPlane.rowStride.toLong())

        // U and V planes. In YUV_420_888, the U/V planes might be interleaved (NV21) or planar (I420).
        val uPlane = planes[1]
        val vPlane = planes[2]

        // Check for NV21 format (interleaved UV plane). This is the most common format from CameraX.
        // In NV21, the V plane's buffer is the one that contains the interleaved VU data.
        if (vPlane.pixelStride == 2) {
            val uvBuffer = vPlane.buffer
            val uvMat =
                Mat(height / 2, width / 2, CvType.CV_8UC2, uvBuffer, vPlane.rowStride.toLong())
            Imgproc.cvtColorTwoPlane(yMat, uvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21)
            uvMat.release()
        } else {
            // Fallback for planar formats like I420. This path is less efficient as it requires a copy.
            val ySize = yPlane.buffer.remaining()
            val uSize = uPlane.buffer.remaining()
            val vSize = vPlane.buffer.remaining()
            val data = ByteArray(ySize + uSize + vSize)

            // Order for I420 is Y, then U, then V.
            yPlane.buffer.get(data, 0, ySize)
            uPlane.buffer.get(data, ySize, uSize)
            vPlane.buffer.get(data, ySize + uSize, vSize)

            val yuvI420Mat = Mat(height + height / 2, width, CvType.CV_8UC1)
            yuvI420Mat.put(0, 0, data)
            Imgproc.cvtColor(yuvI420Mat, bgrMat, Imgproc.COLOR_YUV2BGR_I420)
            yuvI420Mat.release()
        }

        yMat.release()
        return bgrMat
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