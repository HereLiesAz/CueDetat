// app/src/main/java/com/hereliesaz/cuedetat/data/TableScanRepository.kt
package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hereliesaz.cuedetat.domain.TableScanModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TableScanRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder().create()
    private val file: File get() = File(context.filesDir, "table_scan_model.json")

    /** Loads the saved model from disk, or null if none exists. */
    fun load(): TableScanModel? = runCatching {
        if (!file.exists()) return null
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

    /**
     * Attempts to get the device's current GPS coordinates.
     * Returns Pair(lat, lon) or null if location is unavailable or permission is denied.
     * Must be called after the user has granted ACCESS_COARSE_LOCATION.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    cont.resume(location?.let { Pair(it.latitude, it.longitude) })
                }
                .addOnFailureListener { cont.resume(null) }
        }
}
