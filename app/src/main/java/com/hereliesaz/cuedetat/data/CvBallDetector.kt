// app/src/main/java/com/hereliesaz/cuedetat/data/CvBallDetector.kt
package com.hereliesaz.cuedetat.data

import android.graphics.PointF
import android.graphics.Rect
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import org.opencv.core.Rect as CvRect

/**
 * Classical-CV ball detector that runs alongside the ML model. Uses the felt
 * mask as a prior: balls are circular non-felt regions surrounded by felt.
 *
 * Pipeline:
 *  1. Build felt mask from sampled HSV (mean ± stdDev tolerance).
 *  2. Morphologically close to seal ball-sized holes in the felt.
 *  3. Subtract the original mask: the resulting blobs are ball candidates.
 *  4. Filter blobs by area and circularity.
 *  5. For each blob, run Hough circles in a local crop to refine sub-pixel
 *     centre and radius.
 *  6. Classify cue / 8 / solid / stripe from interior colour.
 */
class CvBallDetector {

    data class Detection(
        val center: PointF,
        val radius: Float,
        val type: BallType,
        val confidence: Float,
    )

    private val feltMask = Mat()
    private val closedMask = Mat()
    private val ballMask = Mat()
    private val labels = Mat()
    private val stats = Mat()
    private val centroids = Mat()
    private val gray = Mat()
    private val circlesMat = Mat()
    private val hierarchy = Mat()
    private var morphKernel: Mat? = null

    /**
     * @param bgrMat full-resolution BGR frame
     * @param hsvMat full-resolution HSV frame (same dimensions)
     * @param feltHsv mean felt HSV in OpenCV space (H 0-180, S 0-255, V 0-255)
     * @param feltStdDev per-channel std dev for inRange tolerance
     * @param expectedBallRadiusPx hint at expected ball radius in image pixels
     *        used as the centre of the accepted size range
     */
    fun detect(
        bgrMat: Mat,
        hsvMat: Mat,
        feltHsv: FloatArray,
        feltStdDev: FloatArray,
        expectedBallRadiusPx: Float = 0f,
    ): List<Detection> {
        if (bgrMat.empty() || hsvMat.empty()) return emptyList()
        if (bgrMat.rows() != hsvMat.rows() || bgrMat.cols() != hsvMat.cols()) return emptyList()

        val sdScale = 2.5f
        val lower = Scalar(
            max(0.0, feltHsv[0] - sdScale * feltStdDev[0] - 5.0),
            max(40.0, (feltHsv[1] - sdScale * feltStdDev[1]).toDouble()),
            max(40.0, (feltHsv[2] - sdScale * feltStdDev[2]).toDouble()),
        )
        val upper = Scalar(
            min(180.0, feltHsv[0] + sdScale * feltStdDev[0] + 5.0),
            min(255.0, (feltHsv[1] + sdScale * feltStdDev[1]).toDouble()),
            min(255.0, (feltHsv[2] + sdScale * feltStdDev[2]).toDouble()),
        )

        Core.inRange(hsvMat, lower, upper, feltMask)

        val frameSide = min(bgrMat.cols(), bgrMat.rows())
        val estRadius = if (expectedBallRadiusPx > 0f) {
            expectedBallRadiusPx
        } else {
            frameSide * 0.025f
        }.coerceIn(MIN_RADIUS_PX, MAX_RADIUS_PX)

        val kernelSize = max(3.0, estRadius * 0.8).toInt().let { if (it % 2 == 0) it + 1 else it }
        morphKernel?.release()
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(kernelSize.toDouble(), kernelSize.toDouble())
        ).also { morphKernel = it }

        Imgproc.morphologyEx(feltMask, closedMask, Imgproc.MORPH_CLOSE, kernel)
        Core.subtract(closedMask, feltMask, ballMask)

        val componentCount = Imgproc.connectedComponentsWithStats(
            ballMask, labels, stats, centroids, 8, CvType.CV_32S
        )
        if (componentCount <= 1) return emptyList()

        val minArea = (PI * (estRadius * 0.4) * (estRadius * 0.4)).toInt().coerceAtLeast(MIN_AREA_PX)
        val maxArea = (PI * (estRadius * 2.5) * (estRadius * 2.5)).toInt()

        val results = ArrayList<Detection>()

        for (label in 1 until componentCount) {
            val area = stats.get(label, Imgproc.CC_STAT_AREA)[0].toInt()
            if (area < minArea || area > maxArea) continue

            val x = stats.get(label, Imgproc.CC_STAT_LEFT)[0].toInt()
            val y = stats.get(label, Imgproc.CC_STAT_TOP)[0].toInt()
            val w = stats.get(label, Imgproc.CC_STAT_WIDTH)[0].toInt()
            val h = stats.get(label, Imgproc.CC_STAT_HEIGHT)[0].toInt()

            val aspect = max(w, h).toFloat() / min(w, h).toFloat()
            if (aspect > 2.0f) continue

            val approxRadius = (sqrt(area / PI)).toFloat()
            val bboxRadius = (max(w, h) / 2f)
            val circularity = approxRadius / bboxRadius
            if (circularity < 0.55f) continue

            val pad = (bboxRadius * 0.6f).toInt().coerceAtLeast(4)
            val cropX = (x - pad).coerceAtLeast(0)
            val cropY = (y - pad).coerceAtLeast(0)
            val cropW = (w + 2 * pad).coerceAtMost(bgrMat.cols() - cropX)
            val cropH = (h + 2 * pad).coerceAtMost(bgrMat.rows() - cropY)
            if (cropW <= 0 || cropH <= 0) continue

            val crop = bgrMat.submat(CvRect(cropX, cropY, cropW, cropH))
            try {
                val refined = refineWithHough(crop, bboxRadius)
                val (cxLocal, cyLocal, refinedRadius) = refined ?: Triple(
                    centroids.get(label, 0)[0].toFloat() - cropX,
                    centroids.get(label, 1)[0].toFloat() - cropY,
                    bboxRadius,
                )

                val cx = cxLocal + cropX
                val cy = cyLocal + cropY

                val type = classifyBallType(
                    bgrMat,
                    Rect(
                        (cx - refinedRadius).toInt().coerceAtLeast(0),
                        (cy - refinedRadius).toInt().coerceAtLeast(0),
                        (cx + refinedRadius).toInt().coerceAtMost(bgrMat.cols()),
                        (cy + refinedRadius).toInt().coerceAtMost(bgrMat.rows()),
                    )
                )

                results.add(
                    Detection(
                        center = PointF(cx, cy),
                        radius = refinedRadius,
                        type = type,
                        confidence = circularity,
                    )
                )
            } finally {
                crop.release()
            }
        }

        return results
    }

    private fun refineWithHough(crop: Mat, bboxRadius: Float): Triple<Float, Float, Float>? {
        Imgproc.cvtColor(crop, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.medianBlur(gray, gray, 3)

        val minR = (bboxRadius * 0.5f).toInt().coerceAtLeast(MIN_RADIUS_PX.toInt())
        val maxR = (bboxRadius * 1.5f).toInt().coerceAtMost(MAX_RADIUS_PX.toInt())
        if (minR >= maxR) return null

        Imgproc.HoughCircles(
            gray, circlesMat, Imgproc.HOUGH_GRADIENT,
            1.0,
            (bboxRadius * 1.5).toDouble().coerceAtLeast(8.0),
            80.0, 18.0,
            minR, maxR
        )

        if (circlesMat.empty() || circlesMat.cols() == 0) return null

        val data = DoubleArray(3)
        circlesMat.get(0, 0, data)
        return Triple(data[0].toFloat(), data[1].toFloat(), data[2].toFloat())
    }

    private fun classifyBallType(frame: Mat, box: Rect): BallType {
        val x = box.left.coerceIn(0, frame.cols() - 1)
        val y = box.top.coerceIn(0, frame.rows() - 1)
        val w = (box.right - x).coerceIn(1, frame.cols() - x)
        val h = (box.bottom - y).coerceIn(1, frame.rows() - y)

        val roi = CvRect(x, y, w, h)
        val ballMat = frame.submat(roi)
        val hsv = Mat()
        try {
            Imgproc.cvtColor(ballMat, hsv, Imgproc.COLOR_BGR2HSV)

            val midTop = (h * 0.30f).toInt().coerceIn(0, h - 1)
            val midH = (h * 0.40f).toInt().coerceAtLeast(1).coerceAtMost(h - midTop)
            val midHsv = hsv.submat(CvRect(0, midTop, w, midH))
            val midMean = Core.mean(midHsv)
            midHsv.release()

            if (midMean.`val`[2] < 60.0) return BallType.EIGHT
            if (midMean.`val`[1] < 40.0 && midMean.`val`[2] > 190.0) return BallType.CUE

            val poleH = (h * 0.15f).toInt().coerceAtLeast(1)
            val topHsv = hsv.submat(CvRect(0, 0, w, poleH))
            val bottomHsv = hsv.submat(CvRect(0, h - poleH, w, poleH))
            val topMean = Core.mean(topHsv)
            val bottomMean = Core.mean(bottomHsv)
            topHsv.release()
            bottomHsv.release()

            val topWhite = topMean.`val`[1] < 50.0 && topMean.`val`[2] > 180.0
            val bottomWhite = bottomMean.`val`[1] < 50.0 && bottomMean.`val`[2] > 180.0
            return if (topWhite && bottomWhite) BallType.STRIPE else BallType.SOLID
        } finally {
            ballMat.release()
            hsv.release()
        }
    }

    fun release() {
        feltMask.release()
        closedMask.release()
        ballMask.release()
        labels.release()
        stats.release()
        centroids.release()
        gray.release()
        circlesMat.release()
        hierarchy.release()
        morphKernel?.release()
        morphKernel = null
    }

    private companion object {
        const val MIN_RADIUS_PX = 4f
        const val MAX_RADIUS_PX = 80f
        const val MIN_AREA_PX = 50
    }
}
