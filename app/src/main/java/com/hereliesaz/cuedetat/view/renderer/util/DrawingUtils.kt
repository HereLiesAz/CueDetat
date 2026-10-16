// FILE: app/src/main/java/com/hereliesaz/cuedetat/view/renderer/util/DrawingUtils.kt

package com.hereliesaz.cuedetat.view.renderer.util

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

object DrawingUtils {

    data class PerspectiveRadiusInfo(val radius: Float, val lift: Float)

    /**
     * On-screen radius of a ball of [logicalRadius] at [logicalCenter] under [matrix], and the
     * screen-up "lift" that places the ball's visual body above its on-table footprint.
     *
     * Lift = radius * |sin(pitch)| * [liftScale], where pitch is `state.effectivePitchDegrees`
     * (the pitch UpdateStateUseCase built pitchMatrix and rail lift with: AR/depth-derived when
     * available), falling back to the raw IMU `state.pitchAngle` before the first matrix pass.
     * [liftScale] lets the caller fade the lift out as the view animates to the orthographic
     * top-down layout (1 = full perspective, 0 = flat).
     */
    fun getPerspectiveRadiusAndLift(
        logicalCenter: PointF,
        logicalRadius: Float,
        state: CueDetatState,
        matrix: Matrix,
        liftScale: Float = 1f
    ): PerspectiveRadiusInfo {
        synchronized(ptsBuffer) {
            ptsBuffer[0] = logicalCenter.x
            ptsBuffer[1] = logicalCenter.y
            matrix.mapPoints(ptsBuffer)
            val centerX = ptsBuffer[0]
            val centerY = ptsBuffer[1]

            ptsBuffer[0] = logicalCenter.x + logicalRadius
            ptsBuffer[1] = logicalCenter.y
            matrix.mapPoints(ptsBuffer)
            val edgeX = ptsBuffer[0]
            val edgeY = ptsBuffer[1]

            val radiusOnScreen = hypot((centerX - edgeX).toDouble(), (centerY - edgeY).toDouble()).toFloat()
            val lift = radiusOnScreen * abs(sin(Math.toRadians((state.effectivePitchDegrees ?: state.pitchAngle).toDouble()))).toFloat() * liftScale

            return PerspectiveRadiusInfo(radiusOnScreen, lift)
        }
    }

    private val ptsBuffer = FloatArray(2)

    fun mapPoint(p: PointF, m: Matrix): PointF {
        synchronized(ptsBuffer) {
            ptsBuffer[0] = p.x
            ptsBuffer[1] = p.y
            m.mapPoints(ptsBuffer)
            return PointF(ptsBuffer[0], ptsBuffer[1])
        }
    }

    /**
     * Applies the OpenCV radial/tangential distortion model to a projected point.
     *
     * Coordinate-space caveat (unverified scaling, see GLEE #10): [cameraMatrix] (fx, fy, cx, cy)
     * is in the pixel space of the image the calibration was taken at, while the input here is
     * a VIEW pixel from pitchMatrix. The two agree only when the calibration image and the view
     * share resolution and framing. No code path rescales the intrinsics to the view (the
     * preview is FILL_CENTER-cropped and rotated, so a correct rescale needs the calibration
     * resolution, which the stored calibration does not carry). Until it does, the distortion
     * centre and the corner clamp are approximate on devices whose view differs from the
     * calibration image.
     *
     * Verified state (grep, this revision): nothing in app/src assigns
     * `CueDetatState.cameraMatrix` or `distCoeffs`, so this path is currently never taken at
     * runtime; whoever wires a producer must also supply the calibration resolution and scale
     * fx/fy/cx/cy to the view (accounting for the FILL_CENTER crop) before use.
     */
    fun applyBarrelDistortion(
        screenX: Float, screenY: Float,
        cameraMatrix: DoubleArray,
        distCoeffs: DoubleArray
    ): PointF {
        val out = FloatArray(2)
        distortInto(screenX, screenY, cameraMatrix, distCoeffs, out)
        return PointF(out[0], out[1])
    }

    /** Allocation-free form of [applyBarrelDistortion]; writes (x, y) into [out]. */
    fun distortInto(
        screenX: Float, screenY: Float,
        cameraMatrix: DoubleArray,
        distCoeffs: DoubleArray,
        out: FloatArray
    ) {
        val fx = cameraMatrix[0]
        val cx = cameraMatrix[2]
        val fy = cameraMatrix[4]
        val cy = cameraMatrix[5]

        val k1 = distCoeffs[0]
        val k2 = distCoeffs[1]
        val p1 = distCoeffs[2]
        val p2 = distCoeffs[3]
        val k3 = if (distCoeffs.size > 4) distCoeffs[4] else 0.0

        val x = (screenX - cx) / fx
        val y = (screenY - cy) / fy

        val r2 = x * x + y * y
        // The calibration only holds inside the image. Far outside it the polynomial turns
        // over (barrel k1 < 0 drives the factor negative), flinging a line's off-screen end
        // across the view: lines appeared to fly up off the table. Beyond the image corner
        // the distortion is held at its corner value, which keeps the mapping monotonic.
        val cornerR2 = (cx / fx) * (cx / fx) + (cy / fy) * (cy / fy)
        val scale = if (r2 > cornerR2 && r2 > 0.0) kotlin.math.sqrt(cornerR2 / r2) else 1.0
        val rc2 = minOf(r2, cornerR2)

        val radialDistortion = 1.0 + k1 * rc2 + k2 * (rc2 * rc2) + k3 * (rc2 * rc2 * rc2)

        // Tangential terms grow with r²; beyond the corner, scale them linearly with r instead.
        val xc = x * scale
        val yc = y * scale
        val xTangential = (2.0 * p1 * xc * yc + p2 * (rc2 + 2.0 * xc * xc)) / scale
        val yTangential = (p1 * (rc2 + 2.0 * yc * yc) + 2.0 * p2 * xc * yc) / scale

        val xDistorted = x * radialDistortion + xTangential
        val yDistorted = y * radialDistortion + yTangential

        out[0] = (xDistorted * fx + cx).toFloat()
        out[1] = (yDistorted * fy + cy).toFloat()
    }

    /**
     * Builds a screen-space polyline for the logical segment [startLogical]->[endLogical]
     * projected through [pitchMatrix], sampled at [segments]+1 points so barrel distortion
     * curves it. Writes into [out] (reset first) so hot callers can reuse one Path; the
     * default allocates. Render-thread only.
     */
    fun buildDistortedLinePath(
        startLogical: PointF,
        endLogical: PointF,
        pitchMatrix: Matrix,
        cameraMatrix: DoubleArray?,
        distCoeffs: DoubleArray?,
        segments: Int = 20,
        out: Path = Path()
    ): Path {
        val path = out
        path.reset()
        val hasDistortion = cameraMatrix != null && distCoeffs != null && cameraMatrix.size == 9
        val localPts = linePts
        // Without distortion the projected segment is straight: two points suffice.
        val n = if (hasDistortion) segments else 1

        for (i in 0..n) {
            val t = i.toFloat() / n

            localPts[0] = startLogical.x + (endLogical.x - startLogical.x) * t
            localPts[1] = startLogical.y + (endLogical.y - startLogical.y) * t
            pitchMatrix.mapPoints(localPts)

            if (hasDistortion) {
                distortInto(localPts[0], localPts[1], cameraMatrix!!, distCoeffs!!, localPts)
            }

            if (i == 0) path.moveTo(localPts[0], localPts[1]) else path.lineTo(localPts[0], localPts[1])
        }
        return path
    }

    private val linePts = FloatArray(2)

    private val clipValues = FloatArray(9)

    /**
     * Clips the logical segment [start]->[end] to the part in front of the camera's near plane
     * (projective w > epsilon under [matrix]) and returns that visible part as (start, end),
     * or null when the whole segment is behind the camera.
     *
     * w is affine along the segment, so the crossing is at t = (eps - w1) / (w2 - w1) in both
     * partial cases: when the END is behind, the visible part is start->crossing; when the
     * START is behind, it is crossing->end. Unclipped endpoints are returned as the same
     * instances that were passed in, so callers may test `result.second === end` to ask
     * "was the end clipped?". Render-thread only.
     */
    fun clipToNearPlane(start: PointF, end: PointF, matrix: Matrix): Pair<PointF, PointF>? {
        val v = clipValues
        matrix.getValues(v)
        val g = v[Matrix.MPERSP_0]
        val h = v[Matrix.MPERSP_1]
        val i = v[Matrix.MPERSP_2]

        val w1 = g * start.x + h * start.y + i
        val w2 = g * end.x + h * end.y + i

        val epsilon = 0.0001f
        val startVisible = w1 > epsilon
        val endVisible = w2 > epsilon

        if (startVisible && endVisible) return start to end
        if (!startVisible && !endVisible) return null

        val t = (epsilon - w1) / (w2 - w1)
        val crossing = PointF(
            start.x + t * (end.x - start.x),
            start.y + t * (end.y - start.y)
        )
        return if (startVisible) start to crossing else crossing to end
    }
}

/**
 * Per-renderer cache of the camera intrinsics / distortion coefficient arrays used for barrel
 * distortion of projected lines. [resolve] returns nulls when uncalibrated or in LITE_AR (same
 * LITE_AR rule as renderLensTps), so every line renderer gates distortion identically. Arrays
 * are reused across frames and only reallocated when the Mat size changes.
 */
class LensArrayCache {
    private var camBuffer: DoubleArray? = null
    private var distBuffer: DoubleArray? = null

    fun resolve(state: CueDetatState): Pair<DoubleArray?, DoubleArray?> {
        if (state.cameraMode == com.hereliesaz.cuedetat.domain.CameraMode.LITE_AR) return null to null
        val camMat = state.cameraMatrix
        val distMat = state.distCoeffs
        if (camMat == null || camMat.empty() || distMat == null || distMat.empty()) return null to null
        val camN = camMat.total().toInt()
        val distN = distMat.total().toInt()
        val cam = camBuffer?.takeIf { it.size == camN } ?: DoubleArray(camN).also { camBuffer = it }
        val dist = distBuffer?.takeIf { it.size == distN } ?: DoubleArray(distN).also { distBuffer = it }
        camMat.get(0, 0, cam)
        distMat.get(0, 0, dist)
        return cam to dist
    }
}
