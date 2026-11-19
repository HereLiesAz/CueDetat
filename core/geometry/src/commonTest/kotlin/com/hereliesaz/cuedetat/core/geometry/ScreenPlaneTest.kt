package com.hereliesaz.cuedetat.core.geometry

import com.hereliesaz.cuedetat.core.units.meters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Expected values are worked from the definition (long axis: screen y <-> table x; short axis:
 * screen x <-> table y) and from the physical size of an 8 ft table (88 x 44 in = 2.2352 x 1.1176 m),
 * not read back from the code under test.
 */
class ScreenPlaneTest {

    private val plane = ScreenPlane(unitsPerMeter = 1000.0)
    private val eps = 1e-9

    @Test
    fun theScreensYIsTheTablesLongAxis() {
        // 1 m down the screen is 1 m along the table's +x, and nothing across.
        val p = plane.toTable(x = 0.0, y = 1000.0)
        assertEquals(1.0, p.x, eps)
        assertEquals(0.0, p.y, eps)
    }

    @Test
    fun theScreensXIsTheTablesShortAxis() {
        val p = plane.toTable(x = 250.0, y = 0.0)
        assertEquals(0.0, p.x, eps)
        assertEquals(0.25, p.y, eps)
    }

    @Test
    fun aPortraitTableLandsExactlyOnTheCoreTable() {
        val table = Table(TableSpec.EIGHT_FOOT)
        val halfLongSideUnits = 1.1176 * 1000.0   // 44 in in metres, in screen units
        val halfShortSideUnits = 0.5588 * 1000.0  // 22 in

        // The foot rail's centre (bottom of the screen) and the right rail's centre.
        assertTrue(table.containsCenter(plane.toTable(0.0, halfLongSideUnits)))
        assertTrue(table.containsCenter(plane.toTable(halfShortSideUnits, 0.0)))
        // A hair further along the long axis is off the table; so is a hair further across.
        assertTrue(!table.containsCenter(plane.toTable(0.0, halfLongSideUnits + 1.0)))
        assertTrue(!table.containsCenter(plane.toTable(halfShortSideUnits + 1.0, 0.0)))
        // The wrong way round, which is what a table drawn landscape would be: the long side across.
        assertTrue(!table.containsCenter(plane.toTable(halfLongSideUnits, 0.0)))
    }

    @Test
    fun aCounterClockwiseTurnAsSeenIsCounterClockwiseInTheTablePlane() {
        // On the screen: right, then up (y is down, so up is -y). Right -> up is a quarter turn
        // counter-clockwise as the viewer sees it.
        val right = plane.toTable(1.0, 0.0)
        val up = plane.toTable(0.0, -1.0)
        // Counter-clockwise from `right` to `up` means a positive cross product in a right-handed frame.
        assertTrue(right cross up > 0.0, "right x up = ${right cross up}")
        // And a quarter turn counter-clockwise in the table plane takes right to up exactly.
        val turned = right.perpendicular
        assertEquals(up.x, turned.x, eps)
        assertEquals(up.y, turned.y, eps)
    }

    @Test
    fun theWayBackIsTheInverse() {
        for ((x, y) in listOf(0.0 to 0.0, 123.5 to -987.25, -40.0 to 2235.2, 558.8 to 1117.6)) {
            val back = plane.toTable(x, y)
            assertEquals(x, plane.screenX(back), 1e-9)
            assertEquals(y, plane.screenY(back), 1e-9)
        }
    }

    @Test
    fun lengthsCrossAtTheScale() {
        assertEquals(0.5, plane.length(500.0).meters, eps)
        assertEquals(2000.0, plane.units(2.0.meters), eps)
    }

    @Test
    fun aNonsenseScaleIsRefused() {
        assertFailsWith<IllegalArgumentException> { ScreenPlane(0.0) }
        assertFailsWith<IllegalArgumentException> { ScreenPlane(-1.0) }
        assertFailsWith<IllegalArgumentException> { ScreenPlane(Double.NaN) }
    }
}
