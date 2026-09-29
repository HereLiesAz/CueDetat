package com.hereliesaz.cuedetat.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decides whether a [TableScanModel] saved on disk could plausibly describe the table the
 * user is standing at right now.
 *
 * The saved scan exists so a phone pulled out of a pocket mid-game doesn't demand a rescan.
 * It is not a long-term memory: lighting shifts through the day and a scan is quick, so a
 * scan older than [MAX_AGE_MS] is not restored. It used to be restored forever, then a
 * "you may be at a different table" warning blamed the user for the result.
 */
object SavedScanPlausibility {

    /** A game session, generously. Past this the scan is stale (light, table, venue). */
    const val MAX_AGE_MS = 2L * 60 * 60 * 1000

    /** Farther than this from the scan site and it is a different venue, not a different table. */
    const val MAX_DISTANCE_M = 100.0

    private const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * @param model the saved scan.
     * @param nowMs current wall-clock time, epoch millis.
     * @param current the device's current (lat, lon), or null when no fix is available.
     * @return true if the scan may be restored.
     *
     * - Age: restored only if made within [MAX_AGE_MS]. Scans with no timestamp (0, legacy) or
     *   one in the future (clock change) are not restored.
     * - Location: a veto only. If both the scan and the device have a fix and they are more
     *   than [MAX_DISTANCE_M] apart, not restored. A missing fix vetoes nothing — a basement
     *   bar with no signal mid-game still gets its table back.
     */
    fun isPlausiblySameTable(
        model: TableScanModel,
        nowMs: Long,
        current: Pair<Double, Double>?,
    ): Boolean {
        val age = nowMs - model.calibrationTimestamp
        if (model.calibrationTimestamp <= 0L || age < 0 || age > MAX_AGE_MS) return false
        val lat = model.scanLatitude ?: return true
        val lon = model.scanLongitude ?: return true
        if (current == null) return true
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
