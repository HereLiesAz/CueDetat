package com.hereliesaz.cuedetat.domain

import com.hereliesaz.cuedetat.domain.TableFrameHomography.Vec3
import com.hereliesaz.cuedetat.domain.TableOrientationGuess.Candidate
import com.hereliesaz.cuedetat.domain.TableOrientationGuess.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Expected values are worked by hand from the rules in the KDoc: noisy-or for agreement, half the
 * dissenter's confidence off for a conflict, the end nearer 0 (or nearer memory) for an ambiguous source.
 */
class TableOrientationGuessTest {

    private fun memory(heading: Float, c: Float) = Candidate(Source.MEMORY, heading, ambiguousEnds = false, confidence = c)
    private fun plane(heading: Float, c: Float) = Candidate(Source.PLANE, heading, ambiguousEnds = true, confidence = c)

    // The camera in the preview's own basis: away = -z, right = (-away.z, away.x) = +x.
    private val away = Vec3(0f, 0f, -1f)
    private val right = Vec3(1f, 0f, 0f)

    @Test
    fun `a table that lies the way the camera faces is heading 0, one with its far end to the right is 90`() {
        assertEquals(0f, TableOrientationGuess.headingOf(away, right, away), 1e-4f)
        assertEquals(90f, TableOrientationGuess.headingOf(right, right, away), 1e-4f)
        assertEquals(-90f, TableOrientationGuess.headingOf(Vec3(-1f, 0f, 0f), right, away), 1e-4f)
        assertEquals(180f, abs(TableOrientationGuess.headingOf(Vec3(0f, 0f, 1f), right, away)), 1e-4f)
        // 45 degrees to the right of ahead; the vertical part of the axis counts for nothing.
        assertEquals(45f, TableOrientationGuess.headingOf(Vec3(0.7071f, 5f, -0.7071f), right, away), 1e-2f)
    }

    @Test
    fun `nothing worth having, nothing returned`() {
        assertNull(TableOrientationGuess.choose(emptyList()))
        assertNull(TableOrientationGuess.choose(listOf(memory(30f, 0.1f), plane(10f, 0.05f))))
        assertNull(TableOrientationGuess.choose(listOf(memory(Float.NaN, 0.9f))))
    }

    @Test
    fun `a lone memory is taken as it is`() {
        val g = TableOrientationGuess.choose(listOf(memory(30f, 0.8f)))!!
        assertEquals(30f, g.headingDeg, 1e-3f)
        assertEquals(0.8f, g.confidence, 1e-4f)
        assertEquals(setOf(Source.MEMORY), g.sources)
    }

    @Test
    fun `a lone plane, which cannot tell its ends, is turned to the end nearer the camera facing`() {
        // 150 and -30 are the two ends; -30 is nearer 0.
        assertEquals(-30f, TableOrientationGuess.choose(listOf(plane(150f, 0.3f)))!!.headingDeg, 1e-3f)
        assertEquals(30f, TableOrientationGuess.choose(listOf(plane(30f, 0.3f)))!!.headingDeg, 1e-3f)
        // 90 and -90 are as near as each other: the stated one stands.
        assertEquals(90f, TableOrientationGuess.choose(listOf(plane(90f, 0.3f)))!!.headingDeg, 1e-3f)
    }

    @Test
    fun `memory settles which end of the plane is which`() {
        // The plane says 5 or 185; memory says 170. 185 is the end near memory.
        val g = TableOrientationGuess.choose(listOf(memory(170f, 0.6f), plane(5f, 0.3f)))!!
        assertTrue("heading ${g.headingDeg}", abs(TableOrientationGuess.wrap(g.headingDeg - 175f)) < 8f)
        assertEquals(setOf(Source.MEMORY, Source.PLANE), g.sources)
    }

    @Test
    fun `two sources that agree are surer than either`() {
        // 1 - (1 - 0.5) * (1 - 0.3) = 0.65
        val g = TableOrientationGuess.choose(listOf(memory(10f, 0.5f), plane(15f, 0.3f)))!!
        assertEquals(0.65f, g.confidence, 1e-4f)
        // Weighted by confidence: 10 * 5/8 + 15 * 3/8 is about 11.9.
        assertEquals(11.9f, g.headingDeg, 0.3f)
    }

    @Test
    fun `a source that disagrees takes half its confidence off the leader's`() {
        // Memory at 0 with 0.6; the plane's 90/-90 ends are equally far, so it stands at 90, 90 degrees off.
        val g = TableOrientationGuess.choose(listOf(memory(0f, 0.6f), plane(90f, 0.4f)))!!
        assertEquals(0f, g.headingDeg, 1e-3f)
        assertEquals(0.6f * (1f - 0.5f * 0.4f), g.confidence, 1e-4f) // 0.48
        assertEquals(setOf(Source.MEMORY), g.sources)
    }

    @Test
    fun `the wrap at 180 is no disagreement`() {
        // 175 and -175 are 10 degrees apart.
        val g = TableOrientationGuess.choose(listOf(memory(175f, 0.5f), memory(-175f, 0.5f)))!!
        assertEquals(0.75f, g.confidence, 1e-4f)
        assertTrue("heading ${g.headingDeg}", abs(abs(g.headingDeg) - 180f) < 1f)
    }

    // --- PlaneShape

    private fun rectangle(longSide: Float, shortSide: Float, turnDeg: Float, alongX: Boolean = true): FloatArray {
        val hx = if (alongX) longSide / 2f else shortSide / 2f
        val hz = if (alongX) shortSide / 2f else longSide / 2f
        val corners = listOf(-hx to -hz, hx to -hz, hx to hz, -hx to hz)
        val a = Math.toRadians(turnDeg.toDouble())
        return corners.flatMap { (x, z) ->
            listOf((x * cos(a) - z * sin(a)).toFloat(), (x * sin(a) + z * cos(a)).toFloat())
        }.toFloatArray()
    }

    @Test
    fun `the box of a rectangle is the rectangle, whichever way it lies`() {
        val flat = PlaneShape.boundingBox(rectangle(2.4f, 1.2f, 0f))!!
        assertEquals(2.4f, flat.longSide, 0.01f)
        assertEquals(1.2f, flat.shortSide, 0.01f)
        assertEquals(0f, flat.angleDeg, 0.5f)
        assertEquals(1f, flat.rectangularity, 0.01f)

        assertEquals(30f, PlaneShape.boundingBox(rectangle(2.4f, 1.2f, 30f))!!.angleDeg, 1f)
        // Long along z in the plane's frame: 90 degrees from x.
        val along = PlaneShape.boundingBox(rectangle(2.4f, 1.2f, 0f, alongX = false))!!
        assertEquals(90f, along.angleDeg, 0.5f)
        assertEquals(2.4f, along.longSide, 0.01f)
    }

    @Test
    fun `an L shape fills less of its box than a table top does`() {
        // 2 x 2 box with a 1 x 1 corner cut out: area 3 of 4.
        val l = floatArrayOf(0f, 0f, 2f, 0f, 2f, 1f, 1f, 1f, 1f, 2f, 0f, 2f)
        val box = PlaneShape.boundingBox(l)!!
        assertEquals(0.75f, box.rectangularity, 0.02f)
    }

    @Test
    fun `no box from too few points, from non-finite ones, or from nothing with area`() {
        assertNull(PlaneShape.boundingBox(floatArrayOf(0f, 0f, 1f, 1f)))
        assertNull(PlaneShape.boundingBox(floatArrayOf(0f, 0f, 1f, 0f, Float.NaN, 1f)))
        assertNull(PlaneShape.boundingBox(floatArrayOf(0f, 0f, 1f, 0f, 2f, 0f)))
    }

    @Test
    fun `a table-sized table-shaped plane earns the cap, and a floor, or a round one, earns nothing`() {
        val table = PlaneShape.boundingBox(rectangle(2.54f, 1.27f, 20f))!!
        assertEquals(TableOrientationGuess.PLANE_CONFIDENCE_CAP, PlaneShape.confidence(table, 2.54f, 1.27f), 0.02f)

        val floor = PlaneShape.boundingBox(rectangle(8f, 6f, 0f))!!
        assertEquals(0f, PlaneShape.confidence(floor, 2.54f, 1.27f), 1e-3f)

        // A circle of the table's diameter: fills 0.785 of its box, and is not twice as long as wide.
        val circle = FloatArray(72) { i ->
            val a = Math.toRadians((i / 2) * 10.0)
            (1.27 * if (i % 2 == 0) cos(a) else sin(a)).toFloat()
        }
        val round = PlaneShape.boundingBox(circle)!!
        assertTrue(PlaneShape.confidence(round, 2.54f, 1.27f) < 0.02f)

        assertNotNull(table)
    }

    @Test
    fun `half the right size still counts for something, but less`() {
        val half = PlaneShape.boundingBox(rectangle(1.8f, 0.9f, 0f))!!
        val c = PlaneShape.confidence(half, 2.54f, 1.27f)
        assertTrue("confidence $c", c > 0.05f && c < PlaneShape.confidence(PlaneShape.boundingBox(rectangle(2.54f, 1.27f, 0f))!!, 2.54f, 1.27f))
    }
}
