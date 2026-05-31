// app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt
package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.hereliesaz.cuedetat.domain.TableScanModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TableScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val file: File get() = File(context.filesDir, "table_scan_model.json")
    private val feltSamplesFile: File get() = File(context.filesDir, "felt_samples.json")
    private val partialScanFile: File get() = File(context.filesDir, "partial_scan.json")

    fun loadPartialScan(): Pair<com.hereliesaz.cuedetat.ui.composables.tablescan.ScanStep, Map<com.hereliesaz.cuedetat.domain.PocketId, android.graphics.PointF>>? = runCatching {
        if (!partialScanFile.exists()) return@runCatching null
        val type = object : com.google.gson.reflect.TypeToken<Pair<com.hereliesaz.cuedetat.ui.composables.tablescan.ScanStep, Map<com.hereliesaz.cuedetat.domain.PocketId, android.graphics.PointF>>>() {}.type
        gson.fromJson<Pair<com.hereliesaz.cuedetat.ui.composables.tablescan.ScanStep, Map<com.hereliesaz.cuedetat.domain.PocketId, android.graphics.PointF>>>(partialScanFile.readText(), type)
    }.getOrNull()

    fun savePartialScan(step: com.hereliesaz.cuedetat.ui.composables.tablescan.ScanStep, clusters: Map<com.hereliesaz.cuedetat.domain.PocketId, android.graphics.PointF>) {
        runCatching { partialScanFile.writeText(gson.toJson(Pair(step, clusters))) }
    }

    fun clearPartialScan() {
        partialScanFile.delete()
    }

    /** Loads the saved model from disk, or null if none exists. */
    fun load(): TableScanModel? = runCatching {
        if (!file.exists()) return@runCatching null
        gson.fromJson(file.readText(), TableScanModel::class.java)
    }.getOrNull()

    /** Saves the model to disk. Call from a background coroutine. */
    fun save(model: TableScanModel) {
        runCatching { file.writeText(gson.toJson(model)) }
    }

    /** Clears the saved model from disk. */
    fun clear() {
        file.delete()
    }

    /** Loads the saved felt samples from disk. */
    fun loadFeltSamples(): List<com.hereliesaz.cuedetat.domain.FeltSample> = runCatching {
        if (!feltSamplesFile.exists()) return@runCatching emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<com.hereliesaz.cuedetat.domain.FeltSample>>() {}.type
        gson.fromJson<List<com.hereliesaz.cuedetat.domain.FeltSample>>(feltSamplesFile.readText(), type) ?: emptyList()
    }.getOrDefault(emptyList())

    /** Saves the felt samples to disk. */
    fun saveFeltSamples(samples: List<com.hereliesaz.cuedetat.domain.FeltSample>) {
        runCatching { feltSamplesFile.writeText(gson.toJson(samples)) }
    }

    /**
     * Attempts to get the device's current coordinates for coarse "am I at a different
     * table?" matching.
     *
     * Battery: prefers the cached last fix (which never powers up GPS) and only requests
     * an active fix when there is no recent cache. The active request is bounded by
     * [LOCATION_TIMEOUT_MS] so a failed fix can't keep the location radio warm.
     * Returns Pair(lat, lon) or null if location is unavailable or permission is denied.
     * Must be called after the user has granted ACCESS_COARSE_LOCATION.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // Cheap path: a recent cached fix is plenty for venue-level matching.
        val cached = runCatching { client.lastLocation.await() }.getOrNull()
        if (cached != null && cached.isFresh()) {
            return Pair(cached.latitude, cached.longitude)
        }

        // No usable cache: request one active fix, time-bounded.
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        cont.resume(location?.let { Pair(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
        // Fall back to a stale cache rather than nothing if the active fix timed out.
        return fresh ?: cached?.let { Pair(it.latitude, it.longitude) }
    }

    private fun Location.isFresh(): Boolean {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000L
        return ageMs in 0..LOCATION_FRESH_MS
    }

    private companion object {
        const val LOCATION_FRESH_MS = 10 * 60 * 1000L // cached fix considered usable for 10 min
        const val LOCATION_TIMEOUT_MS = 5_000L        // cap on an active GPS request
    }
}
