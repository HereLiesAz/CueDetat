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
 * Detection strategy (in priority order):
 * 1. If [pocketDetector] is non-null, delegate to it. A null return from the
 *    detector falls through to strategy 2.
 * 2. Hough-circle fallback: downsample to 480p, run [Imgproc.HoughCircles],
 *    filter candidates by adaptive darkness threshold derived from the felt luma.
 *
 * Hough tuning (480p scale): minRadius=15, maxRadius=60,
 * param1 (Canny)=80, param2 (accumulator)=25.
 *
 * In v1.4, supply a TFLite-backed [PocketDetector] to replace the Hough pipeline.
 */
class TableScanAnalyzer(
    private val onPocketsDetected: (imagePoints: List<PointF>, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit,
    private val onFeltColorSampled: (FloatArray) -> Unit,
    private val pocketDetector: PocketDetector? = null,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val originalWidth = image.width
        val originalHeight = image.height

        val planes = image.planes
        val yBuffer = planes[0].buffer
        val yBytes = ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }

        // --- Strategy 1: TFLite detector (v1.4+) ---
        val modelDetections: List<PointF>? = pocketDetector?.detect(yBytes, originalWidth, originalHeight)

        val detections: List<PointF>
        if (modelDetections != null) {
            detections = modelDetections
        } else {
            // --- Strategy 2: Hough-circle fallback ---
            val grayFull = Mat(originalHeight, originalWidth, CvType.CV_8UC1)
            grayFull.put(0, 0, yBytes)

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

            // Derive adaptive brightness threshold from the felt's own luma.
            // Sample the center 10% of graySmall (same region as HSV felt-color sampling).
            // Pockets must be at least 60 Y-units darker than the felt surface.
            val fcx = targetWidth / 2; val fcy = targetHeight / 2
            val fhw = (targetWidth / 20).coerceAtLeast(1)
            val fhh = (targetHeight / 20).coerceAtLeast(1)
            val feltRoi = org.opencv.core.Rect(
                (fcx - fhw).coerceAtLeast(0), (fcy - fhh).coerceAtLeast(0),
                (fhw * 2).coerceAtMost(targetWidth), (fhh * 2).coerceAtMost(targetHeight)
            )
            val feltSample = Mat(graySmall, feltRoi)
            val feltMeanY = Core.mean(feltSample).`val`[0]
            feltSample.release()
            val pocketMaxBrightness = (feltMeanY - 60.0).coerceAtLeast(30.0)

            // Upscale detected centres back to original frame coordinates.
            // Filter by darkness: pockets are far darker than felt or balls.
            // graySmall is NOT released yet so we can sample brightness at each detection.
            val houghDetections = mutableListOf<PointF>()
            if (!circles.empty()) {
                for (i in 0 until circles.cols()) {
                    val data = circles.get(0, i)
                    val cx = data[0].toInt().coerceIn(0, targetWidth - 1)
                    val cy = data[1].toInt().coerceIn(0, targetHeight - 1)
                    val centerPixel = graySmall.get(cy, cx)
                    val brightness = centerPixel?.getOrNull(0) ?: 255.0
                    if (brightness < pocketMaxBrightness) {
                        val x = (data[0] / scale).toFloat()
                        val y = (data[1] / scale).toFloat()
                        houghDetections.add(PointF(x, y))
                    }
                }
            }
            circles.release()
            graySmall.release()
            detections = houghDetections
        }

        if (detections.isNotEmpty()) {
            onPocketsDetected(detections, originalWidth, originalHeight, rotationDegrees)
        }

        // Sample felt colour from the centre 10% of the frame.
        // We actually extract the U and V planes now, leaving 1950s television broadcasts in the past.
        try {
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer
            val ySize = yBytes.size
            val vSize = vBuffer.remaining()
            val uSize = uBuffer.remaining()

            val nv21 = ByteArray(ySize + vSize + uSize)
            System.arraycopy(yBytes, 0, nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvMat = Mat(originalHeight + originalHeight / 2, originalWidth, CvType.CV_8UC1)
            yuvMat.put(0, 0, nv21)
            val bgr = Mat()
            Imgproc.cvtColor(yuvMat, bgr, Imgproc.COLOR_YUV2BGR_NV21)

            val cx = originalWidth / 2; val cy = originalHeight / 2
            val hw = originalWidth / 20; val hh = originalHeight / 20
            val roi = org.opencv.core.Rect(cx - hw, cy - hh, hw * 2, hh * 2)
            val crop = Mat(bgr, roi)

            val hsv = Mat()
            Imgproc.cvtColor(crop, hsv, Imgproc.COLOR_BGR2HSV)
            val mean = Core.mean(hsv)
            onFeltColorSampled(floatArrayOf(mean.`val`[0].toFloat(), mean.`val`[1].toFloat() / 255f, mean.`val`[2].toFloat() / 255f))

            crop.release()
            bgr.release()
            hsv.release()
            yuvMat.release()
        } catch (_: Exception) { /* ignore if ROI is out of bounds */ }

        image.close()
    }
}