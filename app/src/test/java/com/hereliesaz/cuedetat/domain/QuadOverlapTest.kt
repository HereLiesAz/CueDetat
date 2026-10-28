package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Pt
import org.junit.Assert.assertEquals
import org.junit.Test

class QuadOverlapTest {

    private fun square(x: Float, y: Float, s: Float = 1f) =
        listOf(Pt(x, y), Pt(x + s, y), Pt(x + s, y + s), Pt(x, y + s))

    @Test
    fun `the same quad overlaps itself completely`() {
        assertEquals(1f, QuadOverlap.iou(square(0f, 0f), square(0f, 0f)), 1e-5f)
    }

    @Test
    fun `apart is zero`() {
        assertEquals(0f, QuadOverlap.iou(square(0f, 0f), square(5f, 5f)), 1e-5f)
    }

    @Test
    fun `half-shifted unit squares overlap a third`() {
        // Independent reference: intersection 0.5, union 1 + 1 - 0.5 = 1.5.
        assertEquals(1f / 3f, QuadOverlap.iou(square(0f, 0f), square(0.5f, 0f)), 1e-5f)
    }

    @Test
    fun `winding does not matter`() {
        assertEquals(
            QuadOverlap.iou(square(0f, 0f), square(0.5f, 0f)),
            QuadOverlap.iou(square(0f, 0f).reversed(), square(0.5f, 0f)),
            1e-5f,
        )
    }

    @Test
    fun `a small square inside a big one is the ratio of their areas`() {
        assertEquals(0.25f, QuadOverlap.iou(square(0f, 0f, 2f), square(0.5f, 0.5f, 1f)), 1e-5f)
    }

    @Test
    fun `a diamond on a square overlaps by the analytic amount`() {
        // Unit square centred at the origin vs the same square turned 45 degrees: the octagon
        // between them has area 2(sqrt2 - 1) = 0.8284; union = 1 + 1 - 0.8284.
        val h = 0.5f
        val r = (kotlin.math.sqrt(2.0) / 2.0).toFloat()
        val a = listOf(Pt(-h, -h), Pt(h, -h), Pt(h, h), Pt(-h, h))
        val b = listOf(Pt(0f, -r), Pt(r, 0f), Pt(0f, r), Pt(-r, 0f))
        val inter = 2f * (kotlin.math.sqrt(2.0).toFloat() - 1f)
        assertEquals(inter / (2f - inter), QuadOverlap.iou(a, b), 1e-4f)
    }
}
