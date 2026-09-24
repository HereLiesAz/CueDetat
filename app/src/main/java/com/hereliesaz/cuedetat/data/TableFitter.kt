package com.hereliesaz.cuedetat.data

import android.graphics.Matrix
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.PoseSearch
import com.hereliesaz.cuedetat.domain.TableOrientationLearner
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.exp
import kotlin.math.ln

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
 * camera frame (intersection over union), plus a pull toward the remembered orientation
 * ([TableOrientationLearner]) in proportion to how much that memory is trusted. [PoseSearch]
 * does the searching, from where the user put the table, from the remembered orientation, and
 * from the user's placement turned a quarter turn (the commonest gross misplacement).
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
    )

    private val feltMask = Mat()
    private val quadMask = Mat()
    private val scratch = Mat()
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

        val w0Inverse = Matrix()
        if (!worldMatrix(current).invert(w0Inverse)) return null
        // A = M0 · W0⁻¹: everything in the screen matrix that the pose search doesn't touch.
        val a = Matrix(w0Inverse).apply { postConcat(currentMatrix) }

        val cost = { p: DoubleArray ->
            val pose = Pose(p[0].toFloat(), p[1].toFloat(), p[2].toFloat(), exp(p[3]).toFloat())
            (1.0 - iou(pose, a, viewToFrame, corners)) + priorPenalty(pose, prior)
        }

        val starts = buildList {
            add(current)
            add(current.copy(rotationDeg = current.rotationDeg + 90f))
            if (prior != null && prior.confidence >= PRIOR_START_CONFIDENCE) {
                add(current.copy(rotationDeg = prior.rotationDeg, zoom = prior.zoom))
            }
        }
        val best = starts.map { s ->
            PoseSearch.minimize(
                start = doubleArrayOf(s.offsetX.toDouble(), s.offsetY.toDouble(), s.rotationDeg.toDouble(), ln(s.zoom.toDouble())),
                steps = doubleArrayOf(OFFSET_STEP, OFFSET_STEP, ROTATION_STEP, LOG_ZOOM_STEP),
                minSteps = doubleArrayOf(OFFSET_MIN_STEP, OFFSET_MIN_STEP, ROTATION_MIN_STEP, LOG_ZOOM_MIN_STEP),
                maxEvaluations = EVALUATIONS_PER_START,
                cost = cost,
            )
        }.minByOrNull { it.cost } ?: return null

        val pose = Pose(
            best.params[0].toFloat(), best.params[1].toFloat(),
            TableOrientationLearner.normalize180(best.params[2].toFloat()), exp(best.params[3]).toFloat(),
        )
        val quad = viewQuad(pose, a, corners)
        return Fit(pose, iou(pose, a, viewToFrame, corners), quad)
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
        val k = 2.5
        Core.inRange(
            hsv,
            Scalar(maxOf(0.0, felt[0] - k * sd[0] - 5.0), maxOf(25.0, felt[1] - k * sd[1]), maxOf(25.0, felt[2] - k * sd[2])),
            Scalar(minOf(180.0, felt[0] + k * sd[0] + 5.0), minOf(255.0, felt[1] + k * sd[1]), minOf(255.0, felt[2] + k * sd[2])),
            feltMask,
        )
        // Close ball-sized holes so balls don't count against the table.
        Imgproc.morphologyEx(feltMask, feltMask, Imgproc.MORPH_CLOSE, closeKernel, Point(-1.0, -1.0), 2)
    }

    fun release() {
        feltMask.release(); quadMask.release(); scratch.release(); closeKernel.release()
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
        const val PRIOR_START_CONFIDENCE = 0.3f
        const val PRIOR_WEIGHT = 0.05
        const val ROTATION_TOLERANCE_DEG = 15.0
        const val LOG_ZOOM_TOLERANCE = 0.25
    }
}
