// app/src/test/java/com/hereliesaz/cuedetat/domain/TableGeometryFitterTest.kt
package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableGeometryFitter.Pt
import org.junit.Assert.*
import org.junit.Test

class TableGeometryFitterTest {

    // Standard 9ft table: 100" × 50", pockets at corners and long-side midpoints.
    // Origin at centre; TL=(-50,-25), TR=(50,-25), BL=(-50,25), BR=(50,25), SL=(-50,0), SR=(50,0)
    private val idealPts = mapOf(
        PocketId.TL to Pt(-50f, -25f),
        PocketId.TR to Pt( 50f, -25f),
        PocketId.BL to Pt(-50f,  25f),
        PocketId.BR to Pt( 50f,  25f),
        PocketId.SL to Pt(-50f,   0f),
        PocketId.SR to Pt( 50f,   0f)
    )

    private fun shuffled() = idealPts.values.toMutableList().also { it.shuffle() }

    @Test
    fun `fit returns all six identities for ideal layout`() {
        val result = TableGeometryFitter.fitPt(shuffled())
        assertNotNull(result)
        assertEquals(6, result!!.size)
        val ids = result.map { it.first }.toSet()
        assertEquals(PocketId.values().toSet(), ids)
    }

    @Test
    fun `fit assigns correct identity to each point for ideal layout`() {
        val result = TableGeometryFitter.fitPt(shuffled())!!
        val byId = result.associate { it.first to it.second }
        PocketId.values().forEach { id ->
            val expected = idealPts[id]!!
            val actual = byId[id]!!
            assertEquals("${id}.x", expected.x, actual.x, 0.5f)
            assertEquals("${id}.y", expected.y, actual.y, 0.5f)
        }
    }

    @Test
    fun `fit handles layout with small noise on each point`() {
        val noisy = idealPts.values.map { Pt(it.x + (-1..1).random().toFloat(), it.y + (-1..1).random().toFloat()) }
        val result = TableGeometryFitter.fitPt(noisy)
        assertNotNull("Should fit even with small noise", result)
        assertEquals(6, result!!.size)
    }

    @Test
    fun `fit returns null for five points`() {
        val fivePts = shuffled().take(5)
        assertNull(TableGeometryFitter.fitPt(fivePts))
    }

    @Test
    fun `fit returns null for six collinear points`() {
        val collinear = (0..5).map { Pt(it * 10f, 0f) }
        assertNull(TableGeometryFitter.fitPt(collinear))
    }

    @Test
    fun `fit works for 8ft table aspect ratio`() {
        // 8ft: 88" × 44"
        val pts = listOf(
            Pt(-44f, -22f), Pt(44f, -22f),
            Pt(-44f,  22f), Pt(44f,  22f),
            Pt(-44f,   0f), Pt(44f,   0f)
        ).shuffled()
        val result = TableGeometryFitter.fitPt(pts)
        assertNotNull(result)
        assertEquals(6, result!!.size)
    }
}
