// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.PointF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * CameraX ImageAnalysis.Analyzer that detects pocket-sized circular blobs
 * in each frame and forwards detected image-space positions to the ViewModel.
 *
 * Pockets are larger and darker than billiard balls. Tuned parameters:
 *   minRadius = 15 px (at 480p scale), maxRadius = 60 px
 *   param1 (Canny threshold) = 80, param2 (accumulator threshold) = 25
 *
 * Frames are downsampled to 480p height before processing to limit CPU cost.
 */
class TableScanAnalyzer(
    private val onPocketsDetected: (imagePoints: List<PointF>, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit,
    private val onFeltColorSampled: (FloatArray) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val originalWidth = image.width
        val originalHeight = image.height

        // Simplified: work on the Y (luma) plane only — sufficient for Hough circles.
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val yBytes = ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
        val grayFull = Mat(originalHeight, originalWidth, CvType.CV_8UC1)
        grayFull.put(0, 0, yBytes)

        image.close()

        // Downsample to target height of 480 for speed.
        val targetHeight = 480
        val scale = targetHeight.toDouble() / originalHeight
        val targetWidth = (originalWidth * scale).toInt()
        val graySmall = Mat()
        Imgproc.resize(grayFull, graySmall, Size(targetWidth.toDouble(), targetHeight.toDouble()))
        grayFull.release()

        // Hough circle detection tuned for pocket size.
        val circles = Mat()
        Imgproc.HoughCircles(
            graySmall, circles, Imgproc.CV_HOUGH_GRADIENT,
            /* dp= */ 1.5,
            /* minDist= */ graySmall.rows() / 5.0,
            /* param1= */ 80.0,
            /* param2= */ 25.0,
            /* minRadius= */ 15,
            /* maxRadius= */ 60
        )
        graySmall.release()

        // Upscale detected centres back to original frame coordinates.
        val detections = mutableListOf<PointF>()
        if (!circles.empty()) {
            for (i in 0 until circles.cols()) {
                val data = circles.get(0, i)
                val x = (data[0] / scale).toFloat()
                val y = (data[1] / scale).toFloat()
                detections.add(PointF(x, y))
            }
        }
        circles.release()

        if (detections.isNotEmpty()) {
            onPocketsDetected(detections, originalWidth, originalHeight, rotationDegrees)
        }

        // Sample felt colour from the centre 10% of the frame (full-res luma → BGR → HSV).
        // Run every frame; the ViewModel holds the rolling value used at scan completion.
        try {
            val cx = originalWidth / 2; val cy = originalHeight / 2
            val hw = originalWidth / 20; val hh = originalHeight / 20
            val roi = org.opencv.core.Rect(cx - hw, cy - hh, hw * 2, hh * 2)
            val yRoi = Mat(originalHeight, originalWidth, CvType.CV_8UC1)
            yRoi.put(0, 0, yBytes)
            val crop = Mat(yRoi, roi)
            val bgr = Mat(); Imgproc.cvtColor(crop, bgr, Imgproc.COLOR_GRAY2BGR)
            val hsv = Mat(); Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV)
            val mean = Core.mean(hsv)
            onFeltColorSampled(floatArrayOf(mean.`val`[0].toFloat(), mean.`val`[1].toFloat() / 255f, mean.`val`[2].toFloat() / 255f))
            crop.release(); bgr.release(); hsv.release(); yRoi.release()
        } catch (_: Exception) { /* ignore if ROI is out of bounds */ }
    }
}
