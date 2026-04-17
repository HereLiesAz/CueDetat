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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot

/**
 * CameraX ImageAnalysis.Analyzer.
 * Strategy 1: TFLite/ONNX Object Detection.
 * Strategy 2: Felt-Boundary Extraction. We map the presence of the table
 * to deduce the absence of the pockets by collapsing the felt into a quadrilateral.
 */
class TableScanAnalyzer(
    private val onPocketsDetected: (imagePoints: List<PointF>, edges: List<Pair<PointF, PointF>>?, tableBoundary: android.graphics.RectF?, confidence: Float, imageWidth: Int, imageHeight: Int) -> Unit,
    private val onFeltColorSampled: (FloatArray) -> Unit,
    private val pocketDetector: PocketDetector? = null,
) : ImageAnalysis.Analyzer {

    private var reusableBitmap: Bitmap? = null
    private var nv21Buffer: ByteArray? = null
    private var yuvMat: Mat? = null
    private var bgrMat: Mat? = null
    private var smallBgrMat: Mat? = null
    private var hsvMat: Mat? = null
    private var maskMat: Mat? = null
    
    private val isProcessing = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        // Skip frame if previous one is still processing to keep UI thread fluid
        if (isProcessing.get()) {
            image.close()
            return
        }
        isProcessing.set(true)

        try {
            val originalWidth = image.width
            val originalHeight = image.height

            // --- Efficient YUV_420_888 to BGR conversion handling strides ---
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val requiredBufferSize = originalWidth * originalHeight * 3 / 2
            if (nv21Buffer?.size != requiredBufferSize) {
                nv21Buffer = ByteArray(requiredBufferSize)
            }
            val nv21 = nv21Buffer!!
            val ySize = originalWidth * originalHeight

            // Copy Y plane row by row to handle strides
            val yRowStride = yPlane.rowStride
            for (row in 0 until originalHeight) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, row * originalWidth, originalWidth)
            }

            // Interleave UV for NV21 (VUVU...)
            val vPixelStride = vPlane.pixelStride
            val uPixelStride = uPlane.pixelStride
            val vRowStride = vPlane.rowStride
            val uRowStride = uPlane.rowStride

            if (vPixelStride == 2 && uPixelStride == 2 && vRowStride == uRowStride) {
                val uvSize = (originalWidth * originalHeight) / 2
                vBuffer.get(nv21, ySize, minOf(vBuffer.remaining(), uvSize))
            } else {
                var pos = ySize
                val uvWidth = originalWidth / 2
                val uvHeight = originalHeight / 2
                for (row in 0 until uvHeight) {
                    for (col in 0 until uvWidth) {
                        nv21[pos++] = vBuffer.get(row * vRowStride + col * vPixelStride)
                        nv21[pos++] = uBuffer.get(row * uRowStride + col * uPixelStride)
                    }
                }
            }

            if (yuvMat == null || yuvMat!!.cols() != originalWidth || yuvMat!!.rows() != originalHeight + originalHeight / 2) {
                yuvMat?.release()
                yuvMat = Mat(originalHeight + originalHeight / 2, originalWidth, CvType.CV_8UC1)
            }
            yuvMat!!.put(0, 0, nv21)

            if (bgrMat == null) bgrMat = Mat()
            Imgproc.cvtColor(yuvMat!!, bgrMat!!, Imgproc.COLOR_YUV2BGR_NV21)

            val bitmap = reusableBitmap?.takeIf { it.width == originalWidth && it.height == originalHeight }
                ?: Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888).also { reusableBitmap = it }
            Utils.matToBitmap(bgrMat!!, bitmap)

            // --- ALWAYS sample felt color from every frame ---
            val scale = 0.25
            val smallWidth = (originalWidth * scale).toInt()
            val smallHeight = (originalHeight * scale).toInt()
            
            if (smallBgrMat == null) smallBgrMat = Mat()
            Imgproc.resize(bgrMat!!, smallBgrMat!!, Size(smallWidth.toDouble(), smallHeight.toDouble()))

            if (hsvMat == null) hsvMat = Mat()
            Imgproc.cvtColor(smallBgrMat!!, hsvMat!!, Imgproc.COLOR_BGR2HSV)

            // Sample the centre felt color
            val cx = smallWidth / 2; val cy = smallHeight / 2
            val hw = smallWidth / 20; val hh = smallHeight / 20
            val roi = org.opencv.core.Rect(cx - hw, cy - hh, hw * 2, hh * 2)
            val crop = Mat(hsvMat!!, roi)
            val meanHsv = Core.mean(crop)
            crop.release()
            
            onFeltColorSampled(
                floatArrayOf(
                    meanHsv.`val`[0].toFloat() * 2f,
                    meanHsv.`val`[1].toFloat() / 255f,
                    meanHsv.`val`[2].toFloat() / 255f
                )
            )

            // --- Strategy 1: ML detector (TFLite + ONNX side-by-side) ---
            val modelDetections = pocketDetector?.detect(bitmap)

            if (modelDetections != null) {
                onPocketsDetected(
                    modelDetections.pockets, 
                    null, 
                    modelDetections.tableBoundary, 
                    modelDetections.confidence, 
                    originalWidth, 
                    originalHeight
                )
            } else {
                // --- Strategy 2: Felt-Boundary Extraction Fallback ---
                try {
                    val lowerBound = Scalar(maxOf(0.0, meanHsv.`val`[0] - 15.0), 50.0, 50.0)
                    val upperBound = Scalar(minOf(180.0, meanHsv.`val`[0] + 15.0), 255.0, 255.0)
                    
                    if (maskMat == null) maskMat = Mat()
                    Core.inRange(hsvMat!!, lowerBound, upperBound, maskMat!!)

                    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
                    Imgproc.morphologyEx(maskMat!!, maskMat!!, Imgproc.MORPH_CLOSE, kernel)
                    Imgproc.morphologyEx(maskMat!!, maskMat!!, Imgproc.MORPH_OPEN, kernel)
                    kernel.release()

                    val contours = mutableListOf<MatOfPoint>()
                    Imgproc.findContours(maskMat!!, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                    val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }

                    if (largestContour != null && Imgproc.contourArea(largestContour) > (smallWidth * smallHeight * 0.1)) {
                        val contour2f = MatOfPoint2f(*largestContour.toArray())
                        val perimeter = Imgproc.arcLength(contour2f, true)
                        val approx = MatOfPoint2f()

                        var epsilonCoeff = 0.01
                        while (epsilonCoeff < 0.1) {
                            Imgproc.approxPolyDP(contour2f, approx, epsilonCoeff * perimeter, true)
                            if (approx.rows() == 4) break
                            epsilonCoeff += 0.01
                        }

                        if (approx.rows() == 4) {
                            val pts = approx.toArray()
                            val corners = pts.map { PointF((it.x / scale).toFloat(), (it.y / scale).toFloat()) }

                            val edges = mutableListOf<Pair<PointF, PointF>>()
                            for (i in 0..3) {
                                edges.add(Pair(corners[i], corners[(i + 1) % 4]))
                            }

                            edges.sortByDescending { (p1, p2) -> hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()) }

                            val side1 = edges[0]
                            val side2 = edges[1]

                            val mid1 = PointF((side1.first.x + side1.second.x) / 2f, (side1.first.y + side1.second.y) / 2f)
                            val mid2 = PointF((side2.first.x + side2.second.x) / 2f, (side2.first.y + side2.second.y) / 2f)

                            val fallbackDetections = mutableListOf<PointF>()
                            fallbackDetections.addAll(corners)
                            fallbackDetections.add(mid1)
                            fallbackDetections.add(mid2)

                            onPocketsDetected(fallbackDetections, edges, null, 0.5f, originalWidth, originalHeight)
                        }
                        approx.release()
                        contour2f.release()
                    }
                    contours.forEach { it.release() }

                } catch (_: Exception) {}
            }
        } finally {
            image.close()
            isProcessing.set(false)
        }
    }
}
