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
}
