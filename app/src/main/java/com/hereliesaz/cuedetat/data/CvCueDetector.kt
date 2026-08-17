// app/src/main/java/com/hereliesaz/cuedetat/data/CvCueDetector.kt
package com.hereliesaz.cuedetat.data

import android.graphics.PointF
import android.graphics.Rect
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Classical-CV cue-stick detector, the "cue" sibling of [CvBallDetector].
 *
 * There is no cue-trained ML head (see PoolDetection.kt's KDoc) so this is
 * the only source of cue-stick candidates. A cue stick is modeled as a long,
 * straight, narrow object lying across the (mostly uniform-color) felt:
 *
 *  1. Build the felt mask from sampled HSV (mean +/- stdDev tolerance) via
 *     the shared [FeltMask] helper — the same prior [CvBallDetector] uses.
 *  2. Invert it: non-felt pixels are candidate object pixels (balls, cue,
 *     hands, rails, etc). Dilate slightly so edges right at the felt/object
 *     boundary survive the mask.
 *  3. Run Canny edge detection on the frame, then restrict those edges to
 *     the dilated non-felt mask so felt texture/shadow noise can't produce
 *     spurious lines.
 *  4. Run a probabilistic Hough line transform (HoughLinesP) on the masked
 *     edges.
 *  5. Merge nearly-collinear, nearby segments (a single cue's edge is often
 *     fragmented into several pieces by specular highlights) into one
 *     candidate, then keep only merged segments meeting a minimum length
 *     (relative to the frame diagonal) and reject ones that hug a frame
 *     border their whole length — a cheap, conservative filter against the
 *     table rail/cushion running along the edge of the visible frame.
 *
 * Honesty note: this is inherently less reliable than a trained detector.
 * Rail cushions, bridges/mechanical aids, and straight shadow edges can all
 * produce long straight non-felt-adjacent lines. Thresholds below favor
 * conservatism (long minimum length, tight angle/gap tolerances for
 * merging, border-hugging rejection) because a false-positive "cue"
 * actively misleads the aiming overlay, which is worse than an occasional
 * miss.
 */
class CvCueDetector {

    data class Detection(
        val start: PointF,
        val end: PointF,
        val rect: Rect,
        val confidence: Float,
    )

    private val feltMask = Mat()
    private val nonFeltMask = Mat()
    private val dilatedMask = Mat()
    private val gray = Mat()
    private val edges = Mat()
    private val maskedEdges = Mat()
    private val linesMat = Mat()
    private var dilateKernel: Mat? = null
    private var dilateKernelSize = -1

    /**
     * @param bgrMat full-resolution BGR frame
     * @param hsvMat full-resolution HSV frame (same dimensions)
     * @param feltHsv mean felt HSV in OpenCV space (H 0-180, S 0-255, V 0-255)
     * @param feltStdDev per-channel std dev for inRange tolerance
     */
    fun detect(
        bgrMat: Mat,
        hsvMat: Mat,
        feltHsv: FloatArray,
        feltStdDev: FloatArray,
    ): List<Detection> {
        if (bgrMat.empty() || hsvMat.empty()) return emptyList()
        if (bgrMat.rows() != hsvMat.rows() || bgrMat.cols() != hsvMat.cols()) return emptyList()

        val width = bgrMat.cols()
        val height = bgrMat.rows()
        val frameDiag = hypot(width.toDouble(), height.toDouble())
        val minLineLength = (frameDiag * MIN_LENGTH_FRACTION).coerceAtLeast(MIN_LENGTH_PX)

        FeltMask.build(hsvMat, feltHsv, feltStdDev, feltMask)
        Core.bitwise_not(feltMask, nonFeltMask)

        val kernelSize = max(5.0, frameDiag * 0.01).toInt().let { if (it % 2 == 0) it + 1 else it }
        if (dilateKernel == null || dilateKernelSize != kernelSize) {
            dilateKernel?.release()
            dilateKernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE, Size(kernelSize.toDouble(), kernelSize.toDouble())
            )
            dilateKernelSize = kernelSize
        }
        Imgproc.dilate(nonFeltMask, dilatedMask, dilateKernel)

        Imgproc.cvtColor(bgrMat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(gray, edges, 50.0, 150.0)
        Core.bitwise_and(edges, dilatedMask, maskedEdges)

        Imgproc.HoughLinesP(
            maskedEdges, linesMat,
            1.0, PI / 180.0,
            HOUGH_THRESHOLD,
            minLineLength * SEED_MIN_LENGTH_FRACTION,
            MAX_LINE_GAP_PX,
        )
        if (linesMat.empty() || linesMat.rows() == 0) return emptyList()

        val rawSegments = ArrayList<FloatArray>(linesMat.rows())
        for (i in 0 until linesMat.rows()) {
            val v = linesMat.get(i, 0)
            rawSegments.add(floatArrayOf(v[0].toFloat(), v[1].toFloat(), v[2].toFloat(), v[3].toFloat()))
        }

        // Cap the working set before the O(n^2)-ish merge pass: keep the
        // longest candidates, a cue is rare enough that dozens of short
        // fragments are noise, not signal.
        val seeds = rawSegments
            .sortedByDescending { segLength(it) }
            .take(MAX_SEED_SEGMENTS)
            .toMutableList()

        val merged = mergeCollinearSegments(seeds)

        return merged.mapNotNull { seg ->
            val length = segLength(seg)
            if (length < minLineLength) return@mapNotNull null
            if (hugsBorder(seg, width, height)) return@mapNotNull null

            val x1 = seg[0]; val y1 = seg[1]; val x2 = seg[2]; val y2 = seg[3]
            val pad = max(4f, (length * 0.03).toFloat())
            val left = min(x1, x2) - pad
            val top = min(y1, y2) - pad
            val right = max(x1, x2) + pad
            val bottom = max(y1, y2) + pad

            val rect = Rect(
                left.toInt().coerceAtLeast(0),
                top.toInt().coerceAtLeast(0),
                right.toInt().coerceAtMost(width),
                bottom.toInt().coerceAtMost(height),
            )
            if (rect.width() <= 0 || rect.height() <= 0) return@mapNotNull null

            val confidence = (length / frameDiag).toFloat().coerceIn(0f, 1f)

            Detection(
                start = PointF(x1, y1),
                end = PointF(x2, y2),
                rect = rect,
                confidence = confidence,
            )
        }
    }

    private fun segLength(s: FloatArray): Double =
        hypot((s[2] - s[0]).toDouble(), (s[3] - s[1]).toDouble())

    private fun segAngle(s: FloatArray): Double {
        var a = atan2((s[3] - s[1]).toDouble(), (s[2] - s[0]).toDouble())
        if (a < 0) a += PI
        return a
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(a - b) % PI
        return if (d > PI / 2) PI - d else d
    }

    /** Perpendicular distance from (px, py) to the infinite line through segment [s]. */
    private fun perpDistance(px: Float, py: Float, s: FloatArray): Double {
        val x1 = s[0]; val y1 = s[1]; val x2 = s[2]; val y2 = s[3]
        val len = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())
        if (len < 1e-6) return hypot((px - x1).toDouble(), (py - y1).toDouble())
        return abs((x2 - x1) * (y1 - py) - (x1 - px) * (y2 - y1)) / len
    }

    private fun endpointGap(a: FloatArray, b: FloatArray): Double {
        var minGap = Double.MAX_VALUE
        val ax = floatArrayOf(a[0], a[2])
        val ay = floatArrayOf(a[1], a[3])
        val bx = floatArrayOf(b[0], b[2])
        val by = floatArrayOf(b[1], b[3])
        for (i in 0..1) for (j in 0..1) {
            val d = hypot((ax[i] - bx[j]).toDouble(), (ay[i] - by[j]).toDouble())
            if (d < minGap) minGap = d
        }
        return minGap
    }

    /**
     * Greedily fuses segments that are nearly collinear (similar angle, both
     * endpoints of one lie close to the other's line) and close together
     * (endpoint gap within tolerance) into a single segment spanning their
     * two most distant endpoints. Repeats until no more merges happen.
     */
    private fun mergeCollinearSegments(segments: MutableList<FloatArray>): List<FloatArray> {
        var changed = true
        while (changed) {
            changed = false
            outer@ for (i in segments.indices) {
                for (j in segments.indices) {
                    if (i == j) continue
                    val a = segments[i]
                    val b = segments[j]
                    if (angleDiff(segAngle(a), segAngle(b)) > ANGLE_TOL_RAD) continue
                    if (perpDistance(b[0], b[1], a) > MAX_PERP_DIST_PX) continue
                    if (perpDistance(b[2], b[3], a) > MAX_PERP_DIST_PX) continue
                    if (endpointGap(a, b) > MAX_MERGE_GAP_PX) continue

                    val pts = arrayOf(
                        floatArrayOf(a[0], a[1]), floatArrayOf(a[2], a[3]),
                        floatArrayOf(b[0], b[1]), floatArrayOf(b[2], b[3]),
                    )
                    var bestP = pts[0]
                    var bestQ = pts[1]
                    var bestDist = -1.0
                    for (p in pts) for (q in pts) {
                        val d = hypot((p[0] - q[0]).toDouble(), (p[1] - q[1]).toDouble())
                        if (d > bestDist) {
                            bestDist = d
                            bestP = p
                            bestQ = q
                        }
                    }
                    segments[i] = floatArrayOf(bestP[0], bestP[1], bestQ[0], bestQ[1])
                    segments.removeAt(j)
                    changed = true
                    break@outer
                }
            }
        }
        return segments
    }

    /**
     * Rejects segments that run parallel to a frame border and stay within
     * [BORDER_MARGIN_PX] of it along their whole length — a cheap guard
     * against the visible table rail/cushion, which commonly hugs one edge
     * of the frame during AR setup and would otherwise read as a very long,
     * very straight "cue".
     */
    private fun hugsBorder(seg: FloatArray, width: Int, height: Int): Boolean {
        val x1 = seg[0]; val y1 = seg[1]; val x2 = seg[2]; val y2 = seg[3]
        val nearLeft = x1 < BORDER_MARGIN_PX && x2 < BORDER_MARGIN_PX
        val nearRight = x1 > width - BORDER_MARGIN_PX && x2 > width - BORDER_MARGIN_PX
        val nearTop = y1 < BORDER_MARGIN_PX && y2 < BORDER_MARGIN_PX
        val nearBottom = y1 > height - BORDER_MARGIN_PX && y2 > height - BORDER_MARGIN_PX
        return nearLeft || nearRight || nearTop || nearBottom
    }

    fun release() {
        feltMask.release()
        nonFeltMask.release()
        dilatedMask.release()
        gray.release()
        edges.release()
        maskedEdges.release()
        linesMat.release()
        dilateKernel?.release()
        dilateKernel = null
    }

    private companion object {
        const val MIN_LENGTH_FRACTION = 0.18
        const val MIN_LENGTH_PX = 80.0
        const val SEED_MIN_LENGTH_FRACTION = 0.4
        const val HOUGH_THRESHOLD = 60
        const val MAX_LINE_GAP_PX = 20.0
        const val MAX_SEED_SEGMENTS = 40
        const val ANGLE_TOL_RAD = 0.09 // ~5 degrees
        const val MAX_PERP_DIST_PX = 6f
        const val MAX_MERGE_GAP_PX = 40f
        const val BORDER_MARGIN_PX = 12f
    }
}
