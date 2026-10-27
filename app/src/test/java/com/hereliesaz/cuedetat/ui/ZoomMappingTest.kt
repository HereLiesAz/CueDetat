package com.hereliesaz.cuedetat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomMappingTest {

    @Test
    fun `the whole slider travel is the whole standard zoom range`() {
        // Independent reference: standard range is 0.2x..4x, so 100 units is 4 / 0.2 = 20x.
        assertEquals(20f, ZoomMapping.unitsToFactor(100f), 1e-3f)
        assertEquals(1f, ZoomMapping.unitsToFactor(0f), 1e-6f)
    }

    @Test
    fun `opposite nudges cancel`() {
        assertEquals(1f, ZoomMapping.unitsToFactor(7.5f) * ZoomMapping.unitsToFactor(-7.5f), 1e-5f)
    }
}
