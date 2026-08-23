package com.hereliesaz.cuedetat.core.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitsTest {

    @Test
    fun imperialAndMetricAgree() {
        assertEquals(0.0254, 1.0.inches.meters, 1e-12)
        assertEquals(12.0, 1.0.feet.inches, 1e-12)
        assertEquals(100.0, 1.0.meters.centimeters, 1e-12)
    }

    @Test
    fun aRegulationBallIsTwoAndAQuarterInches() {
        assertEquals(57.15, 2.25.inches.millimeters, 1e-9)
    }

    @Test
    fun lengthsCompareAndArithmeticStaysDimensional() {
        assertTrue(2.0.meters > 1.0.meters)
        assertEquals(3.0, (1.0.meters + 2.0.meters).meters, 1e-12)
        assertEquals(2.0, (4.0.meters / 2.0.meters), 1e-12) // ratio is dimensionless
    }

    @Test
    fun angleNormalisationWrapsIntoASingleTurn() {
        assertEquals(0.0, 360.0.degrees.normalized().degrees, 1e-9)
        assertEquals(-90.0, 270.0.degrees.normalized().degrees, 1e-9)
        assertEquals(180.0, 180.0.degrees.normalized().degrees, 1e-9)
    }

    @Test
    fun imperialFormattingCarriesTwelveInchesIntoTheNextFoot() {
        // A naive implementation prints 4' 12".
        assertEquals("5' 0\"", (4.999.feet).formatImperial())
        assertEquals("4' 7\"", (4.0.feet + 7.0.inches).formatImperial())
    }

    @Test
    fun metricFormattingIsWholeCentimetres() {
        assertEquals("139 cm", 1.39.meters.formatMetric())
    }

    @Test
    fun approximateComparisonRespectsTolerance() {
        assertTrue(1.0.meters.approximately(1.0002.meters, 1.0.millimeters))
        assertTrue(!1.0.meters.approximately(1.02.meters, 1.0.millimeters))
        assertTrue(0.0.degrees.approximately(359.99.degrees, 0.1.degrees))
    }

    @Test
    fun speedReferencesAreOrdered() {
        assertTrue(Speed.SOFT < Speed.MEDIUM)
        assertTrue(Speed.MEDIUM < Speed.FIRM)
        assertTrue(Speed.FIRM < Speed.BREAK)
    }
}
