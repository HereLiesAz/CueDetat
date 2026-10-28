package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableFitStabilizerTest {

    private fun quad(dx: Float = 0f, dy: Float = 0f) =
        listOf(Pt(100f + dx, 100f + dy), Pt(400f + dx, 100f + dy), Pt(400f + dx, 300f + dy), Pt(100f + dx, 300f + dy))

    @Test
    fun `identical fits are fully steady once there are enough of them`() {
        val s = TableFitStabilizer()
        var out = s.add(quad())
        repeat(4) { out = s.add(quad()) }
        assertEquals(1f, out.stability, 1e-4f)
        assertEquals(100f, out.quad[0].x, 1e-4f)
    }

    @Test
    fun `a lone fit is not steady`() {
        assertEquals(0f, TableFitStabilizer().add(quad()).stability, 1e-6f)
    }

    @Test
    fun `one wild frame does not move the outline`() {
        val s = TableFitStabilizer()
        repeat(4) { s.add(quad()) }
        val out = s.add(quad(dx = 40f))
        assertEquals(100f, out.quad[0].x, 1e-4f)
        assertTrue("a scattering fit should not read as steady: ${out.stability}", out.stability < 0.5f)
    }

    @Test
    fun `scatter lowers stability`() {
        val steady = TableFitStabilizer().apply { repeat(5) { add(quad()) } }.add(quad())
        val noisy = TableFitStabilizer().apply {
            repeat(5) { add(quad(dx = if (it % 2 == 0) 8f else -8f)) }
        }.add(quad(dx = 8f))
        assertTrue(noisy.stability < steady.stability)
    }

    @Test
    fun `a table that really moved restarts the history`() {
        val s = TableFitStabilizer()
        repeat(5) { s.add(quad()) }
        val out = s.add(quad(dx = 300f))
        assertEquals(1, out.count)
        assertEquals(400f, out.quad[0].x, 1e-4f)
    }

    @Test
    fun `quality is overlap discounted by instability`() {
        assertEquals(0.9f, TableFitQuality.of(0.9f, 1f), 1e-5f)
        assertEquals(0.9f * 0.6f, TableFitQuality.of(0.9f, 0f), 1e-5f)
    }
}
