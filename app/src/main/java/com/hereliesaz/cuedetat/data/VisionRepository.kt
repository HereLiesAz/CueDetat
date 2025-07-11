package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2

/**
 * Data class to hold the results of computer vision processing.
 */
data class VisionData(
    val tableCorners: List<PointF> = emptyList(),
    val balls: List<PointF> = emptyList(),
    val detectedHsvColor: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisionData
        if (tableCorners != other.tableCorners) return false
        if (balls != other.balls) return false
        if (detectedHsvColor != null && other.detectedHsvColor != null) {
            if (!detectedHsvColor.contentEquals(other.detectedHsvColor)) return false
        } else if (other.detectedHsvColor != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tableCorners.hashCode()
        result = 31 * result + balls.hashCode()
        result = 31 * result + (detectedHsvColor?.contentHashCode() ?: 0)
        return result
    }
}


/**
 * Repository to handle computer vision tasks using a hybrid TFLite and OpenCV approach.
 */
@Singleton
class VisionRepository @Inject constructor(
    private val detector: ObjectDetector
) {

    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, uiState: OverlayState) {
        try {
            val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // --- TFLite Detection ---
            val detectedObjects: List<Detection> = detector.detect(inputImage)

            // --- Mat conversion for OpenCV ---
            val yBuffer: ByteBuffer = imageProxy.planes[0].buffer
            val uBuffer: ByteBuffer = imageProxy.planes[1].buffer
            val vBuffer: ByteBuffer = imageProxy.planes[2].buffer
            val ySize: Int = yBuffer.remaining()
            val uSize: Int = uBuffer.remaining()
            val vSize: Int = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvMat = Mat(imageProxy.height + imageProxy.height / 2, imageProxy.width, org.opencv.core.CvType.CV_8UC1)
            yuvMat.put(0, 0, nv21)
            val rgbaMat = Mat()
            Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21, 4)
            val rotatedRgbaMat = Mat()
            Core.rotate(rgbaMat, rotatedRgbaMat, imageProxy.imageInfo.rotationDegrees.toCvRotateCode())

            val refinedBalls = mutableListOf<PointF>()
            val grayMat = Mat()
            Imgproc.cvtColor(rotatedRgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            for (obj in detectedObjects) {
                val roiRect = obj.boundingBox.toOpenCvRect()
                if (roiRect.x >= 0 && roiRect.y >= 0 && roiRect.x + roiRect.width <= grayMat.cols() && roiRect.y + roiRect.height <= grayMat.rows()) {
                    val roi = Mat(grayMat, roiRect)
                    val circles = Mat()
                    // Run HoughCircles on the smaller ROI
                    Imgproc.HoughCircles(
                        roi, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                        roiRect.width.toDouble(),
                        uiState.houghP1.toDouble(),
                        uiState.houghP2.toDouble(),
                        5,
                        roiRect.width.coerceAtLeast(6)
                    )
                    if (!circles.empty()) {
                        val circle = circles.get(0, 0)
                        refinedBalls.add(PointF((circle[0] + roiRect.x).toFloat(), (circle[1] + roiRect.y).toFloat()))
                    }
                    roi.release()
                    circles.release()
                }
            }

            // Table detection using Canny edge
            val cannyEdges = Mat()
            Imgproc.Canny(grayMat, cannyEdges, uiState.cannyThreshold1.toDouble(), uiState.cannyThreshold2.toDouble())
            val lines = Mat()
            Imgproc.HoughLinesP(cannyEdges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)
            val tableCorners = findTableCorners(lines, rotatedRgbaMat.size())

            _visionDataFlow.value = VisionData(tableCorners = tableCorners, balls = refinedBalls)

            // Release Mats
            grayMat.release()
            cannyEdges.release()
            lines.release()
            yuvMat.release()
            rgbaMat.release()
            rotatedRgbaMat.release()

        } catch (e: Exception) {
            Log.e("VisionRepository", "Error processing image with TFLite/OpenCV", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun findTableCorners(lines: Mat, imageSize: Size): List<PointF> {
        if (lines.empty()) return emptyList()
        var top: DoubleArray? = null
        var bottom: DoubleArray? = null
        var left: DoubleArray? = null
        var right: DoubleArray? = null
        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val x1 = line[0]; val y1 = line[1]; val x2 = line[2]; val y2 = line[3]
            val angle = atan2(y2 - y1, x2 - x1) * 180 / Math.PI
            if (kotlin.math.abs(angle) < 20 || kotlin.math.abs(angle - 180) < 20) { // Horizontal
                if (top == null || y1 < top[1]) top = line
                if (bottom == null || y1 > bottom[1]) bottom = line
            } else if (kotlin.math.abs(angle - 90) < 20 || kotlin.math.abs(angle + 90) < 20) { // Vertical
                if (left == null || x1 < left[0]) left = line
                if (right == null || x1 > right[0]) right = line
            }
        }
        if (top == null || bottom == null || left == null || right == null) return emptyList()
        val corners = mutableListOf<PointF>()
        corners.add(lineIntersection(left, top) ?: PointF(0f, 0f))
        corners.add(lineIntersection(right, top) ?: PointF(0f, 0f))
        corners.add(lineIntersection(right, bottom) ?: PointF(0f, 0f))
        corners.add(lineIntersection(left, bottom) ?: PointF(0f, 0f))
        return corners
    }

    private fun lineIntersection(line1: DoubleArray, line2: DoubleArray): PointF? {
        val x1 = line1[0]; val y1 = line1[1]; val x2 = line1[2]; val y2 = line1[3]
        val x3 = line2[0]; val y3 = line2[1]; val x4 = line2[2]; val y4 = line2[3]
        val den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (den == 0.0) return null
        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
        val intersectX = x1 + t * (x2 - x1)
        val intersectY = y1 + t * (y2 - y1)
        return PointF(intersectX.toFloat(), intersectY.toFloat())
    }

    private fun Int.toCvRotateCode(): Int = when (this) {
        90 -> Core.ROTATE_90_CLOCKWISE
        180 -> Core.ROTATE_180
        270 -> Core.ROTATE_90_COUNTERCLOCKWISE
        else -> -1
    }

    private fun RectF.toOpenCvRect(): org.opencv.core.Rect {
        return org.opencv.core.Rect(this.left.toInt(), this.top.toInt(), this.width().toInt(), this.height().toInt())
    }
}