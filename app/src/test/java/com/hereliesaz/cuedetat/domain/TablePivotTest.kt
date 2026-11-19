package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pivot corner, and the shift that keeps it still while the table changes size. Worked by hand. */
class TablePivotTest {

    private val w = 1000
    private val h = 2000

    // A table seen from behind its near end: wider at the bottom of the screen (TL, TR, BR, BL).
    private fun quad(vararg p: Pair<Float, Float>) = p.map { Pt(it.first, it.second) }
    private val fullyInView = quad(300f to 600f, 700f to 600f, 900f to 1500f, 100f to 1500f)

    @Test
    fun `with all four corners in view the nearest is the one with the longest edges at it`() {
        // The near corners are the bottom two; BR's edges are 900 to 700 wide-ish and 900 tall; BL's mirror them.
        // TR to BR is hypot(200, 900); BR to BL is 800; BL to TL is hypot(200, 900): BR = 800 + 922 = 1722, BL the same.
        // A tie goes to the first with that reach in index order of maxBy: BR (index 2) comes before BL (3).
        val i = TablePivot.choose(fullyInView, w, h)
        assertTrue("a near corner: $i", i == 2 || i == 3)
    }

    @Test
    fun `a corner off the screen is not the pivot, however near it is`() {
        // BL hangs off the left edge (x < 0) and is the nearest; BR is in view.
        val q = quad(300f to 600f, 700f to 600f, 900f to 1500f, -400f to 1500f)
        assertEquals(2, TablePivot.choose(q, w, h))
    }

    @Test
    fun `the near corner is chosen over a far one that is also in view`() {
        // TL, TR in view but far (short edges between them); BR in view and near.
        val q = quad(450f to 700f, 550f to 700f, 950f to 1900f, -300f to 1900f)
        assertEquals(2, TablePivot.choose(q, w, h))
    }

    @Test
    fun `with no corner in view it is the one nearest the middle of the screen`() {
        // A table filling the frame: every corner is far outside. TL is the least far.
        val q = quad(-100f to -100f, 2000f to -3000f, 4000f to 5000f, -3000f to 4000f)
        assertEquals(0, TablePivot.choose(q, w, h))
    }

    @Test
    fun `a bad viewport or a non-finite corner is not a crash, it is corner 0`() {
        assertEquals(0, TablePivot.choose(fullyInView, 0, 0))
        assertEquals(0, TablePivot.choose(quad(Float.NaN to 0f, 1f to 1f, 2f to 2f, 3f to 3f), w, h))
    }

    @Test
    fun `the shift is the corner's offset times one minus the ratio, and never lifts the table`() {
        val s = TablePivot.centerShift(Vec3(2f, 0.3f, -1f), 1.5f)
        assertEquals(-1f, s.x, 1e-5f)   // 2 * (1 - 1.5)
        assertEquals(0f, s.y, 0f)
        assertEquals(0.5f, s.z, 1e-5f)  // -1 * -0.5
        // Shrinking moves the centre the other way.
        assertEquals(1f, TablePivot.centerShift(Vec3(2f, 0f, 0f), 0.5f).x, 1e-5f)
        // Nonsense ratios move nothing.
        assertEquals(0f, TablePivot.centerShift(Vec3(2f, 0f, 3f), Float.NaN).x, 0f)
        assertEquals(0f, TablePivot.centerShift(Vec3(2f, 0f, 3f), -1f).z, 0f)
    }

    @Test
    fun `scaling about the pivot with the shift leaves that corner exactly where it was, whatever the heading`() {
        val logical = listOf(Pt(-300f, -600f), Pt(300f, -600f), Pt(300f, 600f), Pt(-300f, 600f)) // TL TR BR BL
        val center = Vec3(1.0f, 0.7f, -2.0f)
        for (yaw in listOf(0f, 33f, 90f, -120f)) {
            val right = Vec3(1f, 0f, 0f)
            val away = Vec3(0f, 0f, -1f)
            val before = TableFrameHomography.metricRectangleCorners(logical, center, right, away, yaw, scale = 1f)
            for (pivot in 0..3) {
                for (ratio in listOf(0.7f, 1.3f, 1.6f)) {
                    val offset = Vec3(before[pivot].x - center.x, 0f, before[pivot].z - center.z)
                    val shift = TablePivot.centerShift(offset, ratio)
                    val moved = Vec3(center.x + shift.x, center.y, center.z + shift.z)
                    val after = TableFrameHomography.metricRectangleCorners(logical, moved, right, away, yaw, scale = ratio)
                    assertEquals("pivot $pivot x, yaw $yaw, ratio $ratio", before[pivot].x, after[pivot].x, 1e-4f)
                    assertEquals("pivot $pivot z, yaw $yaw, ratio $ratio", before[pivot].z, after[pivot].z, 1e-4f)
                    // And the rest really did change size: the opposite corner is `ratio` times as far.
                    val opposite = (pivot + 2) % 4
                    val d0 = Math.hypot((before[opposite].x - before[pivot].x).toDouble(), (before[opposite].z - before[pivot].z).toDouble())
                    val d1 = Math.hypot((after[opposite].x - after[pivot].x).toDouble(), (after[opposite].z - after[pivot].z).toDouble())
                    assertEquals(ratio.toDouble(), d1 / d0, 1e-4)
                }
            }
        }
    }
}
