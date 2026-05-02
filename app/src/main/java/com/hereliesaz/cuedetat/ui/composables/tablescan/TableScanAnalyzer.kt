// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TableScanAnalyzer.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hereliesaz.cuedetat.BuildConfig
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import com.hereliesaz.cuedetat.utils.toMat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.hypot
import androidx.core.graphics.createBitmap

/**
 * CameraX ImageAnalysis.Analyzer.
 * Strategy 1: TFLite/ONNX Object Detection.
 * Strategy 2: Felt-Boundary Extraction. We map the presence of the table
 * to deduce the absence of the pockets by collapsing the felt into a quadrilateral.
 */
class TableScanAnalyzer(
    private val onPocketsDetected: (imagePoints: List<PointF>, edges: List<Pair<PointF, PointF>>?, tableBoundary: android.graphics.RectF?, confidence: Float, imageWidth: Int, imageHeight: Int) -> Unit,
    private val onFeltColorSampled: (FloatArray) -> Unit,
    private val onCenterVSampled: (normalizedV: Float, histogram: List<Float>) -> Unit = { _, _ -> },
    private val pocketDetector: PocketDetector? = null,
) : ImageAnalysis.Analyzer {

    private var reusableMlBitmap: Bitmap? = null
    private val bgrMat = Mat()
    private val rgbMat = Mat()
    private val smallRgbMat = Mat()
    private val hsvMat = Mat()
    private val maskMat = Mat()

    private val isProcessing = AtomicBoolean(false)
    private val frameCounter = AtomicInteger(0)

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

            // --- Robust YUV_420_888 to BGR conversion using shared utility ---
            image.toMat(bgrMat)

            if (bgrMat.empty() || bgrMat.cols() == 0 || bgrMat.rows() == 0) {
                Log.e("TableScanAnalyzer", "BGR Mat is empty or invalid after toMat(): size=${bgrMat.size()} format=${image.format}")
                return
            }

            // Convert to RGB for detector and sampling
            Imgproc.cvtColor(bgrMat, rgbMat, Imgproc.COLOR_BGR2RGB)

            // Diagnostic: check average brightness and color of the frame.
            // Logging is debug-only — at 30fps this is a string allocation in the
            // camera hot path, which we do not want shipped in release.
            val avgColor = Core.mean(rgbMat)
            val frameNum = frameCounter.incrementAndGet()
            if (BuildConfig.DEBUG) {
                if (frameNum % 30 == 0) {
                    Log.d("TableScanAnalyzer", "Frame Avg RGB: ${avgColor.`val`[0].toInt()}, ${avgColor.`val`[1].toInt()}, ${avgColor.`val`[2].toInt()}")
                }
                if (avgColor.`val`[0] < 5.0 && avgColor.`val`[1] < 5.0 && avgColor.`val`[2] < 5.0) {
                    Log.w("TableScanAnalyzer", "Frame is very dark!")
                }
            }

            // --- ALWAYS sample felt color from every frame ---
            val scale = 0.25
            val scaleF = scale.toFloat()
            val smallWidth = (originalWidth * scale).toInt()
            val smallHeight = (originalHeight * scale).toInt()

            if (smallWidth < 1 || smallHeight < 1) return

            Imgproc.resize(rgbMat, smallRgbMat, Size(smallWidth.toDouble(), smallHeight.toDouble()))
            Imgproc.cvtColor(smallRgbMat, hsvMat, Imgproc.COLOR_RGB2HSV)

            // Sample the centre felt color
            val cx = smallWidth / 2; val cy = smallHeight / 2
            val hw = (smallWidth / 20).coerceAtLeast(1)
            val hh = (smallHeight / 20).coerceAtLeast(1)

            val roiX = (cx - hw).coerceIn(0, smallWidth - 1)
            val roiY = (cy - hh).coerceIn(0, smallHeight - 1)
            val roiW = (hw * 2).coerceIn(1, smallWidth - roiX)
            val roiH = (hh * 2).coerceIn(1, smallHeight - roiY)

            val roi = org.opencv.core.Rect(roiX, roiY, roiW, roiH)
            val crop = hsvMat.submat(roi)
            val centerVNormalised: Float
            val centerHistogram: List<Float>
            val meanHsv: org.opencv.core.Scalar
            try {
                meanHsv = Core.mean(crop)

                // Sample centre region V channel and 16-bin histogram for pocket darkness check
                centerVNormalised = (meanHsv.`val`[2] / 255f).toFloat()

                // Compute 16-bin V-channel histogram of centre region
                val hist = org.opencv.core.Mat()
                val histChannels = org.opencv.core.MatOfInt(2)
                val histBins = org.opencv.core.MatOfInt(16)
                val histRanges = org.opencv.core.MatOfFloat(0f, 256f)
                val histMask = Mat()
                try {
                    Imgproc.calcHist(
                        listOf(crop),
                        histChannels,
                        histMask,
                        hist,
                        histBins,
                        histRanges
                    )
                    Core.normalize(hist, hist)
                    centerHistogram = (0 until 16).map { hist.get(it, 0)[0].toFloat() }
                } finally {
                    histMask.release()
                    histRanges.release()
                    histBins.release()
                    histChannels.release()
                    hist.release()
                }
            } finally {
                crop.release()
            }

            onCenterVSampled(centerVNormalised, centerHistogram)

            val sampledHsv = floatArrayOf(
                meanHsv.`val`[0].toFloat() * 2f, // 0-180 -> 0-360
                meanHsv.`val`[1].toFloat() / 255f, // 0-255 -> 0-1
                meanHsv.`val`[2].toFloat() / 255f  // 0-255 -> 0-1
            )
            onFeltColorSampled(sampledHsv)

            // --- Strategy 1: ML detector (TFLite side-by-side) ---
            // Downsample the silicon torture chamber. We feed it the small mat.
            val mlBitmap = reusableMlBitmap?.takeIf { it.width == smallWidth && it.height == smallHeight }
                ?: createBitmap(smallWidth, smallHeight).also { reusableMlBitmap = it }
            Utils.matToBitmap(smallRgbMat, mlBitmap)

            val modelDetections = pocketDetector?.detect(mlBitmap)

            if (modelDetections != null && modelDetections.pockets.isNotEmpty()) {
                android.util.Log.d("TableScanAnalyzer", "ML Strategy: Found ${modelDetections.pockets.size} pockets (conf: ${modelDetections.confidence})")

                // Scale the detections back up to the original frame size
                val scaledPockets = modelDetections.pockets.map {
                    PointF(it.x / scaleF, it.y / scaleF)
                }
                val scaledBoundary = modelDetections.tableBoundary?.let {
                    android.graphics.RectF(
                        it.left / scaleF,
                        it.top / scaleF,
                        it.right / scaleF,
                        it.bottom / scaleF
                    )
                }

                onPocketsDetected(
                    scaledPockets,
                    null,
                    scaledBoundary,
                    modelDetections.confidence,
                    originalWidth,
                    originalHeight
                )
            } else {
                // --- Strategy 2: Felt-Boundary Extraction Fallback ---
                try {
                    val h = meanHsv.`val`[0]
                    val hRange = 15.0
                    val sMin = 40.0
                    val vMin = 40.0

                    if (h - hRange < 0 || h + hRange > 180) {
                        val mask1 = Mat()
                        val mask2 = Mat()
                        val lower1 = Scalar(0.0, sMin, vMin)
                        val upper1 = Scalar(minOf(180.0, h + hRange).let { if (it > 180) it - 180 else it }, 255.0, 255.0)
                        val lower2 = Scalar(maxOf(0.0, h - hRange).let { if (it < 0) it + 180 else it }, sMin, vMin)
                        val upper2 = Scalar(180.0, 255.0, 255.0)

                        Core.inRange(hsvMat, lower1, upper1, mask1)
                        Core.inRange(hsvMat, lower2, upper2, mask2)
                        Core.bitwise_or(mask1, mask2, maskMat)
                        mask1.release()
                        mask2.release()
                    } else {
                        val lowerBound = Scalar(h - hRange, sMin, vMin)
                        val upperBound = Scalar(h + hRange, 255.0, 255.0)
                        Core.inRange(hsvMat, lowerBound, upperBound, maskMat)
                    }

                    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
                    Imgproc.morphologyEx(maskMat, maskMat, Imgproc.MORPH_CLOSE, kernel)
                    Imgproc.morphologyEx(maskMat, maskMat, Imgproc.MORPH_OPEN, kernel)
                    kernel.release()

                    val contours = mutableListOf<MatOfPoint>()
                    val contourHierarchy = Mat()
                    try {
                        Imgproc.findContours(maskMat, contours, contourHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }

                        if (largestContour != null && Imgproc.contourArea(largestContour) > (smallWidth * smallHeight * 0.1)) {
                            val contour2f = MatOfPoint2f(*largestContour.toArray())
                            val approx = MatOfPoint2f()
                            try {
                                val perimeter = Imgproc.arcLength(contour2f, true)
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
                            } finally {
                                approx.release()
                                contour2f.release()
                            }
                        }
                    } finally {
                        contourHierarchy.release()
                        contours.forEach { it.release() }
                    }

                } catch (_: Exception) {}
            }
        } finally {
            image.close()
            isProcessing.set(false)
        }
    }
}