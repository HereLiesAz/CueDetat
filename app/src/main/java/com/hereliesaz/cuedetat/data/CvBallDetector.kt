// app/src/main/java/com/hereliesaz/cuedetat/data/CvBallDetector.kt
package com.hereliesaz.cuedetat.data

import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.BallIslandRules
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min
import org.opencv.core.Rect as CvRect

/**
 * Finds pool balls by colour: a ball is an island of non-felt inside the table.
 *
 * Pipeline, on the full-resolution frame:
 *  1. Felt mask from the felt HSV (mean ± tolerance).
 *  2. Table region: the table polygon if the caller knows it (a table pose), else the filled
 *     outline of the largest felt area.
 *  3. Islands = table region AND NOT felt, lightly opened to drop pixel noise.
 *  4. Each island is judged by [BallIslandRules.judge] against the ball radius expected at
 *     that spot; touching pairs are split in two.
 *  5. Each ball is named by [BallIslandRules.classify] from its white / dark pixel shares.
 *
 * This replaces a morphological-closing approach that could never work: it closed the felt
 * mask with a kernel about 0.8 ball radii wide, but a ball is a hole two radii wide, so the ball
 * holes survived the closing and only slivers of glare were ever found. It also ran on a frame
 * shrunk to a quarter, where a ball is about two pixels across.
 *
 * Not thread-safe: reuses its scratch Mats. One instance per processing thread.
 */
class CvBallDetector {

    data class Detection(
        /** Ball centre in the input frame's pixels. */
        val center: PointF,
        /** Ball radius in the input frame's pixels. */
        val radius: Float,
        val type: BallType,
        val confidence: Float,
    )

    private val feltMask = Mat()
    private val regionMask = Mat()
    private val islandMask = Mat()
    private val labels = Mat()
    private val stats = Mat()
    private val centroids = Mat()
    private val scratch = Mat()
    private val openKernel: Mat by lazy {
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
    }

    /**
     * @param bgrMat full-resolution BGR frame
     * @param hsvMat HSV of the same frame (OpenCV scale: H 0-180, S and V 0-255)
     * @param feltHsv felt mean in OpenCV HSV scale
     * @param feltStdDev per-channel felt spread, same scale
     * @param tablePolygon the playing surface's corners in frame pixels, when a table pose is
     *   known; null falls back to the largest felt area
     * @param radiusAt ball radius in frame pixels expected at a frame point, when a table pose is
     *   known; null falls back to an estimate from the visible surface area
     */
    fun detect(
        bgrMat: Mat,
        hsvMat: Mat,
        feltHsv: FloatArray,
        feltStdDev: FloatArray,
        tablePolygon: List<PointF>? = null,
        radiusAt: ((Float, Float) -> Float)? = null,
    ): List<Detection> {
        if (bgrMat.empty() || hsvMat.empty()) return emptyList()
        if (bgrMat.rows() != hsvMat.rows() || bgrMat.cols() != hsvMat.cols()) return emptyList()

        buildFeltMask(hsvMat, feltHsv, feltStdDev)
        val surfaceArea = buildRegionMask(tablePolygon) ?: return emptyList()
        val fallbackRadius = BallIslandRules.fallbackRadius(surfaceArea)

        // Islands: inside the table, not felt.
        Core.bitwise_not(feltMask, scratch)
        Core.bitwise_and(scratch, regionMask, islandMask)
        Imgproc.morphologyEx(islandMask, islandMask, Imgproc.MORPH_OPEN, openKernel)

        val count = Imgproc.connectedComponentsWithStats(islandMask, labels, stats, centroids, 8, CvType.CV_32S)
        if (count <= 1) return emptyList()

        val results = ArrayList<Detection>()
        for (label in 1 until count) {
            val area = stats.get(label, Imgproc.CC_STAT_AREA)[0].toInt()
            val x = stats.get(label, Imgproc.CC_STAT_LEFT)[0].toInt()
            val y = stats.get(label, Imgproc.CC_STAT_TOP)[0].toInt()
            val w = stats.get(label, Imgproc.CC_STAT_WIDTH)[0].toInt()
            val h = stats.get(label, Imgproc.CC_STAT_HEIGHT)[0].toInt()
            val cx = centroids.get(label, 0)[0].toFloat()
            val cy = centroids.get(label, 1)[0].toFloat()

            val expected = radiusAt?.invoke(cx, cy)?.takeIf { it > 0f } ?: fallbackRadius
            val confidence = (area.toFloat() / (w * h) / 0.785f).coerceIn(0f, 1f)

            when (BallIslandRules.judge(area, w, h, expected)) {
                BallIslandRules.Verdict.Reject -> Unit
                BallIslandRules.Verdict.Single ->
                    results += ball(hsvMat, cx, cy, expected, confidence)
                BallIslandRules.Verdict.Pair -> {
                    // Two touching balls: split along the island's principal axis.
                    val m = islandMoments(label, x, y, w, h)
                    val c = BallIslandRules.splitPair(cx, cy, m.mu20, m.mu02, m.mu11, expected)
                    results += ball(hsvMat, c[0], c[1], expected, confidence)
                    results += ball(hsvMat, c[2], c[3], expected, confidence)
                }
            }
        }
        return results
    }

    /** Second moments of one labelled island, from its bounding-box crop of [labels]. */
    private fun islandMoments(label: Int, x: Int, y: Int, w: Int, h: Int): org.opencv.imgproc.Moments {
        val crop = labels.submat(CvRect(x, y, w, h))
        try {
            Core.compare(crop, Scalar(label.toDouble()), scratch, Core.CMP_EQ)
            return Imgproc.moments(scratch, true)
        } finally {
            crop.release()
        }
    }

    private fun buildFeltMask(hsvMat: Mat, felt: FloatArray, sd: FloatArray) {
        // Empirically tuned spread; hue is held tighter than saturation/value, which move with
        // shadow and glare across a table. The S/V floor is low so dim or dark felt still masks.
        val k = 2.5
        val lower = Scalar(
            max(0.0, felt[0] - k * sd[0] - 5.0),
            max(25.0, felt[1] - k * sd[1]),
            max(25.0, felt[2] - k * sd[2]),
        )
        val upper = Scalar(
            min(180.0, felt[0] + k * sd[0] + 5.0),
            min(255.0, felt[1] + k * sd[1]),
            min(255.0, felt[2] + k * sd[2]),
        )
        Core.inRange(hsvMat, lower, upper, feltMask)
    }

    /**
     * Fills [regionMask] with the playing surface. Returns its area in pixels, or null when
     * there is no plausible table in view.
     */
    private fun buildRegionMask(tablePolygon: List<PointF>?): Double? {
        regionMask.create(feltMask.size(), CvType.CV_8UC1)
        regionMask.setTo(Scalar(0.0))

        val outline: MatOfPoint = if (tablePolygon != null && tablePolygon.size >= 3) {
            MatOfPoint(*tablePolygon.map { Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray())
        } else {
            largestFeltOutline() ?: return null
        }
        try {
            Imgproc.fillPoly(regionMask, listOf(outline), Scalar(255.0))
            // Pull in off the cushion line so rail edges don't read as islands.
            Imgproc.erode(regionMask, regionMask, openKernel, Point(-1.0, -1.0), 2)
        } finally {
            outline.release()
        }
        val area = Core.countNonZero(regionMask).toDouble()
        return if (area < feltMask.total() * MIN_SURFACE_FRACTION) null else area
    }

    /** Convex hull of the largest felt blob: the visible playing surface, ball holes and all. */
    private fun largestFeltOutline(): MatOfPoint? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        try {
            Imgproc.findContours(feltMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            val largest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return null
            val hullIdx = MatOfInt()
            try {
                Imgproc.convexHull(largest, hullIdx)
                val pts = largest.toArray()
                return MatOfPoint(*hullIdx.toArray().map { pts[it] }.toTypedArray())
            } finally {
                hullIdx.release()
            }
        } finally {
            hierarchy.release()
            contours.forEach { it.release() }
        }
    }

    private fun ball(hsv: Mat, cx: Float, cy: Float, r: Float, confidence: Float) =
        Detection(PointF(cx, cy), r, classify(hsv, cx, cy, r), confidence)

    /** White / dark pixel shares inside 85% of the ball's radius (the rim is felt-tinted). */
    private fun classify(hsv: Mat, cx: Float, cy: Float, r: Float): BallType {
        if (r < BallIslandRules.MIN_CLASSIFY_RADIUS_PX) return BallType.UNKNOWN
        val rr = r * 0.85f
        val x0 = (cx - rr).toInt().coerceIn(0, hsv.cols() - 1)
        val y0 = (cy - rr).toInt().coerceIn(0, hsv.rows() - 1)
        val x1 = (cx + rr).toInt().coerceIn(x0 + 1, hsv.cols())
        val y1 = (cy + rr).toInt().coerceIn(y0 + 1, hsv.rows())
        val roi = hsv.submat(CvRect(x0, y0, x1 - x0, y1 - y0))
        try {
            // One bulk read (a submat is not contiguous, so copy it first) instead of a JNI
            // call per pixel.
            roi.copyTo(scratch)
            val cols = scratch.cols()
            val px = ByteArray((scratch.total() * 3).toInt())
            scratch.get(0, 0, px)
            var total = 0; var white = 0; var dark = 0
            val r2 = rr * rr
            for (y in 0 until scratch.rows()) for (x in 0 until cols) {
                val dx = x0 + x - cx; val dy = y0 + y - cy
                if (dx * dx + dy * dy > r2) continue
                val i = (y * cols + x) * 3
                val s = px[i + 1].toInt() and 0xFF
                val v = px[i + 2].toInt() and 0xFF
                total++
                if (v < DARK_V) dark++ else if (s < WHITE_MAX_S && v > WHITE_MIN_V) white++
            }
            if (total == 0) return BallType.UNKNOWN
            return when (BallIslandRules.classify(white / total.toFloat(), dark / total.toFloat(), r)) {
                BallIslandRules.BallKind.CUE -> BallType.CUE
                BallIslandRules.BallKind.EIGHT -> BallType.EIGHT
                BallIslandRules.BallKind.STRIPE -> BallType.STRIPE
                BallIslandRules.BallKind.SOLID -> BallType.SOLID
                BallIslandRules.BallKind.UNKNOWN -> BallType.UNKNOWN
            }
        } finally {
            roi.release()
        }
    }

    fun release() {
        feltMask.release()
        regionMask.release()
        islandMask.release()
        labels.release()
        stats.release()
        centroids.release()
        scratch.release()
        openKernel.release()
    }

    private companion object {
        /** Less felt than this fraction of the frame: no table in view. */
        const val MIN_SURFACE_FRACTION = 0.05
        const val DARK_V = 60
        const val WHITE_MAX_S = 60
        const val WHITE_MIN_V = 150
    }
}
