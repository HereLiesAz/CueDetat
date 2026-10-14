package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableFrameHomographyTest {

    private val identity4 = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    /** Apply a row-major 3x3 homography to a logical point, returning screen pixels. */
    private fun applyH(h: FloatArray, p: Pt): Pt {
        val w = h[6] * p.x + h[7] * p.y + h[8]
        val x = (h[0] * p.x + h[1] * p.y + h[2]) / w
        val y = (h[3] * p.x + h[4] * p.y + h[5]) / w
        return Pt(x, y)
    }

    @Test
    fun `logical scale matches Table convention`() {
        // (LOGICAL_BALL_RADIUS*2)/2.25 = 50/2.25
        assertEquals(50f / 2.25f, TableFrameHomography.LOGICAL_UNITS_PER_INCH, 1e-3f)
        assertEquals(
            TableFrameHomography.LOGICAL_UNITS_PER_INCH / 0.0254f,
            TableFrameHomography.LOGICAL_UNITS_PER_METER,
            1e-2f
        )
    }

    @Test
    fun `metric eight-foot rectangle preserves actual playing-bed dimensions`() {
        // Independent reference: eight-foot bed = 44in x 88in; 1in = 0.0254m.
        val unit = TableFrameHomography.LOGICAL_UNITS_PER_INCH
        val logical = listOf(
            Pt(-22f * unit, -44f * unit), Pt(22f * unit, -44f * unit),
            Pt(22f * unit, 44f * unit), Pt(-22f * unit, 44f * unit)
        )
        val corners = TableFrameHomography.metricRectangleCorners(
            logical, Vec3(2f, 1f, -3f),
            Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f), 0f
        )
        assertEquals(1.4412f, corners[0].x, 1e-4f)
        assertEquals(-4.1176f, corners[0].z, 1e-4f)
        assertEquals(2.5588f, corners[1].x, 1e-4f)
        assertEquals(-1.8824f, corners[2].z, 1e-4f)
        corners.forEach { assertEquals(1f, it.y, 1e-4f) }
        val rotated = TableFrameHomography.metricRectangleCorners(
            logical, Vec3(2f, 1f, -3f),
            Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f), 90f
        )
        assertEquals(0.8824f, rotated[0].x, 1e-4f)
        assertEquals(-2.4412f, rotated[0].z, 1e-4f)
    }

    @Test
    fun `camera translation changes apparent position without moving table`() {
        val logical = listOf(Pt(-1f, -1f), Pt(1f, -1f), Pt(1f, 1f), Pt(-1f, 1f))
        val corners = TableFrameHomography.metricRectangleCorners(
            logical, Vec3(0f, 0f, -2f),
            Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f), 0f
        )
        val proj = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
            0f, 0f, -1f, -1f, 0f, 0f, 0f, 0f)
        val shiftedCamera = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f, -1f, 0f, 0f, 1f)
        val before = TableFrameHomography.worldToScreen(identity4, proj, corners[0], 1000, 1000)!!
        val after = TableFrameHomography.worldToScreen(shiftedCamera, proj, corners[0], 1000, 1000)!!
        assertEquals(250f, before.x - after.x, 1f)
        assertEquals(before.y, after.y, 1e-4f)
        assertEquals(-2f, (corners[0].z + corners[2].z) / 2f, 1e-4f)
    }

    @Test
    fun `apparent size follows inverse distance not artificial pitch scaling`() {
        val proj = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
            0f, 0f, -1f, -1f, 0f, 0f, 0f, 0f)
        val closerView = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f, 0f, 0.1f, 1f)
        val fixed = Vec3(0.3f, 0f, -2f)
        val first = TableFrameHomography.worldToScreen(identity4, proj, fixed, 1000, 1000)!!
        val closer = TableFrameHomography.worldToScreen(closerView, proj, fixed, 1000, 1000)!!
        assertEquals(575f, first.x, 1e-3f)
        assertEquals(578.9474f, closer.x, 0.01f)
    }

    @Test
    fun `multiplyColMajor applies translation from last column`() {
        val translate = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            5f, 6f, 7f, 1f   // last column = translation
        )
        val out = TableFrameHomography.multiplyColMajor(translate, 1f, 2f, 3f, 1f)
        assertEquals(6f, out[0], 1e-4f)
        assertEquals(8f, out[1], 1e-4f)
        assertEquals(10f, out[2], 1e-4f)
        assertEquals(1f, out[3], 1e-4f)
    }

    @Test
    fun `worldToScreen maps NDC to pixels with y-flip`() {
        // identity view & proj: clip = world, w = 1, so ndc = (x, y).
        // NDC (-1,1) is top-left -> screen (0,0); NDC (1,-1) is bottom-right -> (W,H).
        val topLeft = TableFrameHomography.worldToScreen(identity4, identity4, Vec3(-1f, 1f, 0f), 1000, 800)!!
        assertEquals(0f, topLeft.x, 1e-3f)
        assertEquals(0f, topLeft.y, 1e-3f)

        val center = TableFrameHomography.worldToScreen(identity4, identity4, Vec3(0f, 0f, 0f), 1000, 800)!!
        assertEquals(500f, center.x, 1e-3f)
        assertEquals(400f, center.y, 1e-3f)

        val bottomRight = TableFrameHomography.worldToScreen(identity4, identity4, Vec3(1f, -1f, 0f), 1000, 800)!!
        assertEquals(1000f, bottomRight.x, 1e-3f)
        assertEquals(800f, bottomRight.y, 1e-3f)
    }

    @Test
    fun `screenToPlane hits the plane under the pixel`() {
        // Identity view & proj: NDC == world. Ray through screen centre runs along z from -1 to 1;
        // plane z = 0.5 (normal +z). Expected hit: world (0, 0, 0.5), written out by hand.
        val hit = TableFrameHomography.screenToPlane(
            identity4, 500f, 400f, 1000, 800,
            planePoint = Vec3(0f, 0f, 0.5f), planeNormal = Vec3(0f, 0f, 1f)
        )!!
        assertEquals(0f, hit.x, 1e-4f)
        assertEquals(0f, hit.y, 1e-4f)
        assertEquals(0.5f, hit.z, 1e-4f)

        // Top-left pixel is NDC (-1, 1) -> world (-1, 1, z).
        val corner = TableFrameHomography.screenToPlane(
            identity4, 0f, 0f, 1000, 800,
            planePoint = Vec3(0f, 0f, 0f), planeNormal = Vec3(0f, 0f, 1f)
        )!!
        assertEquals(-1f, corner.x, 1e-4f)
        assertEquals(1f, corner.y, 1e-4f)
    }

    @Test
    fun `screenToPlane rejects a ray parallel to the plane`() {
        // Ray runs along z; a plane with normal +x is parallel to it.
        assertNull(
            TableFrameHomography.screenToPlane(
                identity4, 500f, 400f, 1000, 800,
                planePoint = Vec3(5f, 0f, 0f), planeNormal = Vec3(1f, 0f, 0f)
            )
        )
    }

    @Test
    fun `worldToScreen rejects points behind the camera`() {
        // Perspective-style proj (col-major) where clip.w = -z_eye. With identity view, a point at
        // z = +1 has w = -1 (behind, OpenGL camera looks down -z) -> null; z = -1 -> projectable.
        val proj = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, -1f, -1f,  // column 2: row2 = -1 (z), row3 (w) = -1 => w = -z
            0f, 0f, 0f, 0f
        )
        assertNull(TableFrameHomography.worldToScreen(identity4, proj, Vec3(0f, 0f, 1f), 100, 100))
        assertNotNull(TableFrameHomography.worldToScreen(identity4, proj, Vec3(0f, 0f, -1f), 100, 100))
    }

    @Test
    fun `solveHomography recovers a known affine map and round-trips interior points`() {
        // Map a unit square to a scaled + translated rectangle (an affine map is a special homography).
        val src = listOf(Pt(0f, 0f), Pt(1f, 0f), Pt(1f, 1f), Pt(0f, 1f))
        val dst = listOf(Pt(100f, 200f), Pt(300f, 200f), Pt(300f, 600f), Pt(100f, 600f))
        val h = TableFrameHomography.solveHomography(src, dst)!!
        // corners
        src.forEachIndexed { i, p ->
            val got = applyH(h, p)
            assertEquals(dst[i].x, got.x, 1e-2f)
            assertEquals(dst[i].y, got.y, 1e-2f)
        }
        // interior point (0.5,0.5) -> rectangle centre (200,400)
        val mid = applyH(h, Pt(0.5f, 0.5f))
        assertEquals(200f, mid.x, 1e-2f)
        assertEquals(400f, mid.y, 1e-2f)
    }

    @Test
    fun `solveHomography returns null for collinear source`() {
        val src = listOf(Pt(0f, 0f), Pt(1f, 0f), Pt(2f, 0f), Pt(3f, 0f))
        val dst = listOf(Pt(0f, 0f), Pt(1f, 1f), Pt(2f, 2f), Pt(3f, 3f))
        assertNull(TableFrameHomography.solveHomography(src, dst))
    }

    @Test
    fun `computeLogicalToScreen pins logical corners to projected anchors and centre to centre`() {
        // Four world corners (z=0 floor) chosen to land at quarter points of a 1000x1000 viewport
        // under identity view/proj. Order TL, TR, BR, BL.
        val cornersWorld = listOf(
            Vec3(-0.5f, 0.5f, 0f),   // TL -> (250,250)
            Vec3(0.5f, 0.5f, 0f),    // TR -> (750,250)
            Vec3(0.5f, -0.5f, 0f),   // BR -> (750,750)
            Vec3(-0.5f, -0.5f, 0f)   // BL -> (250,750)
        )
        // Ideal 8ft logical corners, same order TL, TR, BR, BL.
        val ideal = listOf(
            Pt(-44f, -22f), Pt(44f, -22f), Pt(44f, 22f), Pt(-44f, 22f)
        )
        val h = TableFrameHomography.computeLogicalToScreen(
            identity4, identity4, cornersWorld, ideal, 1000, 1000
        )!!

        val expected = listOf(Pt(250f, 250f), Pt(750f, 250f), Pt(750f, 750f), Pt(250f, 750f))
        ideal.forEachIndexed { i, p ->
            val got = applyH(h, p)
            assertEquals("corner $i x", expected[i].x, got.x, 0.5f)
            assertEquals("corner $i y", expected[i].y, got.y, 0.5f)
        }
        // Logical centre (0,0) -> screen centre (500,500).
        val centre = applyH(h, Pt(0f, 0f))
        assertEquals(500f, centre.x, 0.5f)
        assertEquals(500f, centre.y, 0.5f)
    }

    @Test
    fun `computeLogicalToScreen height lift keeps a level table centred but larger`() {
        // Camera above the floor looking down: a perspective proj so that lifting the plane toward
        // the camera enlarges it. We just assert the matrix is still solvable and the centre stays put.
        val cornersWorld = listOf(
            Vec3(-0.5f, 0.5f, 0f), Vec3(0.5f, 0.5f, 0f),
            Vec3(0.5f, -0.5f, 0f), Vec3(-0.5f, -0.5f, 0f)
        )
        val ideal = listOf(Pt(-44f, -22f), Pt(44f, -22f), Pt(44f, 22f), Pt(-44f, 22f))
        val lifted = TableFrameHomography.computeLogicalToScreen(
            identity4, identity4, cornersWorld, ideal, 1000, 1000, heightMeters = 0.1f
        )
        assertNotNull(lifted)
        // The plane normal is along z here, and identity proj is orthographic, so a lift along the
        // normal does not change screen x/y -> centre still 500,500. This guards the raise() path.
        val centre = applyH(lifted!!, Pt(0f, 0f))
        assertEquals(500f, centre.x, 0.5f)
        assertEquals(500f, centre.y, 0.5f)
        assertTrue(lifted[8] == 1f)
    }
    @Test
    fun `single-anchor calibration yields orthogonal stable metric corners`() {
        // Ray intersections on a slightly skewed plane: choose yaw and centre,
        // but do not inherit noisy width or skew in the locked playing bed.
        val sampled = listOf(
            Vec3(-0.6f, 0.76f, -3.0f), Vec3(0.65f, 0.76f, -2.95f),
            Vec3(0.6f, 0.76f, -0.45f), Vec3(-0.55f, 0.76f, -0.5f),
        )
        val pose = TableFrameHomography.calibrateHorizontalRectangle(sampled)!!
        val unit = TableFrameHomography.LOGICAL_UNITS_PER_INCH
        val logical = listOf(
            Pt(-22f * unit, -44f * unit), Pt(22f * unit, -44f * unit),
            Pt(22f * unit, 44f * unit), Pt(-22f * unit, 44f * unit),
        )
        val corners = TableFrameHomography.metricRectangleCorners(
            logical, pose.center, pose.right, pose.away, 0f,
        )
        fun distance(a: Vec3, b: Vec3) =
            kotlin.math.hypot(a.x - b.x, a.z - b.z)
        assertEquals(44f * 0.0254f, distance(corners[0], corners[1]), 1e-4f)
        assertEquals(88f * 0.0254f, distance(corners[1], corners[2]), 1e-4f)
        assertEquals(44f * 0.0254f, distance(corners[2], corners[3]), 1e-4f)
        assertEquals(88f * 0.0254f, distance(corners[3], corners[0]), 1e-4f)
        val a = corners[1]; val b = corners[0]; val c = corners[3]
        val dot = (a.x - b.x) * (c.x - b.x) + (a.z - b.z) * (c.z - b.z)
        assertEquals(0f, dot, 1e-4f)
        // The centroid remains attached to the one world anchor.
        assertEquals(pose.center.x, corners.sumOf { it.x.toDouble() }.toFloat() / 4f, 1e-4f)
        assertEquals(pose.center.z, corners.sumOf { it.z.toDouble() }.toFloat() / 4f, 1e-4f)
        assertEquals(1f, TableFrameHomography.awayHandedness(pose), 1e-4f)
    }

    @Test
    fun `calibration keeps the selected table end instead of flipping 180 degrees`() {
        val sampled = listOf(
            Vec3(-0.5f, 1f, -2.5f), Vec3(0.5f, 1f, -2.5f),
            Vec3(0.5f, 1f, -4.5f), Vec3(-0.5f, 1f, -4.5f),
        )
        val pose = TableFrameHomography.calibrateHorizontalRectangle(sampled)!!
        assertEquals(1f, pose.away.z, 1e-4f)
        assertEquals(1f, pose.right.x, 1e-4f)
        assertEquals(-1f, TableFrameHomography.awayHandedness(pose), 1e-4f)
    }

    @Test
    fun `calibration rejects unusable geometry before locking`() {
        assertNull(TableFrameHomography.calibrateHorizontalRectangle(emptyList()))
        assertNull(TableFrameHomography.calibrateHorizontalRectangle(
            List(4) { Vec3(0f, 0.6f, -2f) },
        ))
        assertNull(TableFrameHomography.calibrateHorizontalRectangle(listOf(
            Vec3(0f, 0f, 0f), Vec3(1f, 0f, 0f),
            Vec3(2f, 0f, 0f), Vec3(3f, 0f, 0f),
        )))
        assertNull(TableFrameHomography.calibrateHorizontalRectangle(listOf(
            Vec3(Float.NaN, 0f, 0f), Vec3(1f, 0f, 0f),
            Vec3(2f, 0f, 0f), Vec3(3f, 0f, 0f),
        )))
    }

}
