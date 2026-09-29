package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedScanPlausibilityTest {

    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    private fun scan(
        lat: Double?,
        lon: Double?,
        madeAt: Long = now - 10 * minute,
        usedAt: Long = 0L,
    ) = TableScanModel(
        pockets = emptyList(),
        lensWarpTps = TpsWarpData(srcPoints = listOf(PointF(0f, 0f)), dstPoints = listOf(PointF(0f, 0f))),
        tableSize = TableSize.EIGHT_FT,
        feltColorHsv = listOf(0f, 0f, 0f),
        scanLatitude = lat,
        scanLongitude = lon,
        calibrationTimestamp = madeAt,
        lastUsedTimestamp = usedAt,
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
    fun `scan without location is restored while fresh`() {
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(null, null), now, null))
    }

    @Test
    fun `located scan with no current fix is restored while fresh`() {
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), now, null))
    }

    @Test
    fun `scan 90 minutes old is restored`() {
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(null, null, now - 90 * minute), now, null))
    }

    @Test
    fun `scan 3 hours old is not restored`() {
        assertFalse(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon, now - 180 * minute), now, lat to lon))
    }

    @Test
    fun `legacy scan without timestamp is not restored`() {
        assertFalse(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon, 0L), now, lat to lon))
    }

    @Test
    fun `scan from the future is not restored`() {
        assertFalse(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon, now + 5 * minute), now, lat to lon))
    }

    @Test
    fun `about 55 m away is restored`() {
        // 0.0005 deg latitude ~ 55.6 m.
        assertTrue(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), now, (lat + 0.0005) to lon))
    }

    @Test
    fun `another bar across town is not restored`() {
        // 0.01 deg latitude ~ 1.1 km.
        assertFalse(SavedScanPlausibility.isPlausiblySameTable(scan(lat, lon), now, (lat + 0.01) to lon))
    }

    @Test
    fun `scan made 5 hours ago but used 30 minutes ago is restored`() {
        assertTrue(
            SavedScanPlausibility.isPlausiblySameTable(
                scan(null, null, madeAt = now - 300 * minute, usedAt = now - 30 * minute), now, null
            )
        )
    }

    @Test
    fun `scan last used 3 hours ago is not restored`() {
        assertFalse(
            SavedScanPlausibility.isPlausiblySameTable(
                scan(null, null, madeAt = now - 300 * minute, usedAt = now - 180 * minute), now, null
            )
        )
    }
}
