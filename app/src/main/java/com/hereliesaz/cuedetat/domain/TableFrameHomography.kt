package com.hereliesaz.cuedetat.domain

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure, JVM-testable math for projecting the logical table plane onto screen pixels using
 * ARCore camera matrices.
 *
 * Deliberately free of Android and OpenCV types (uses the local [Pt]/[Vec3] structs) so it runs
 * in plain JUnit without Robolectric, mirroring the [TableGeometryFitter] pattern. The caller
 * (the AR session) converts the returned row-major 3x3 into an [android.graphics.Matrix] via
 * `Matrix.setValues`.
 *
 * The whole idea: a perspective projection of a single plane onto the screen is exactly a 3x3
 * homography. We project the four real-world corner anchors to screen, then solve the homography
 * that maps the *ideal* logical table corners onto those screen points. The 2D Canvas renderer
 * then applies that homography to every logical point, so the overlay stays pinned to the real
 * table in full 6DoF as the user walks around it.
 */
object TableFrameHomography {

    data class Pt(val x: Float, val y: Float)
    data class Vec3(val x: Float, val y: Float, val z: Float)

    /** Logical units per inch — matches Table.kt: (LOGICAL_BALL_RADIUS*2) / 2.25in ball. */
    const val LOGICAL_UNITS_PER_INCH = (LOGICAL_BALL_RADIUS * 2f) / 2.25f
    const val METERS_PER_INCH = 0.0254f
    /** Logical units per meter, for converting ARCore metric distances into logical space. */
    const val LOGICAL_UNITS_PER_METER = LOGICAL_UNITS_PER_INCH / METERS_PER_INCH

    /**
     * Physical pool-table coordinates in a fixed ARCore world frame. Logical width/length
     * convert to metres through the existing ball-based coordinate scale. This is a *rigid*
     * planar rectangle, regardless of camera position or projected magnification.
     */
    fun metricRectangleCorners(
        logical: List<Pt>, center: Vec3, right: Vec3, away: Vec3,
        yawDegrees: Float,
    ): List<Vec3> {
        val radians = Math.toRadians(yawDegrees.toDouble())
        val c = kotlin.math.cos(radians).toFloat()
        val s = kotlin.math.sin(radians).toFloat()
        val rx = right.x * c + away.x * s
        val rz = right.z * c + away.z * s
        val ax = away.x * c - right.x * s
        val az = away.z * c - right.z * s
        return logical.map { corner ->
            val x = corner.x / LOGICAL_UNITS_PER_METER
            val y = -corner.y / LOGICAL_UNITS_PER_METER
            Vec3(center.x + rx * x + ax * y, center.y,
                center.z + rz * x + az * y)
        }
    }

    /** A rigid table centre and heading on an ARCore horizontal plane.
     * The table's width/length come from logical dimensions, never from a
     * noisy image-space quad or from independently tracking corner anchors.
     */
    data class HorizontalRectanglePose(val center: Vec3, val right: Vec3, val away: Vec3)

    /**
     * Calibrate a world-space table from four screen-ray intersections in TL,TR,BR,BL order.
     * Ignore the apparent width/length (perspective/plane estimates can distort them);
     * reject near-collinear or nonfinite geometry instead of creating a rotating table.
     */
    fun calibrateHorizontalRectangle(worldCorners: List<Vec3>): HorizontalRectanglePose? {
        if (worldCorners.size != 4 ||
            worldCorners.any { !it.x.isFinite() || !it.y.isFinite() || !it.z.isFinite() }
        ) return null
        val (tl, tr, _, bl) = worldCorners
        val dx = tr.x - tl.x
        val dz = tr.z - tl.z
        val width = kotlin.math.hypot(dx, dz)
        val alongX = tl.x - bl.x
        val alongZ = tl.z - bl.z
        val length = kotlin.math.hypot(alongX, alongZ)
        if (width < 0.05f || length < 0.05f) return null
        val right = Vec3(dx / width, 0f, dz / width)
        var away = Vec3(right.z, 0f, -right.x)
        val alignment = away.x * alongX + away.z * alongZ
        // Both projected axes need to be distinguishable on the chosen plane.
        if (kotlin.math.abs(alignment) < length * 0.1f) return null
        if (alignment < 0f) away = Vec3(-away.x, 0f, -away.z)
        val center = Vec3(
            worldCorners.sumOf { it.x.toDouble() }.toFloat() / 4f,
            worldCorners.sumOf { it.y.toDouble() }.toFloat() / 4f,
            worldCorners.sumOf { it.z.toDouble() }.toFloat() / 4f,
        )
        return HorizontalRectanglePose(center, right, away)
    }

    /** Whether calibrated table-away matches the conventional -anchor.zAxis direction.
     * A mirrored projection cannot be represented by an ARCore yaw quaternion alone.
     */
    fun awayHandedness(pose: HorizontalRectanglePose): Float {
        val defaultAwayX = pose.right.z
        val defaultAwayZ = -pose.right.x
        return if (defaultAwayX * pose.away.x + defaultAwayZ * pose.away.z < 0f) -1f else 1f
    }

    // --- vector helpers ---
    private fun sub(a: Vec3, b: Vec3) = Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
    private fun add(a: Vec3, b: Vec3) = Vec3(a.x + b.x, a.y + b.y, a.z + b.z)
    private fun scale(a: Vec3, s: Float) = Vec3(a.x * s, a.y * s, a.z * s)
    private fun dot(a: Vec3, b: Vec3) = a.x * b.x + a.y * b.y + a.z * b.z
    private fun cross(a: Vec3, b: Vec3) = Vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    )
    private fun length(a: Vec3) = sqrt(dot(a, a))
    private fun normalize(a: Vec3): Vec3 {
        val l = length(a)
        return if (l < 1e-6f) a else scale(a, 1f / l)
    }

    /**
     * Multiply a 4x4 column-major matrix (ARCore/OpenGL convention, element = m[col*4 + row])
     * by the homogeneous vector (x, y, z, w). Returns the resulting 4-vector.
     */
    fun multiplyColMajor(m: FloatArray, x: Float, y: Float, z: Float, w: Float): FloatArray {
        return floatArrayOf(
            m[0] * x + m[4] * y + m[8] * z + m[12] * w,
            m[1] * x + m[5] * y + m[9] * z + m[13] * w,
            m[2] * x + m[6] * y + m[10] * z + m[14] * w,
            m[3] * x + m[7] * y + m[11] * z + m[15] * w
        )
    }

    /**
     * Project a world point to screen pixels through ARCore's view & projection matrices.
     * Returns null if the point is behind the camera (clip.w <= 0).
     *
     * Applies the OpenGL NDC -> screen mapping including the y-flip: NDC +y is up, but Android
     * screen +y is down.
     */
    fun worldToScreen(view: FloatArray, proj: FloatArray, world: Vec3, vpW: Int, vpH: Int): Pt? {
        val eye = multiplyColMajor(view, world.x, world.y, world.z, 1f)
        val clip = multiplyColMajor(proj, eye[0], eye[1], eye[2], eye[3])
        val w = clip[3]
        if (w <= 1e-6f) return null
        val ndcX = clip[0] / w
        val ndcY = clip[1] / w
        val sx = (ndcX * 0.5f + 0.5f) * vpW
        val sy = (1f - (ndcY * 0.5f + 0.5f)) * vpH
        return Pt(sx, sy)
    }

    /**
     * Inverse of [worldToScreen] onto a plane: casts the ray through screen pixel ([sx], [sy]) and
     * intersects it with the plane through [planePoint] with normal [planeNormal].
     *
     * [invViewProj] is the column-major inverse of `proj * view`. Returns null when the ray is
     * parallel to the plane or the intersection lies behind the camera.
     */
    fun screenToPlane(
        invViewProj: FloatArray,
        sx: Float, sy: Float, vpW: Int, vpH: Int,
        planePoint: Vec3, planeNormal: Vec3,
    ): Vec3? {
        val ndcX = sx / vpW * 2f - 1f
        val ndcY = 1f - sy / vpH * 2f
        val near = multiplyColMajor(invViewProj, ndcX, ndcY, -1f, 1f)
        val far = multiplyColMajor(invViewProj, ndcX, ndcY, 1f, 1f)
        if (abs(near[3]) < 1e-9f || abs(far[3]) < 1e-9f) return null
        val origin = Vec3(near[0] / near[3], near[1] / near[3], near[2] / near[3])
        val dir = sub(Vec3(far[0] / far[3], far[1] / far[3], far[2] / far[3]), origin)
        val denom = dot(planeNormal, dir)
        if (abs(denom) < 1e-9f) return null
        val t = dot(planeNormal, sub(planePoint, origin)) / denom
        if (t < 0f) return null
        return add(origin, scale(dir, t))
    }

    /**
     * Plane normal from three corner anchors. The sign is chosen to point "up" (+y in ARCore's
     * Y-up world), so [raise] lifts the table off the floor rather than into it.
     */
    fun planeNormal(tl: Vec3, tr: Vec3, bl: Vec3): Vec3 {
        var n = normalize(cross(sub(tr, tl), sub(bl, tl)))
        if (n.y < 0f) n = scale(n, -1f)
        return n
    }

    /** Lift a world point off the plane by [heightMeters] along the plane normal [n]. */
    fun raise(p: Vec3, n: Vec3, heightMeters: Float): Vec3 =
        if (heightMeters == 0f) p else add(p, scale(n, heightMeters))

    /**
     * Solve the 3x3 homography H (row-major, with H[8] normalised to 1) mapping src[i] -> dst[i]
     * for exactly four correspondences, via the standard DLT 8x8 linear system solved with
     * Gaussian elimination with partial pivoting. Returns null if degenerate (e.g. collinear).
     */
    fun solveHomography(src: List<Pt>, dst: List<Pt>): FloatArray? {
        if (src.size != 4 || dst.size != 4) return null
        val a = Array(8) { DoubleArray(8) }
        val b = DoubleArray(8)
        for (i in 0 until 4) {
            val x = src[i].x.toDouble(); val y = src[i].y.toDouble()
            val u = dst[i].x.toDouble(); val v = dst[i].y.toDouble()
            val r0 = i * 2
            a[r0][0] = x; a[r0][1] = y; a[r0][2] = 1.0
            a[r0][6] = -u * x; a[r0][7] = -u * y
            b[r0] = u
            val r1 = i * 2 + 1
            a[r1][3] = x; a[r1][4] = y; a[r1][5] = 1.0
            a[r1][6] = -v * x; a[r1][7] = -v * y
            b[r1] = v
        }
        val h = gaussianSolve(a, b) ?: return null
        return floatArrayOf(
            h[0].toFloat(), h[1].toFloat(), h[2].toFloat(),
            h[3].toFloat(), h[4].toFloat(), h[5].toFloat(),
            h[6].toFloat(), h[7].toFloat(), 1f
        )
    }

    private fun gaussianSolve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = b.size
        for (col in 0 until n) {
            var pivot = col
            for (r in col + 1 until n) {
                if (abs(a[r][col]) > abs(a[pivot][col])) pivot = r
            }
            if (abs(a[pivot][col]) < 1e-9) return null
            if (pivot != col) {
                val tmp = a[pivot]; a[pivot] = a[col]; a[col] = tmp
                val tb = b[pivot]; b[pivot] = b[col]; b[col] = tb
            }
            val p = a[col][col]
            for (r in 0 until n) {
                if (r == col) continue
                val f = a[r][col] / p
                if (f == 0.0) continue
                for (c in col until n) a[r][c] -= f * a[col][c]
                b[r] -= f * b[col]
            }
        }
        val x = DoubleArray(n)
        for (i in 0 until n) x[i] = b[i] / a[i][i]
        return x
    }

    /**
     * Full per-frame pipeline. Given the four corner anchors in world space (already sorted into
     * TL, TR, BR, BL order) and the four matching ideal-logical corners, the ARCore view &
     * projection matrices and the viewport size, return the row-major 3x3 homography mapping
     * logical-space points -> screen pixels — or null if any corner is not projectable this frame.
     *
     * [heightMeters] lifts the whole table plane along its normal (the AR table-height / Z-slider
     * fix: height is applied in world space, in the same transform as the projection, so it always
     * points straight out of the real table).
     */
    fun computeLogicalToScreen(
        view: FloatArray,
        proj: FloatArray,
        cornersWorld: List<Vec3>,        // size 4, order TL, TR, BR, BL
        idealCornersLogical: List<Pt>,   // size 4, same order
        vpW: Int,
        vpH: Int,
        heightMeters: Float = 0f
    ): FloatArray? {
        if (cornersWorld.size != 4 || idealCornersLogical.size != 4) return null
        // planeNormal takes TL, TR, BL -> indices 0, 1, 3.
        val n = planeNormal(cornersWorld[0], cornersWorld[1], cornersWorld[3])
        val screen = cornersWorld.map { corner ->
            worldToScreen(view, proj, raise(corner, n, heightMeters), vpW, vpH) ?: return null
        }
        return solveHomography(idealCornersLogical, screen)
    }
}
