// app/src/test/java/com/hereliesaz/cuedetat/domain/ThinPlateSplineTest.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Test

class ThinPlateSplineTest {

    private fun assertPointF(expected: PointF, actual: PointF, delta: Float = 0.05f) {
        assertEquals("x mismatch", expected.x, actual.x, delta)
        assertEquals("y mismatch", expected.y, actual.y, delta)
    }

    private fun sixPts(vararg coords: Float): List<PointF> {
        require(coords.size == 12)
        return (0 until 6).map { PointF(coords[it * 2], coords[it * 2 + 1]) }
    }

    @Test
    fun `identity warp - src equals dst - point maps to itself`() {
        val pts = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        val tps = TpsWarpData(srcPoints = pts, dstPoints = pts)
        assertPointF(PointF(18f, 36f), ThinPlateSpline.applyWarp(tps, PointF(18f, 36f)))
        assertPointF(PointF(5f, 10f), ThinPlateSpline.applyWarp(tps, PointF(5f, 10f)))
    }

    @Test
    fun `pure translation warp - all control points shifted - interior point shifts equally`() {
        val src = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        val dst = src.map { PointF(it.x + 5f, it.y + 3f) }
        val tps = TpsWarpData(srcPoints = src, dstPoints = dst)
        assertPointF(PointF(23f, 39f), ThinPlateSpline.applyWarp(tps, PointF(18f, 36f)))
    }

    @Test
    fun `applyWarp passes through all six control points exactly`() {
        val src = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        // Simulate small lens distortion residuals
        val dst = sixPts(0.4f, 0.3f, 35.8f, 0.2f, 35.9f, 72.1f, 0.2f, 71.9f, -0.8f, 36.1f, 37.1f, 35.9f)
        val tps = TpsWarpData(srcPoints = src, dstPoints = dst)
        for (i in src.indices) {
            assertPointF(dst[i], ThinPlateSpline.applyWarp(tps, src[i]), delta = 0.1f)
        }
    }

    @Test
    fun `applyInverseWarp passes through dst control points`() {
        val src = sixPts(0f, 0f, 36f, 0f, 36f, 72f, 0f, 72f, -1f, 36f, 37f, 36f)
        val dst = sixPts(0.4f, 0.3f, 35.8f, 0.2f, 35.9f, 72.1f, 0.2f, 71.9f, -0.8f, 36.1f, 37.1f, 35.9f)
        val tps = TpsWarpData(srcPoints = src, dstPoints = dst)
        for (i in dst.indices) {
            assertPointF(src[i], ThinPlateSpline.applyInverseWarp(tps, dst[i]), delta = 0.1f)
        }
    }
}
