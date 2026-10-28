package com.hereliesaz.cuedetat.data

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.PoseSearch
import com.hereliesaz.cuedetat.domain.TableOrientationLearner
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import com.hereliesaz.cuedetat.domain.TableFitQuality
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

/**
 * Finds the virtual-table pose that best covers the felt the camera sees.
 *
 * The virtual table's on-screen pose is four numbers: pan (x, y), rotation and zoom; the phone's
 * tilt comes from its sensors and is taken as given. The screen matrix is
 * `M(p) = A · W(p)`, where `W(p) = translate · rotate · scale` and `A` (camera tilt, centring,
 * height slider) does not depend on `p`. So `A = M0 · W(p0)⁻¹` is read off the current state and
 * any candidate pose can be projected exactly, without re-deriving the perspective.
 *
 * Each candidate is scored by how well the projected table outline matches the felt mask in the
 * camera frame (intersection over union), by how close the outline's *edges* lie to the felt's
 * boundary (a distance transform of the mask edge: overlap alone barely notices an outline that is
 * a few pixels out, the edge term is what pins down the heading), plus a pull toward the
 * remembered orientation ([TableOrientationLearner]) in proportion to how much that memory is
 * trusted. [PoseSearch] does the searching, from where the user put the table, from the
 * remembered orientation, and from the user's placement turned a quarter turn (the commonest gross
 * misplacement).
 *
 * Two things make the fit good rather than merely close. After the first answer the felt colour is
 * re-measured inside the fitted outline (the colour the camera really sees under this light, not
 * the one sampled earlier) and the fit is refined against the better mask. And the fit says how
 * sure it is of the *heading* ([Fit.orientationConfidence]): how sharply the score worsens when
 * the table is turned a few degrees, and by how much the best long-axis reading beats the
 * quarter-turned one.
 *
 * Only what is in view counts, so a table half off-screen still fits.
 *
 * Not thread-safe: reuses scratch Mats.
 */
class TableFitter {

    /** Pose parameters in the units of `CueDetatState`: pixels, degrees, zoom factor. */
    data class Pose(val offsetX: Float, val offsetY: Float, val rotationDeg: Float, val zoom: Float)

    data class Fit(
        val pose: Pose,
        /** Felt / table-outline overlap, 0..1. */
        val iou: Float,
        /** The fitted table's corners on screen (view pixels), TL, TR, BR, BL. */
        val viewQuad: List<PointF>,
        /**
         * 0..1: how sure the heading is (not the position). Low when the table is mostly out of
         * frame, or when the quarter-turned reading scores nearly as well as the chosen one.
         */
        val orientationConfidence: Float = 1f,
        /** 0..1: how little the outline moved over the last few fits; set by the caller. */
        val stability: Float = 0f,
    ) {
        /** One number for "is this fit to be believed" ([TableFitQuality]). */
        val quality: Float get() = TableFitQuality.of(iou, stability)
    }

    private val feltMask = Mat()
    private val quadMask = Mat()
    private val scratch = Mat()
    private val edgeMask = Mat()
    private val edgeInverse = Mat()
    private val edgeDistance = Mat()
    private var edgeData = FloatArray(0)
    private var edgeW = 0
    private var edgeH = 0
    private val innerMask = Mat()
    private val looseMask = Mat()
    private val hueScratch = Mat()
    private val meanBuf = MatOfDouble()
    private val stdBuf = MatOfDouble()
    private val gradientKernel: Mat by lazy {
        Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    }
    private val closeKernel: Mat by lazy {
        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
    }

    /**
     * @param hsv camera frame in OpenCV HSV (small is fine; ~160 px across is plenty)
     * @param frameToView maps [hsv] pixels to view pixels
     * @param current the table's current pose
     * @param currentMatrix the table's current logical -> view matrix (`pitchMatrix`)
     * @param corners logical table corners, TL, TR, BR, BL
     * @param prior remembered orientation, if any
     * @return the best fit, or null when too little felt is in view to judge
     */
    fun fit(
        hsv: Mat,
        feltHsv: FloatArray,
        feltStdDev: FloatArray,
        frameToView: Matrix,
        current: Pose,
        currentMatrix: Matrix,
        corners: List<PointF>,
        prior: TableOrientationLearner.Prediction?,
    ): Fit? {
        if (hsv.empty() || corners.size != 4) return null
        val viewToFrame = Matrix()
        if (!frameToView.invert(viewToFrame)) return null

        buildFeltMask(hsv, feltHsv, feltStdDev)
        val feltPixels = Core.countNonZero(feltMask)
        if (feltPixels < hsv.total() * MIN_FELT_FRACTION) return null
        buildEdgeField()

        val w0Inverse = Matrix()
        if (!worldMatrix(current).invert(w0Inverse)) return null
        // A = M0 · W0⁻¹: everything in the screen matrix that the pose search doesn't touch.
        val a = Matrix(w0Inverse).apply { postConcat(currentMatrix) }

        var best = search(current, a, viewToFrame, corners, prior, EVALUATIONS_PER_START)
            ?: return null

        // Re-measure the felt inside the answer and refine against that mask. Kept only if it
        // really is better, so a bad re-measurement (a hand, a glare) can't make the fit worse.
        val before = shapeCost(best.pose, a, viewToFrame, corners)
        var improved = false
        if (remeasureFelt(hsv, best.pose, a, viewToFrame, corners, feltHsv, feltStdDev)) {
            buildEdgeField()
            val refined = search(best.pose, a, viewToFrame, corners, prior, REFINE_EVALUATIONS, single = true)
            // Scored on the new mask against the old score on the old one: a good re-measurement
            // lets the true outline score better, a bad one (a hand, glare) makes everything worse.
            if (refined != null && shapeCost(refined.pose, a, viewToFrame, corners) <= before) {
                best = refined
                improved = true
            }
        }
        if (!improved) {
            buildFeltMask(hsv, feltHsv, feltStdDev)
            buildEdgeField()
        }

        val pose = best.pose
        return Fit(
            pose = pose,
            iou = iou(pose, a, viewToFrame, corners).toFloat(),
            viewQuad = viewQuad(pose, a, corners),
            orientationConfidence = orientationConfidence(best, a, viewToFrame, corners, prior),
        )
    }

    /** A converged pose and what it cost (image and prior together). */
    private class Found(val pose: Pose, val cost: Double)

    private fun search(
        start: Pose, a: Matrix, viewToFrame: Matrix, corners: List<PointF>,
        prior: TableOrientationLearner.Prediction?, evaluations: Int, single: Boolean = false,
    ): Found? {
        val cost = { p: DoubleArray ->
            val pose = Pose(p[0].toFloat(), p[1].toFloat(), p[2].toFloat(), exp(p[3]).toFloat())
            shapeCost(pose, a, viewToFrame, corners) + priorPenalty(pose, prior)
        }
        val starts = if (single) listOf(start) else buildList {
            add(start)
            add(start.copy(rotationDeg = start.rotationDeg + 90f))
            if (prior != null && prior.confidence >= PRIOR_START_CONFIDENCE) {
                add(start.copy(rotationDeg = prior.rotationDeg, zoom = prior.zoom))
            }
        }
        val results = starts.map { s ->
            PoseSearch.minimize(
                start = doubleArrayOf(s.offsetX.toDouble(), s.offsetY.toDouble(), s.rotationDeg.toDouble(), ln(s.zoom.toDouble())),
                steps = doubleArrayOf(OFFSET_STEP, OFFSET_STEP, ROTATION_STEP, LOG_ZOOM_STEP),
                minSteps = doubleArrayOf(OFFSET_MIN_STEP, OFFSET_MIN_STEP, ROTATION_MIN_STEP, LOG_ZOOM_MIN_STEP),
                maxEvaluations = evaluations,
                cost = cost,
            )
        }
        val best = results.minByOrNull { it.cost } ?: return null
        val pose = Pose(
            best.params[0].toFloat(), best.params[1].toFloat(),
            TableOrientationLearner.normalize180(best.params[2].toFloat()), exp(best.params[3]).toFloat(),
        )
        return Found(pose, best.cost)
    }

    /**
     * 0..1. Two doubts, the worse one wins: the score barely changes when the table is turned a
     * few degrees (nothing pins the heading: too little of it in frame), or the quarter-turned
     * reading scores nearly as well (the long axis is a coin toss). The quarter-turned reading is
     * found by searching on from the answer turned 90 degrees; if that search slides back to the
     * answer there is no rival reading and the long axis is not in doubt.
     */
    private fun orientationConfidence(
        found: Found, a: Matrix, viewToFrame: Matrix, corners: List<PointF>,
        prior: TableOrientationLearner.Prediction?,
    ): Float {
        val here = shapeCost(found.pose, a, viewToFrame, corners)
        fun turned(deg: Float) = shapeCost(found.pose.copy(rotationDeg = found.pose.rotationDeg + deg), a, viewToFrame, corners)
        val sharpness = min(turned(ORIENT_PROBE_DEG) - here, turned(-ORIENT_PROBE_DEG) - here)
        val bySharpness = (sharpness / ORIENT_SHARP_FULL).coerceIn(0.0, 1.0)
        val rival = search(
            found.pose.copy(rotationDeg = found.pose.rotationDeg + 90f), a, viewToFrame, corners, prior,
            RIVAL_EVALUATIONS, single = true,
        )
        val rivalIsDistinct = rival != null &&
            abs(TableOrientationLearner.diff180(rival.pose.rotationDeg, found.pose.rotationDeg)) > 45f
        val byMargin = if (rivalIsDistinct) ((rival!!.cost - found.cost) / ORIENT_MARGIN_FULL).coerceIn(0.0, 1.0) else 1.0
        return min(bySharpness, byMargin).toFloat()
    }

    /** Overlap plus edge-distance, without the prior: the part of the cost the image decides. */
    private fun shapeCost(p: Pose, a: Matrix, viewToFrame: Matrix, corners: List<PointF>): Double =
        (1.0 - iou(p, a, viewToFrame, corners)) + EDGE_WEIGHT * edgeResidual(p, a, viewToFrame, corners)

    /**
     * Distance from the felt mask's boundary, per frame pixel (0 on the boundary), copied out of
     * OpenCV once so a candidate can be scored by sampling an array rather than by JNI calls.
     */
    private fun buildEdgeField() {
        Imgproc.morphologyEx(feltMask, edgeMask, Imgproc.MORPH_GRADIENT, gradientKernel)
        Core.bitwise_not(edgeMask, edgeInverse)
        Imgproc.distanceTransform(edgeInverse, edgeDistance, DIST_L2, 3)
        edgeW = edgeDistance.cols()
        edgeH = edgeDistance.rows()
        val n = edgeW * edgeH
        if (edgeData.size != n) edgeData = FloatArray(n)
        edgeDistance.get(0, 0, edgeData)
    }

    /**
     * Mean distance of the outline's edges to the felt boundary, as a fraction of [EDGE_CAP_PX]
     * (0 = on it). Only the parts of the outline inside the frame count; with fewer than
     * [MIN_EDGE_SAMPLES] of them there is nothing to judge by and it reads 0.
     */
    private fun edgeResidual(p: Pose, a: Matrix, viewToFrame: Matrix, corners: List<PointF>): Double {
        val pts = FloatArray(8)
        viewQuad(p, a, corners).forEachIndexed { i, c -> pts[i * 2] = c.x; pts[i * 2 + 1] = c.y }
        viewToFrame.mapPoints(pts)
        var sum = 0.0
        var n = 0
        for (e in 0 until 4) {
            val x0 = pts[e * 2]; val y0 = pts[e * 2 + 1]
            val x1 = pts[((e + 1) % 4) * 2]; val y1 = pts[((e + 1) % 4) * 2 + 1]
            for (k in 0 until EDGE_SAMPLES) {
                val t = k / (EDGE_SAMPLES - 1f)
                val x = (x0 + (x1 - x0) * t).toInt()
                val y = (y0 + (y1 - y0) * t).toInt()
                if (x < 1 || y < 1 || x >= edgeW - 1 || y >= edgeH - 1) continue
                sum += min(edgeData[y * edgeW + x].toDouble(), EDGE_CAP_PX)
                n++
            }
        }
        return if (n < MIN_EDGE_SAMPLES) 0.0 else sum / n / EDGE_CAP_PX
    }

    /**
     * Re-measures the felt colour from the middle of the fitted outline and rebuilds the felt mask
     * from it. Returns false (mask untouched) when too little felt-like colour is in there to
     * measure.
     */
    private fun remeasureFelt(
        hsv: Mat, pose: Pose, a: Matrix, viewToFrame: Matrix, corners: List<PointF>,
        oldFelt: FloatArray, oldSd: FloatArray,
    ): Boolean {
        val pts = FloatArray(8)
        viewQuad(pose, a, corners).forEachIndexed { i, c -> pts[i * 2] = c.x; pts[i * 2 + 1] = c.y }
        viewToFrame.mapPoints(pts)
        // Shrink toward the centre so cushions, pockets and the outline's own error stay out.
        val cx = (pts[0] + pts[2] + pts[4] + pts[6]) / 4f
        val cy = (pts[1] + pts[3] + pts[5] + pts[7]) / 4f
        val poly = MatOfPoint(*Array(4) {
            Point(
                (cx + (pts[it * 2] - cx) * INNER_FRACTION).toDouble(),
                (cy + (pts[it * 2 + 1] - cy) * INNER_FRACTION).toDouble(),
            )
        })
        innerMask.create(feltMask.size(), CvType.CV_8UC1)
        innerMask.setTo(Scalar(0.0))
        try {
            Imgproc.fillPoly(innerMask, listOf(poly), Scalar(255.0))
        } finally {
            poly.release()
        }
        // Inside the outline AND felt-like under a generous threshold: balls and glare drop out.
        buildFeltMaskInto(looseMask, hsv, oldFelt, oldSd, LOOSE_K)
        Core.bitwise_and(innerMask, looseMask, scratch)
        if (Core.countNonZero(scratch) < MIN_REMEASURE_PIXELS) return false
        Core.meanStdDev(hsv, meanBuf, stdBuf, scratch)
        val m = meanBuf.toArray()
        val sd = stdBuf.toArray()
        if (m.size < 3 || sd.size < 3) return false
        // Saturation and value average on a line; hue averages round a circle (meanStdDev's hue
        // is wrong for any cloth near the seam).
        val hue = HueMask.stats(hsv, scratch) ?: return false
        val felt = floatArrayOf(hue.mean.toFloat(), m[1].toFloat(), m[2].toFloat())
        // A measured spread can be very tight on a clean patch; keep the mask from going brittle.
        val spread = floatArrayOf(
            maxOf(hue.std.toFloat(), oldSd[0] * MIN_SD_FRACTION),
            maxOf(sd[1].toFloat(), oldSd[1] * MIN_SD_FRACTION),
            maxOf(sd[2].toFloat(), oldSd[2] * MIN_SD_FRACTION),
        )
        buildFeltMask(hsv, felt, spread)
        return true
    }

    private fun worldMatrix(p: Pose) = Matrix().apply {
        postScale(p.zoom, p.zoom)
        postRotate(p.rotationDeg)
        postTranslate(p.offsetX, p.offsetY)
    }

    private fun viewQuad(p: Pose, a: Matrix, corners: List<PointF>): List<PointF> {
        val m = worldMatrix(p).apply { postConcat(a) }
        val pts = FloatArray(8)
        corners.forEachIndexed { i, c -> pts[i * 2] = c.x; pts[i * 2 + 1] = c.y }
        m.mapPoints(pts)
        return List(4) { PointF(pts[it * 2], pts[it * 2 + 1]) }
    }

    private fun iou(p: Pose, a: Matrix, viewToFrame: Matrix, corners: List<PointF>): Double {
        val pts = FloatArray(8)
        viewQuad(p, a, corners).forEachIndexed { i, c -> pts[i * 2] = c.x; pts[i * 2 + 1] = c.y }
        viewToFrame.mapPoints(pts)
        quadMask.create(feltMask.size(), CvType.CV_8UC1)
        quadMask.setTo(Scalar(0.0))
        val poly = MatOfPoint(*Array(4) { Point(pts[it * 2].toDouble(), pts[it * 2 + 1].toDouble()) })
        try {
            Imgproc.fillPoly(quadMask, listOf(poly), Scalar(255.0))
        } finally {
            poly.release()
        }
        Core.bitwise_and(quadMask, feltMask, scratch)
        val inter = Core.countNonZero(scratch)
        Core.bitwise_or(quadMask, feltMask, scratch)
        val union = Core.countNonZero(scratch)
        return if (union == 0) 0.0 else inter.toDouble() / union
    }

    /** Pull toward the remembered orientation, scaled by how much it is trusted. */
    private fun priorPenalty(p: Pose, prior: TableOrientationLearner.Prediction?): Double {
        if (prior == null) return 0.0
        val dRot = TableOrientationLearner.diff180(p.rotationDeg, prior.rotationDeg) / ROTATION_TOLERANCE_DEG
        val dZoom = ln(p.zoom.toDouble() / prior.zoom) / LOG_ZOOM_TOLERANCE
        return PRIOR_WEIGHT * prior.confidence * (dRot * dRot + dZoom * dZoom)
    }

    private fun buildFeltMask(hsv: Mat, felt: FloatArray, sd: FloatArray) {
        buildFeltMaskInto(feltMask, hsv, felt, sd, K_SIGMA)
        // Close ball-sized holes so balls don't count against the table.
        Imgproc.morphologyEx(feltMask, feltMask, Imgproc.MORPH_CLOSE, closeKernel, Point(-1.0, -1.0), 2)
    }

    private fun buildFeltMaskInto(out: Mat, hsv: Mat, felt: FloatArray, sd: FloatArray, k: Double) {
        // Hue is a circle: a window round a red felt crosses the 0/180 seam (HueMask).
        HueMask.inRangeAround(
            hsv, felt[0].toDouble(), k * sd[0] + 5.0,
            sLo = maxOf(25.0, felt[1] - k * sd[1]), sHi = minOf(255.0, felt[1] + k * sd[1]),
            vLo = maxOf(25.0, felt[2] - k * sd[2]), vHi = minOf(255.0, felt[2] + k * sd[2]),
            out = out, scratch = hueScratch,
        )
    }

    fun release() {
        feltMask.release(); quadMask.release(); scratch.release(); closeKernel.release()
        edgeMask.release(); edgeInverse.release(); edgeDistance.release(); gradientKernel.release()
        innerMask.release(); looseMask.release(); hueScratch.release(); meanBuf.release(); stdBuf.release()
    }

    private companion object {
        const val MIN_FELT_FRACTION = 0.05
        const val OFFSET_STEP = 60.0
        const val OFFSET_MIN_STEP = 2.0
        const val ROTATION_STEP = 8.0
        const val ROTATION_MIN_STEP = 0.25
        const val LOG_ZOOM_STEP = 0.15
        const val LOG_ZOOM_MIN_STEP = 0.004
        const val EVALUATIONS_PER_START = 90
        const val REFINE_EVALUATIONS = 60
        const val RIVAL_EVALUATIONS = 60
        /** cv::DIST_L2: this OpenCV binding's Imgproc has no DIST_L2 constant. */
        const val DIST_L2 = 2
        const val K_SIGMA = 2.5
        const val LOOSE_K = 3.5
        const val INNER_FRACTION = 0.7f
        const val MIN_REMEASURE_PIXELS = 200
        const val MIN_SD_FRACTION = 0.6f
        const val EDGE_WEIGHT = 0.25
        const val EDGE_CAP_PX = 6.0
        const val EDGE_SAMPLES = 24
        const val MIN_EDGE_SAMPLES = 16
        const val ORIENT_PROBE_DEG = 3f
        const val ORIENT_SHARP_FULL = 0.03
        const val ORIENT_MARGIN_FULL = 0.12
        const val PRIOR_START_CONFIDENCE = 0.3f
        const val PRIOR_WEIGHT = 0.05
        const val ROTATION_TOLERANCE_DEG = 15.0
        const val LOG_ZOOM_TOLERANCE = 0.25
    }
}
