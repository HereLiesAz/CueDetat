package com.hereliesaz.cuedetat.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decides whether a [TableScanModel] saved on disk could plausibly describe the table the
 * user is standing at right now.
 *
 * A saved scan used to be restored unconditionally on every launch, then a "you may be at a
 * different table" warning fired if the phone had wandered more than [MAX_DISTANCE_M] away —
 * pinning a months-old table from another bar onto the overlay and blaming the user for it.
 * The scan now stays on disk but is only restored when this says it can be the same table.
 */
object SavedScanPlausibility {

    /** Farther than this from the scan site and it is a different venue, not a different table. */
    const val MAX_DISTANCE_M = 100.0

    private const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * @param model the saved scan.
     * @param current the device's current (lat, lon), or null when no fix is available.
     * @return true if the scan may be restored.
     *
     * - Scan made without location (permission denied at scan time): nothing to compare, so it
     *   is restored, as before.
     * - Scan has a location but the device has none now: it can't be confirmed, so it is not
     *   restored. A wrong table is worse than no table; rescanning fixes the latter.
     * - Otherwise: restored only within [MAX_DISTANCE_M] of the scan site.
     */
    fun isPlausiblySameTable(model: TableScanModel, current: Pair<Double, Double>?): Boolean {
        val lat = model.scanLatitude
        val lon = model.scanLongitude
        if (lat == null || lon == null) return true
        if (current == null) return false
        return distanceMetres(lat, lon, current.first, current.second) <= MAX_DISTANCE_M
    }

    /** Great-circle (haversine) distance in metres. */
    fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
