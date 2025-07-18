package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.renderer.util.DrawingUtils
import com.hereliesaz.cuedetat.view.state.CvRefinementMethod
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.pow
import org.opencv.core.Point as OCVPoint
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

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val transformationMatrix = getTransformationMatrix(
            inputImage.width, inputImage.height,
            state.viewWidth, state.viewHeight
        )

        genericObjectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val mat = imageToMat(inputImage)
                val hsvMat = Mat()
                Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)

                val (hsv, mask) = processColorAndMask(hsvMat, state)
                val refinedBalls = refineDetections(detectedObjects, transformationMatrix, state, mat)

                val logicalBalls = if (state.hasInverseMatrix) {
                    refinedBalls.map { screenPoint ->
                        Perspective.screenToLogical(screenPoint, state.inversePitchMatrix)
                    }
                } else {
                    emptyList()
                }

                val finalBalls = if (state.table.isVisible) {
                    logicalBalls.filter { state.table.isPointInside(it) }
                } else {
                    logicalBalls
                }

                _visionDataFlow.value = VisionData(
                    genericBalls = finalBalls,
                    detectedHsvColor = hsv,
                    detectedBoundingBoxes = detectedObjects.map { it.boundingBox },
                    cvMask = if (state.showCvMask) mask else null
                )

                mat.release()
                hsvMat.release()
                mask?.release()

                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    }

    private fun processColorAndMask(hsvMat: Mat, state: OverlayState): Pair<FloatArray, Mat?> {
        val hsv: FloatArray
        val mask: Mat?

        if (state.lockedHsvColor != null) {
            hsv = state.lockedHsvColor
        } else if (state.colorSamplePoint != null) {
            val imageX = (state.colorSamplePoint.x * (hsvMat.cols() / state.viewWidth.toFloat())).toInt()
            val imageY = (state.colorSamplePoint.y * (hsvMat.rows() / state.viewHeight.toFloat())).toInt()
            val sampleRegion = OCVRect(
                (imageX - 2).coerceIn(0, hsvMat.cols() - 5),
                (imageY - 2).coerceIn(0, hsvMat.rows() - 5),
                5, 5
            )
            val patch = hsvMat.submat(sampleRegion)
            val mean = MatOfDouble()
            val stdDev = MatOfDouble()
            Core.meanStdDev(patch, mean, stdDev)

            val meanValues = mean.toArray()
            val stdDevValues = stdDev.toArray()

            hsv = if (meanValues.size >= 3 && stdDevValues.size >= 3) {
                floatArrayOf(
                    meanValues[0].toFloat(), stdDevValues[0].toFloat(),
                    meanValues[1].toFloat(), stdDevValues[1].toFloat(),
                    meanValues[2].toFloat(), stdDevValues[2].toFloat()
                )
            } else {
                floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
            }

            patch.release()
            mean.release()
            stdDev.release()
        } else {
            hsv = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
        }

        mask = if (state.showCvMask && hsv.size >= 6) {
            val m = Mat()
            val multiplier = state.cvHsvRangeMultiplier
            val lowerBound = Scalar(
                (hsv[0] - hsv[1] * multiplier).toDouble().coerceAtLeast(0.0),
                (hsv[2] - hsv[3] * multiplier).toDouble().coerceAtLeast(0.0),
                (hsv[4] - hsv[5] * multiplier).toDouble().coerceAtLeast(0.0)
            )
            val upperBound = Scalar(
                (hsv[0] + hsv[1] * multiplier).toDouble().coerceAtMost(180.0),
                (hsv[2] + hsv[3] * multiplier).toDouble().coerceAtMost(255.0),
                (hsv[4] + hsv[5] * multiplier).toDouble().coerceAtMost(255.0)
            )
            Core.inRange(hsvMat, lowerBound, upperBound, m)
            m
        } else {
            null
        }

        return Pair(hsv, mask)
    }

    private fun refineDetections(
        detectedObjects: List<DetectedObject>,
        matrix: Matrix,
        state: OverlayState,
        fullImageMat: Mat
    ): List<PointF> {
        return detectedObjects.mapNotNull { obj ->
            val screenRect = RectF(obj.boundingBox)
            matrix.mapRect(screenRect)

            val roi = OCVRect(
                obj.boundingBox.left.coerceAtLeast(0),
                obj.boundingBox.top.coerceAtLeast(0),
                obj.boundingBox.width().coerceAtMost(fullImageMat.cols() - obj.boundingBox.left),
                obj.boundingBox.height().coerceAtMost(fullImageMat.rows() - obj.boundingBox.top)
            )
            if (roi.width <= 0 || roi.height <= 0) return@mapNotNull null

            val subMat = fullImageMat.submat(roi)
            val graySubMat = Mat()
            Imgproc.cvtColor(subMat, graySubMat, Imgproc.COLOR_BGR2GRAY)

            val refinedCenter = when (state.cvRefinementMethod) {
                CvRefinementMethod.HOUGH -> refineWithHoughCircles(graySubMat, screenRect, state)
                CvRefinementMethod.CONTOUR -> refineWithContours(subMat, screenRect, state)
            }

            subMat.release()
            graySubMat.release()
            refinedCenter
        }
    }

    private fun refineWithHoughCircles(roiGray: Mat, screenRect: RectF, state: OverlayState): PointF? {
        val circles = Mat()
        val expectedRadius = DrawingUtils.getExpectedRadiusAtScreenY(screenRect.centerY(), state)
        val radiusMargin = expectedRadius * 0.5f

        Imgproc.HoughCircles(
            roiGray,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            (expectedRadius * 2).toDouble(),
            state.houghP1.toDouble(),
            state.houghP2.toDouble(),
            (expectedRadius - radiusMargin).toInt().coerceAtLeast(1),
            (expectedRadius + radiusMargin).toInt().coerceAtLeast(5)
        )

        return if (circles.cols() > 0) {
            val circle = circles[0, 0]
            val refinedScreenX = screenRect.left + circle[0].toFloat()
            val refinedScreenY = screenRect.top + circle[1].toFloat()
            circles.release()
            PointF(refinedScreenX, refinedScreenY)
        } else {
            circles.release()
            null
        }
    }

    private fun refineWithContours(roi: Mat, screenRect: RectF, state: OverlayState): PointF? {
        val thresh = Mat()
        Imgproc.cvtColor(roi, thresh, Imgproc.COLOR_BGR2GRAY)
        Imgproc.threshold(thresh, thresh, 128.0, 255.0, Imgproc.THRESH_BINARY)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val expectedRadius = DrawingUtils.getExpectedRadiusAtScreenY(screenRect.centerY(), state)
        val radiusMargin = expectedRadius * 0.75f
        val minRadius = expectedRadius - radiusMargin
        val maxRadius = expectedRadius + radiusMargin

        var bestCandidate: PointF? = null
        var bestCircularity = 0.0

        for (contour in contours) {
            if (contour.total() > 5) { // Need at least 5 points to fit a circle
                val center = OCVPoint()
                val radiusArr = FloatArray(1)
                Imgproc.minEnclosingCircle(MatOfPoint2f(*contour.toArray()), center, radiusArr)
                val radius = radiusArr[0]

                if (radius in minRadius..maxRadius) {
                    val area = Imgproc.contourArea(contour)
                    val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                    if (perimeter > 0) {
                        val circularity = (4 * PI * area) / (perimeter * perimeter)
                        if (circularity > 0.7 && circularity > bestCircularity) {
                            bestCircularity = circularity
                            bestCandidate = PointF(screenRect.left + center.x.toFloat(), screenRect.top + center.y.toFloat())
                        }
                    }
                }
            }
            contour.release()
        }

        thresh.release()
        hierarchy.release()
        return bestCandidate
    }

    private fun imageToMat(image: InputImage): Mat {
        val yBuffer = image.planes?.get(0)?.buffer
        val uBuffer = image.planes?.get(1)?.buffer
        val vBuffer = image.planes?.get(2)?.buffer

        val ySize = yBuffer?.remaining() ?: 0
        val uSize = uBuffer?.remaining() ?: 0
        val vSize = vBuffer?.remaining() ?: 0

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer?.get(nv21, 0, ySize)
        vBuffer?.get(nv21, ySize, vSize)
        uBuffer?.get(nv21, ySize + vSize, uSize)

        val yuvImage = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        yuvImage.put(0, 0, nv21)
        val mat = Mat()
        Imgproc.cvtColor(yuvImage, mat, Imgproc.COLOR_YUV2BGR_NV21, 3)
        yuvImage.release()
        return mat
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