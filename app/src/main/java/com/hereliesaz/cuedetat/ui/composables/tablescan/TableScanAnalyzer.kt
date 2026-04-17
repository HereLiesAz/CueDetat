// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot

/**
 * CameraX ImageAnalysis.Analyzer.
 * Strategy 1: TFLite/ONNX Object Detection.
 * Strategy 2: Felt-Boundary Extraction. We map the presence of the table
 * to deduce the absence of the pockets by collapsing the felt into a quadrilateral.
 */
class TableScanAnalyzer(
    private val onPocketsDetected: (imagePoints: List<PointF>, edges: List<Pair<PointF, PointF>>?, tableBoundary: android.graphics.RectF?, confidence: Float, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit,
    private val onFeltColorSampled: (FloatArray) -> Unit,
    private val pocketDetector: PocketDetector? = null,
) : ImageAnalysis.Analyzer {

    private var reusableBitmap: Bitmap? = null

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val originalWidth = image.width
        val originalHeight = image.height

        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        // --- Prepare BGR Mat and Bitmap for Strategy 1 & 2 ---
        val ySize = yBuffer.remaining()
        val vSize = vBuffer.remaining()
        val uSize = uBuffer.remaining()

        val nv21 = ByteArray(ySize + vSize + uSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvMat = Mat(originalHeight + originalHeight / 2, originalWidth, CvType.CV_8UC1)
        yuvMat.put(0, 0, nv21)
        val bgr = Mat()
        Imgproc.cvtColor(yuvMat, bgr, Imgproc.COLOR_YUV2BGR_NV21)

        val bitmap = reusableBitmap?.takeIf { it.width == originalWidth && it.height == originalHeight }
            ?: Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888).also { reusableBitmap = it }
        Utils.matToBitmap(bgr, bitmap)

        // --- Strategy 1: ML detector (TFLite + ONNX side-by-side) ---
        val modelDetections = pocketDetector?.detect(bitmap)

        if (modelDetections != null) {
            onPocketsDetected(
                modelDetections.pockets, 
                null, 
                modelDetections.tableBoundary, 
                modelDetections.confidence, 
                originalWidth, 
                originalHeight, 
                rotationDegrees
            )
        } else {
            // --- Strategy 2: Felt-Boundary Extraction Fallback ---
            try {
                // Downscale for performance to avoid freezing the camera pipeline
                val scale = 0.25
                val smallWidth = (originalWidth * scale).toInt()
                val smallHeight = (originalHeight * scale).toInt()
                val smallBgr = Mat()
                Imgproc.resize(bgr, smallBgr, Size(smallWidth.toDouble(), smallHeight.toDouble()))

                val hsv = Mat()
                Imgproc.cvtColor(smallBgr, hsv, Imgproc.COLOR_BGR2HSV)

                // Sample the centre felt color
                val cx = smallWidth / 2; val cy = smallHeight / 2
                val hw = smallWidth / 20; val hh = smallHeight / 20
                val roi = org.opencv.core.Rect(cx - hw, cy - hh, hw * 2, hh * 2)
                val crop = Mat(hsv, roi)
                val meanHsv = Core.mean(crop)
                onFeltColorSampled(
                    floatArrayOf(
                        meanHsv.`val`[0].toFloat() * 2f,
                        meanHsv.`val`[1].toFloat() / 255f,
                        meanHsv.`val`[2].toFloat() / 255f
                    )
                )

                // Isolate the felt based on the sampled center
                val lowerBound = Scalar(maxOf(0.0, meanHsv.`val`[0] - 15.0), 50.0, 50.0)
                val upperBound = Scalar(minOf(180.0, meanHsv.`val`[0] + 15.0), 255.0, 255.0)
                val mask = Mat()
                Core.inRange(hsv, lowerBound, upperBound, mask)

                val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
                Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
                Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel)

                val contours = mutableListOf<MatOfPoint>()
                Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }

                if (largestContour != null && Imgproc.contourArea(largestContour) > (smallWidth * smallHeight * 0.1)) {
                    val contour2f = MatOfPoint2f(*largestContour.toArray())
                    val perimeter = Imgproc.arcLength(contour2f, true)
                    val approx = MatOfPoint2f()

                    // Iteratively force a quadrilateral collapse. We beat the polygon until it surrenders 4 points.
                    var epsilonCoeff = 0.01
                    while (epsilonCoeff < 0.1) {
                        Imgproc.approxPolyDP(contour2f, approx, epsilonCoeff * perimeter, true)
                        if (approx.rows() == 4) break
                        epsilonCoeff += 0.01
                    }

                    if (approx.rows() == 4) {
                        val pts = approx.toArray()
                        // Scale back to original dimension reality
                        val corners = pts.map { PointF((it.x / scale).toFloat(), (it.y / scale).toFloat()) }

                        val edges = mutableListOf<Pair<PointF, PointF>>()
                        for (i in 0..3) {
                            edges.add(Pair(corners[i], corners[(i + 1) % 4]))
                        }

                        // The two longest edges on a pool table are the side rails.
                        // Their exact midpoints are the side pockets.
                        edges.sortByDescending { (p1, p2) -> hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()) }

                        val side1 = edges[0]
                        val side2 = edges[1]

                        val mid1 = PointF((side1.first.x + side1.second.x) / 2f, (side1.first.y + side1.second.y) / 2f)
                        val mid2 = PointF((side2.first.x + side2.second.x) / 2f, (side2.first.y + side2.second.y) / 2f)

                        val fallbackDetections = mutableListOf<PointF>()
                        fallbackDetections.addAll(corners)
                        fallbackDetections.add(mid1)
                        fallbackDetections.add(mid2)

                        onPocketsDetected(fallbackDetections, edges, null, 0.5f, originalWidth, originalHeight, rotationDegrees)
                    }
                    approx.release()
                    contour2f.release()
                }

                // Release memory back to the ether
                crop.release()
                mask.release()
                kernel.release()
                hsv.release()
                smallBgr.release()
                yuvMat.release()
                contours.forEach { it.release() }

            } catch (_: Exception) {
                // The machine blinked. Let the frame die in peace.
            }
        }
        bgr.release()
        image.close()
    }
}
