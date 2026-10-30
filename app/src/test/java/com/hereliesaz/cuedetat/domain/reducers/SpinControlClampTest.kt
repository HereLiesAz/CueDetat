package com.hereliesaz.cuedetat.domain.reducers

import org.junit.Assert.assertEquals
import org.junit.Test

/** The spin/masse dial must always keep its whole 120 dp square on screen. */
class SpinControlClampTest {

    // density 2: the dial's half-size is 120 px.
    @Test
    fun `a centre inside the bounds is left alone`() {
        assertEquals(500f, clampSpinControlCoordinate(500f, 1080, 2f), 0f)
    }

    @Test
    fun `a centre dragged past either edge is pulled back to the dial's half-size`() {
        assertEquals(120f, clampSpinControlCoordinate(-40f, 1080, 2f), 0f)
        assertEquals(960f, clampSpinControlCoordinate(5000f, 1080, 2f), 0f)
    }

    @Test
    fun `a rotation to a smaller dimension pulls a stranded centre back in`() {
        // Portrait y = 2000 on a 2400 px-high view; landscape is 1080 px high.
        assertEquals(960f, clampSpinControlCoordinate(2000f, 1080, 2f), 0f)
    }

    @Test
    fun `a view smaller than the dial centres it instead of inverting the range`() {
        assertEquals(100f, clampSpinControlCoordinate(0f, 200, 2f), 0f)
    }
}
