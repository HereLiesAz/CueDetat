package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedScanPlausibilityTest {

    private fun scan(lat: Double?, lon: Double?) = TableScanModel(
        pockets = emptyList(),
        lensWarpTps = TpsWarpData(srcPoints = listOf(PointF(0f, 0f)), dstPoints = listOf(PointF(0f, 0f))),
        tableSize = TableSize.EIGHT_FT,
        feltColorHsv = listOf(0f, 0f, 0f),
        scanLatitude = lat,
        scanLongitude = lon,
    )

    // Jackson Square, New Orleans.
    private val lat = 29.9574
    private val lon = -90.0629

    @Test
    fun `one degree of latitude is about 111 km`() {
        // Independent figure: meridian arc of 1 deg on a 6371 km sphere = 2*pi*6371/360 = 111.19 km.
        assertEquals(111_195.0, SavedScanPlausibility.distanceMetres(0.0, 0.0, 1.0, 0.0), 5.0)
    }

    @Test
    fun `scan without location is restored`() {
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(null, null), null))
    }

    @Test
    fun `located scan with no current fix is not restored`() {
        assertFalse(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), null))
    }

    @Test
    fun `same spot is restored`() {
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), lat to lon))
    }

    @Test
    fun `about 55 m away is restored`() {
        // 0.0005 deg latitude ~ 55.6 m.
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), (lat + 0.0005) to lon))
    }

    @Test
    fun `another bar across town is not restored`() {
        // 0.01 deg latitude ~ 1.1 km.
        assertFalse(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), (lat + 0.01) to lon))
    }
}
