package com.hereliesaz.cuedetat.core.projection

import com.hereliesaz.cuedetat.core.geometry.Vec2
import com.hereliesaz.cuedetat.core.units.Angle
import com.hereliesaz.cuedetat.core.units.Length
import com.hereliesaz.cuedetat.core.units.meters
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Turning the table into pixels.
 *
 * ## What this replaces
 *
 * The old renderer faked depth with `android.graphics.Camera`, and it was tuned
 * to look good rather than to be true:
 *
 *  - device pitch of 0–75° was remapped onto a *visual* 0–78° with a cubic ease
 *    above 60°, so the drawn plane was never at the angle the phone was at;
 *  - roll was rendered at 30% of actual;
 *  - the virtual camera sat at a hardcoded `translate(0, 0, -32f)` — roughly a
 *    20° field of view, against a real phone lens's ~65°;
 *  - no device intrinsics were read anywhere.
 *
 * Those choices are defensible for a standalone protractor. They are fatal the
 * moment the drawing is laid over a live camera feed, because the overlay and
 * the video are then projected by two different and incompatible cameras. No
 * amount of work in the vision pipeline could have fixed that.
 *
 * Here there is exactly one projection, built from a real [CameraIntrinsics] and
 * a real [TablePose]. It is also the reason ball height stops being a special
 * case: a ball centre is simply the 3D point `(x, y, radius)` and gets projected
 * like anything else, instead of needing a screen-space `radius * sin(pitch)`
 * fudge applied by hand at every draw site — a fudge the old aiming lines forgot
 * to apply, which is why they did not land on the ball they pointed at.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Small linear algebra
// ─────────────────────────────────────────────────────────────────────────────

/** A point in 3D table space. Metres; `z` is height above the cloth. */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3): Double = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val norm: Double get() = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val n = norm
        return if (n < 1e-12) this else Vec3(x / n, y / n, z / n)
    }

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }
}

/** Row-major 3×3 matrix. */
data class Mat3(val m: DoubleArray) {

    init {
        require(m.size == 9) { "Mat3 needs 9 elements, got ${m.size}" }
    }

    operator fun get(row: Int, col: Int): Double = m[row * 3 + col]

    operator fun times(other: Mat3): Mat3 {
        val out = DoubleArray(9)
        for (r in 0..2) for (c in 0..2) {
            var acc = 0.0
            for (k in 0..2) acc += this[r, k] * other[k, c]
            out[r * 3 + c] = acc
        }
        return Mat3(out)
    }

    operator fun times(v: Vec3) = Vec3(
        this[0, 0] * v.x + this[0, 1] * v.y + this[0, 2] * v.z,
        this[1, 0] * v.x + this[1, 1] * v.y + this[1, 2] * v.z,
        this[2, 0] * v.x + this[2, 1] * v.y + this[2, 2] * v.z,
    )

    val determinant: Double
        get() = this[0, 0] * (this[1, 1] * this[2, 2] - this[1, 2] * this[2, 1]) -
            this[0, 1] * (this[1, 0] * this[2, 2] - this[1, 2] * this[2, 0]) +
            this[0, 2] * (this[1, 0] * this[2, 1] - this[1, 1] * this[2, 0])

    /** Inverse, or `null` when singular. Callers must handle the null. */
    fun inverse(): Mat3? {
        val det = determinant
        if (abs(det) < 1e-14) return null
        val inv = DoubleArray(9)
        inv[0] = (this[1, 1] * this[2, 2] - this[1, 2] * this[2, 1]) / det
        inv[1] = (this[0, 2] * this[2, 1] - this[0, 1] * this[2, 2]) / det
        inv[2] = (this[0, 1] * this[1, 2] - this[0, 2] * this[1, 1]) / det
        inv[3] = (this[1, 2] * this[2, 0] - this[1, 0] * this[2, 2]) / det
        inv[4] = (this[0, 0] * this[2, 2] - this[0, 2] * this[2, 0]) / det
        inv[5] = (this[0, 2] * this[1, 0] - this[0, 0] * this[1, 2]) / det
        inv[6] = (this[1, 0] * this[2, 1] - this[1, 1] * this[2, 0]) / det
        inv[7] = (this[0, 1] * this[2, 0] - this[0, 0] * this[2, 1]) / det
        inv[8] = (this[0, 0] * this[1, 1] - this[0, 1] * this[1, 0]) / det
        return Mat3(inv)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Mat3 && m.contentEquals(other.m))

    override fun hashCode(): Int = m.contentHashCode()

    companion object {
        val IDENTITY = Mat3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0))

        fun columns(c0: Vec3, c1: Vec3, c2: Vec3) = Mat3(
            doubleArrayOf(
                c0.x, c1.x, c2.x,
                c0.y, c1.y, c2.y,
                c0.z, c1.z, c2.z,
            )
        )

        /** Rotation about the x axis. */
        fun rotationX(angle: Angle): Mat3 {
            val c = kotlin.math.cos(angle.radians)
            val s = kotlin.math.sin(angle.radians)
            return Mat3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c))
        }

        /** Rotation about the z axis. */
        fun rotationZ(angle: Angle): Mat3 {
            val c = kotlin.math.cos(angle.radians)
            val s = kotlin.math.sin(angle.radians)
            return Mat3(doubleArrayOf(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera
// ─────────────────────────────────────────────────────────────────────────────

/** A point in image space. Pixels, origin top-left, `+y` down. */
data class ScreenPoint(val x: Double, val y: Double)

/**
 * Pinhole intrinsics in pixels.
 *
 * Obtain these from the platform — on Android, `CameraCharacteristics`'
 * `LENS_INTRINSIC_CALIBRATION` when the device publishes it, otherwise derived
 * from `SENSOR_INFO_PHYSICAL_SIZE` and `LENS_INFO_AVAILABLE_FOCAL_LENGTHS`.
 * Never invent them.
 */
data class CameraIntrinsics(
    val focalLengthX: Double,
    val focalLengthY: Double,
    val principalPointX: Double,
    val principalPointY: Double,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    val matrix: Mat3
        get() = Mat3(
            doubleArrayOf(
                focalLengthX, 0.0, principalPointX,
                0.0, focalLengthY, principalPointY,
                0.0, 0.0, 1.0,
            )
        )

    val horizontalFieldOfView: Angle
        get() = Angle(2.0 * kotlin.math.atan(imageWidth / (2.0 * focalLengthX)))

    val verticalFieldOfView: Angle
        get() = Angle(2.0 * kotlin.math.atan(imageHeight / (2.0 * focalLengthY)))

    companion object {
        /**
         * Builds intrinsics from a horizontal field of view. Use only when the
         * platform genuinely cannot supply calibration, and say so in the UI —
         * an assumed FOV means the overlay is approximately, not exactly, aligned.
         */
        fun fromHorizontalFov(fov: Angle, width: Int, height: Int): CameraIntrinsics {
            val fx = width / (2.0 * tan(fov.radians / 2.0))
            return CameraIntrinsics(fx, fx, width / 2.0, height / 2.0, width, height)
        }

        /** A neutral synthetic camera for the no-camera protractor mode. */
        fun synthetic(width: Int, height: Int): CameraIntrinsics =
            fromHorizontalFov(Angle(1.134464), width, height) // 65°, a typical phone rear lens
    }
}

/**
 * Where the camera is, relative to the table.
 *
 * [rotation] and [translation] map a point in table coordinates into camera
 * coordinates: `p_cam = rotation * p_table + translation`.
 */
data class TablePose(val rotation: Mat3, val translation: Vec3) {

    /** Camera position expressed in table coordinates. */
    val cameraPositionInTable: Vec3?
        get() = rotation.inverse()?.let { it * (translation * -1.0) }

    /** Height of the camera above the cloth. */
    val cameraHeight: Length? get() = cameraPositionInTable?.z?.meters

    companion object {
        /**
         * A pose looking at the table centre from [height] above the cloth and
         * [distance] back along `-x`, tilted to frame the table.
         *
         * This is what the no-camera protractor mode uses. It is an honest
         * synthetic viewpoint chosen by the app, not a fudged version of a real
         * one — and it flows through exactly the same projection as a real pose,
         * so there is only ever one code path.
         */
        fun elevatedView(height: Length, distance: Length, tilt: Angle): TablePose {
            // Camera looks along +x and down by `tilt`.
            val r = Mat3.rotationX(Angle(kotlin.math.PI / 2.0 + tilt.radians)) *
                Mat3.rotationZ(Angle(-kotlin.math.PI / 2.0))
            val camInTable = Vec3(-distance.meters, 0.0, height.meters)
            val t = (r * camInTable) * -1.0
            return TablePose(r, t)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Projector
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Projects table geometry to the screen and back.
 *
 * The table plane maps to the image by a plain 3×3 homography, because a
 * perspective projection of a *single plane* always is one. That homography is
 * `K · [r1 | r2 | t]` — the first two columns of the rotation plus the
 * translation. Anything off the plane (a ball centre, a rail top) uses the full
 * 3D projection instead.
 */
class Projector(
    val intrinsics: CameraIntrinsics,
    val pose: TablePose,
) {

    /** Table plane (`z = 0`) to image. */
    val planeHomography: Mat3 = run {
        val r = pose.rotation
        val c0 = Vec3(r[0, 0], r[1, 0], r[2, 0])
        val c1 = Vec3(r[0, 1], r[1, 1], r[2, 1])
        intrinsics.matrix * Mat3.columns(c0, c1, pose.translation)
    }

    private val inversePlaneHomography: Mat3? = planeHomography.inverse()

    /** True when screen points can be mapped back onto the table. */
    val isInvertible: Boolean get() = inversePlaneHomography != null

    /** Projects a point on the cloth. Null when it falls behind the camera. */
    fun tableToScreen(point: Vec2): ScreenPoint? {
        val h = planeHomography * Vec3(point.x, point.y, 1.0)
        if (h.z <= 1e-9) return null
        return ScreenPoint(h.x / h.z, h.y / h.z)
    }

    /**
     * Projects a point at any height above the cloth.
     *
     * This is how a ball centre is drawn: at `(x, y, ballRadius)`. There is no
     * separate "lift" term for any consumer to forget.
     */
    fun tableToScreen(point: Vec3): ScreenPoint? {
        val cam = pose.rotation * point + pose.translation
        if (cam.z <= 1e-9) return null
        val img = intrinsics.matrix * cam
        return ScreenPoint(img.x / img.z, img.y / img.z)
    }

    /** Maps a screen point back onto the cloth. Null when the ray misses the plane. */
    fun screenToTable(point: ScreenPoint): Vec2? {
        val inv = inversePlaneHomography ?: return null
        val h = inv * Vec3(point.x, point.y, 1.0)
        if (abs(h.z) <= 1e-12) return null
        return Vec2(h.x / h.z, h.y / h.z)
    }

    /**
     * On-screen radius of a ball of [radius] whose centre sits at [center] on
     * the cloth.
     *
     * Measured by projecting the ball's centre and a point one radius to the
     * side *at the same height*, then taking the screen distance. Measuring
     * along a single axis of the plane, as the old code did, shrinks with
     * perspective foreshortening and made balls appear to deflate on tilt.
     */
    fun projectedRadius(center: Vec2, radius: Length): Double? {
        val h = radius.meters
        val c = tableToScreen(Vec3(center.x, center.y, h)) ?: return null
        // Offset perpendicular to the viewing direction, so foreshortening does
        // not bias the measurement.
        val toCamera = pose.cameraPositionInTable ?: return null
        val viewDir = Vec2(toCamera.x - center.x, toCamera.y - center.y).normalized()
        val side = viewDir.perpendicular * h
        val e = tableToScreen(Vec3(center.x + side.x, center.y + side.y, h)) ?: return null
        return sqrt((e.x - c.x) * (e.x - c.x) + (e.y - c.y) * (e.y - c.y))
    }

    /** Where a ball's centre appears — the single source of truth for every consumer. */
    fun ballCenterOnScreen(center: Vec2, radius: Length): ScreenPoint? =
        tableToScreen(Vec3(center.x, center.y, radius.meters))
}

// ─────────────────────────────────────────────────────────────────────────────
// Pose estimation from correspondences
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Estimates the table-plane to image homography from four or more
 * correspondences, by the Direct Linear Transform.
 *
 * This is what a four-corner tap gives you, and it is enough on its own to draw
 * a correctly registered overlay without any ARCore at all.
 *
 * Returns `null` if the system is degenerate (collinear points, duplicates).
 */
fun homographyFromCorrespondences(
    tablePoints: List<Vec2>,
    screenPoints: List<ScreenPoint>,
): Mat3? {
    require(tablePoints.size == screenPoints.size) { "correspondence count mismatch" }
    if (tablePoints.size < 4) return null

    // Each correspondence contributes two rows to A·h = 0.
    val rows = ArrayList<DoubleArray>(tablePoints.size * 2)
    for (i in tablePoints.indices) {
        val (x, y) = tablePoints[i]
        val u = screenPoints[i].x
        val v = screenPoints[i].y
        rows += doubleArrayOf(-x, -y, -1.0, 0.0, 0.0, 0.0, u * x, u * y, u)
        rows += doubleArrayOf(0.0, 0.0, 0.0, -x, -y, -1.0, v * x, v * y, v)
    }
    val h = solveNullspace(rows) ?: return null
    if (abs(h[8]) < 1e-12) return null
    // Normalise so h33 == 1, which keeps the numbers comparable and readable.
    return Mat3(DoubleArray(9) { h[it] / h[8] })
}

/**
 * Smallest-singular-vector of `A`, found by inverse iteration on `AᵀA`.
 *
 * Deliberately dependency-free: this module must stay pure Kotlin so it can run
 * on the JVM, on iOS and in a plain unit test with no native libraries.
 */
private fun solveNullspace(rows: List<DoubleArray>): DoubleArray? {
    val n = 9
    val ata = Array(n) { DoubleArray(n) }
    for (row in rows) {
        for (i in 0 until n) for (j in 0 until n) ata[i][j] += row[i] * row[j]
    }

    // Inverse iteration with a small shift: repeatedly solve (AᵀA + eI)x = b.
    val shifted = Array(n) { i -> DoubleArray(n) { j -> ata[i][j] + if (i == j) 1e-10 else 0.0 } }
    var v = DoubleArray(n) { 1.0 / sqrt(n.toDouble()) }

    repeat(200) {
        val solved = solveLinearSystem(shifted, v) ?: return null
        val norm = sqrt(solved.sumOf { it * it })
        if (norm < 1e-300) return null
        v = DoubleArray(n) { solved[it] / norm }
    }
    return v
}

/** Gaussian elimination with partial pivoting. Returns `null` if singular. */
private fun solveLinearSystem(matrix: Array<DoubleArray>, rhs: DoubleArray): DoubleArray? {
    val n = rhs.size
    val a = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) matrix[i][j] else rhs[i] } }

    for (col in 0 until n) {
        var pivot = col
        for (r in col + 1 until n) if (abs(a[r][col]) > abs(a[pivot][col])) pivot = r
        if (abs(a[pivot][col]) < 1e-300) return null
        val tmp = a[col]; a[col] = a[pivot]; a[pivot] = tmp

        for (r in 0 until n) {
            if (r == col) continue
            val factor = a[r][col] / a[col][col]
            if (factor == 0.0) continue
            for (c in col..n) a[r][c] -= factor * a[col][c]
        }
    }
    return DoubleArray(n) { a[it][n] / a[it][it] }
}
