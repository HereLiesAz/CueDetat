// app/src/main/java/com/hereliesaz/cuedetat/data/FeltColorDetector.kt
package com.hereliesaz.cuedetat.data

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * Auto-detects the felt color of a pool table by finding the largest chromatic
 * connected component in the frame. No user interaction required, no table pose
 * dependency.
 *
 * Strategy: pool felt is by far the largest saturated surface in any pool-table
 * scene. Mask out greys, blacks, and blown highlights, then take the largest
 * remaining connected component. Its mean HSV is the felt color.
 *
 * Inputs are expected in OpenCV HSV space: H ∈ [0, 180], S ∈ [0, 255], V ∈ [0, 255].
 */
class FeltColorDetector {

    data class Result(
        val hsv: FloatArray,
        val stdDev: FloatArray,
        val coverage: Float,
        val confidence: Float,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Result) return false
            return hsv.contentEquals(other.hsv) &&
                stdDev.contentEquals(other.stdDev) &&
                coverage == other.coverage &&
                confidence == other.confidence
        }

        override fun hashCode(): Int {
            var result = hsv.contentHashCode()
            result = 31 * result + stdDev.contentHashCode()
            result = 31 * result + coverage.hashCode()
            result = 31 * result + confidence.hashCode()
            return result
        }
    }

    private val chromaticMask = Mat()
    private val labels = Mat()
    private val stats = Mat()
    private val centroids = Mat()
    private val componentMask = Mat()
    private val mean = MatOfDouble()
    private val stdDev = MatOfDouble()

    fun detect(hsvMat: Mat): Result? {
        if (hsvMat.empty()) return null

        val totalPixels = hsvMat.rows() * hsvMat.cols()
        if (totalPixels == 0) return null

        Core.inRange(hsvMat, LOWER_CHROMATIC, UPPER_CHROMATIC, chromaticMask)

        val count = Imgproc.connectedComponentsWithStats(
            chromaticMask, labels, stats, centroids, 8, CvType.CV_32S
        )
        if (count <= 1) return null

        var bestLabel = -1
        var bestArea = 0
        for (label in 1 until count) {
            val area = stats.get(label, Imgproc.CC_STAT_AREA)[0].toInt()
            if (area > bestArea) {
                bestArea = area
                bestLabel = label
            }
        }
        if (bestLabel < 0) return null

        val coverage = bestArea.toFloat() / totalPixels.toFloat()
        if (coverage < MIN_COVERAGE) return null

        Core.compare(labels, Scalar(bestLabel.toDouble()), componentMask, Core.CMP_EQ)
        Core.meanStdDev(hsvMat, mean, stdDev, componentMask)

        val hsv = floatArrayOf(
            mean.get(0, 0)[0].toFloat(),
            mean.get(1, 0)[0].toFloat(),
            mean.get(2, 0)[0].toFloat(),
        )
        val sd = floatArrayOf(
            stdDev.get(0, 0)[0].toFloat(),
            stdDev.get(1, 0)[0].toFloat(),
            stdDev.get(2, 0)[0].toFloat(),
        )

        val saturationScore = (hsv[1] / 255f).coerceIn(0f, 1f)
        val coverageScore = ((coverage - MIN_COVERAGE) / (0.5f - MIN_COVERAGE)).coerceIn(0f, 1f)
        val confidence = (saturationScore * 0.6f + coverageScore * 0.4f).coerceIn(0f, 1f)

        return Result(hsv = hsv, stdDev = sd, coverage = coverage, confidence = confidence)
    }

    fun release() {
        chromaticMask.release()
        labels.release()
        stats.release()
        centroids.release()
        componentMask.release()
        mean.release()
        stdDev.release()
    }

    private companion object {
        // Reject greys/whites (S < 76 ≈ 30%), shadows (V < 51 ≈ 20%),
        // and blown highlights (V > 242 ≈ 95%).
        val LOWER_CHROMATIC: Scalar = Scalar(0.0, 76.0, 51.0)
        val UPPER_CHROMATIC: Scalar = Scalar(180.0, 255.0, 242.0)

        // Felt must cover at least 8% of the frame to be considered a real candidate.
        const val MIN_COVERAGE = 0.08f
    }
}
