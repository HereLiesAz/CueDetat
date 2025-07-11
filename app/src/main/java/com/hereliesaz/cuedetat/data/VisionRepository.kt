package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.PointF
import androidx.camera.core.ImageProxy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Data class to hold the results of computer vision processing.
 */
data class VisionData(
    val tableCorners: List<PointF> = emptyList(),
    val balls: List<PointF> = emptyList(),
    val detectedHsvColor: FloatArray? = null // HSV color detected automatically
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisionData
        if (tableCorners != other.tableCorners) return false
        if (balls != other.balls) return false
        if (detectedHsvColor != null && other.detectedHsvColor != null) {
            if (!detectedHsvColor.contentEquals(other.detectedHsvColor)) return false
        } else if (detectedHsvColor != other.detectedHsvColor) return false
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
 * Repository to handle computer vision tasks using OpenCV.
 * It processes camera frames to detect pool table geometry and ball positions.
 */
@Singleton
class VisionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processImage(image: ImageProxy, expectedPixelRadius: Float, lockedHsvColor: FloatArray?) {
        val yuvBytes = ByteArray(image.width * image.height * 3 / 2)
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer

        yPlane.get(yuvBytes, 0, image.width * image.height)
        vPlane.get(yuvBytes, image.width * image.height, image.width * image.height / 4)
        uPlane.get(yuvBytes, image.width * image.height + image.width * image.height / 4, image.width * image.height / 4)

        val yuvMat = Mat(image.height + image.height / 2, image.width, org.opencv.core.CvType.CV_8UC1)
        yuvMat.put(0, 0, yuvBytes)
        val rgbaMat = Mat()
        Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_I420, 4)

        var rotatedRgbaMat = Mat()
        when (image.imageInfo.rotationDegrees) {
            90 -> Core.rotate(rgbaMat, rotatedRgbaMat, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(rgbaMat, rotatedRgbaMat, Core.ROTATE_180)
            270 -> Core.rotate(rgbaMat, rotatedRgbaMat, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> rotatedRgbaMat = rgbaMat
        }

        // --- Automatic Color Detection & Masking ---
        val hsvMat = Mat()
        Imgproc.cvtColor(rotatedRgbaMat, hsvMat, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(hsvMat, hsvMat, Imgproc.COLOR_RGB2HSV)

        val centerPixel = hsvMat.get(hsvMat.rows() / 2, hsvMat.cols() / 2)
        val autoDetectedHsv = floatArrayOf(centerPixel[0].toFloat(), centerPixel[1].toFloat(), centerPixel[2].toFloat())

        val targetHsv = lockedHsvColor ?: autoDetectedHsv

        val h = targetHsv[0]; val s = targetHsv[1]; val v = targetHsv[2]
        val h_tolerance = 15f; val s_tolerance = 75f; val v_tolerance = 75f

        val lowerBound = Scalar((h - h_tolerance).toDouble(), (s - s_tolerance).toDouble(), (v - v_tolerance).toDouble())
        val upperBound = Scalar((h + h_tolerance).toDouble(), (s + s_tolerance).toDouble(), (v + v_tolerance).toDouble())

        val colorMask = Mat()
        Core.inRange(hsvMat, lowerBound, upperBound, colorMask)

        // --- Ball & Table Detection on Masked Image ---
        val grayMat = Mat()
        Imgproc.cvtColor(rotatedRgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        val maskedGray = Mat()
        Core.bitwise_and(grayMat, colorMask, maskedGray)
        Imgproc.GaussianBlur(maskedGray, maskedGray, Size(9.0, 9.0), 2.0, 2.0)

        // Ball Detection
        val circles = Mat()
        val minRadius = (expectedPixelRadius * 0.50f).toInt().coerceAtLeast(10)
        val maxRadius = (expectedPixelRadius * 1.50f).toInt().coerceAtLeast(20)

        Imgproc.HoughCircles(
            maskedGray, circles, Imgproc.HOUGH_GRADIENT, 1.0,
            expectedPixelRadius.toDouble() * 2, 100.0, 30.0, minRadius, maxRadius
        )

        val detectedBalls = mutableListOf<PointF>()
        for (i in 0 until circles.cols()) {
            val circle = circles.get(0, i)
            if (circle != null && circle.isNotEmpty()) {
                val center = Point(circle[0].roundToInt().toDouble(), circle[1].roundToInt().toDouble())
                detectedBalls.add(PointF(center.x.toFloat(), center.y.toFloat()))
            }
        }

        // Table Detection
        val cannyEdges = Mat()
        Imgproc.Canny(maskedGray, cannyEdges, 50.0, 150.0)
        val lines = Mat()
        Imgproc.HoughLinesP(cannyEdges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)

        val tableCorners = findTableCorners(lines, rotatedRgbaMat.size())

        _visionDataFlow.value = VisionData(
            tableCorners = tableCorners,
            balls = detectedBalls + tableCorners,
            detectedHsvColor = autoDetectedHsv
        )

        // Release Mats
        yuvMat.release()
        rgbaMat.release()
        rotatedRgbaMat.release()
        grayMat.release()
        maskedGray.release()
        hsvMat.release()
        colorMask.release()
        circles.release()
        cannyEdges.release()
        lines.release()

        image.close()
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

            if (abs(angle) < 20 || abs(angle - 180) < 20) { // Horizontal
                if (top == null || y1 < top[1]) top = line
                if (bottom == null || y1 > bottom[1]) bottom = line
            } else if (abs(angle - 90) < 20 || abs(angle + 90) < 20) { // Vertical
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
        val u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / den

        val intersectX = x1 + t * (x2 - x1)
        val intersectY = y1 + t * (y2 - y1)

        return PointF(intersectX.toFloat(), intersectY.toFloat())
    }
}