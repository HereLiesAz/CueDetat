package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.cuedetat.domain.TablePosePrior
import com.hereliesaz.cuedetat.domain.TablePoseSample
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers confirmed table poses at three depths (see [TablePosePrior]):
 * - the session's latest pose, in memory;
 * - every table's poses, on disk, keyed by where it was played (GPS);
 * - which table was used last, so it can serve as a weak guess anywhere.
 *
 * Also appends every confirmed pose, with its sensor readings, to a JSON-lines log
 * ([LOG_FILE]) kept for training a proper orientation model later. The log holds no location
 * finer than about 1 km.
 *
 * Disk access is synchronous and small; call from a background dispatcher.
 */
@Singleton
class TablePoseStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    /** One physical table: where it is and the poses confirmed at it. */
    @Keep // Serialised with Gson; R8 must keep field names.
    data class TableRecord(
        val lat: Double?,
        val lon: Double?,
        val tableSize: String,
        val samples: List<TablePoseSample>,
        val lastUsedMs: Long,
    )

    private val file: File get() = File(context.filesDir, "table_poses.json")
    private val logFile: File get() = File(context.filesDir, LOG_FILE)

    @Volatile var sessionSample: TablePoseSample? = null
        private set

    private var cache: MutableList<TableRecord>? = null

    @Synchronized
    private fun records(): MutableList<TableRecord> = cache ?: runCatching {
        if (!file.exists()) return@runCatching mutableListOf<TableRecord>()
        val type = object : TypeToken<List<TableRecord>>() {}.type
        gson.fromJson<List<TableRecord>>(file.readText(), type)?.toMutableList() ?: mutableListOf()
    }.getOrDefault(mutableListOf()).also { cache = it }

    /** Poses for the table nearest [lat]/[lon], if one is within [TablePosePrior.SAME_TABLE_METERS]. */
    @Synchronized
    fun samplesAt(lat: Double?, lon: Double?): List<TablePoseSample>? {
        if (lat == null || lon == null) return null
        return nearest(lat, lon)?.samples
    }

    /** Poses for the most recently used table, wherever it was. */
    @Synchronized
    fun lastTableSamples(): List<TablePoseSample>? = records().maxByOrNull { it.lastUsedMs }?.samples

    /**
     * Records a confirmed pose: becomes the session pose, joins its table's record (created if
     * new) and is appended to the training log.
     */
    @Synchronized
    fun record(sample: TablePoseSample, lat: Double?, lon: Double?, tableSize: String) {
        sessionSample = sample
        val all = records()
        val existing = if (lat != null && lon != null) nearest(lat, lon) else null
        val updated = if (existing != null) {
            all.remove(existing)
            existing.copy(
                samples = (existing.samples + sample).takeLast(MAX_SAMPLES_PER_TABLE),
                tableSize = tableSize,
                lastUsedMs = sample.timestampMs,
            )
        } else {
            TableRecord(lat, lon, tableSize, listOf(sample), sample.timestampMs)
        }
        all.add(updated)
        while (all.size > MAX_TABLES) all.remove(all.minByOrNull { it.lastUsedMs })
        runCatching { file.writeText(gson.toJson(all)) }
        appendLog(sample, lat, lon, tableSize)
    }

    private fun nearest(lat: Double, lon: Double): TableRecord? = records()
        .filter { it.lat != null && it.lon != null }
        .map { it to TablePosePrior.distanceMeters(lat, lon, it.lat!!, it.lon!!) }
        .filter { it.second <= TablePosePrior.SAME_TABLE_METERS }
        .minByOrNull { it.second }?.first

    private fun appendLog(sample: TablePoseSample, lat: Double?, lon: Double?, tableSize: String) {
        runCatching {
            if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
                // Keep the newer half.
                val lines = logFile.readLines()
                logFile.writeText(lines.takeLast(lines.size / 2).joinToString("\n", postfix = "\n"))
            }
            val entry = mapOf(
                "sample" to sample,
                "tableSize" to tableSize,
                // ~1 km: enough to group sessions by venue, not enough to locate anyone.
                "latCoarse" to lat?.let { Math.round(it * 100) / 100.0 },
                "lonCoarse" to lon?.let { Math.round(it * 100) / 100.0 },
            )
            logFile.appendText(gson.toJson(entry) + "\n")
        }
    }

    companion object {
        const val LOG_FILE = "table_pose_log.jsonl"
        const val MAX_SAMPLES_PER_TABLE = 40
        const val MAX_TABLES = 50
        const val MAX_LOG_BYTES = 5L * 1024 * 1024
    }
}
