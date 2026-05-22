// app/src/main/java/com/hereliesaz/cuedetat/domain/TableScanModel.kt
package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import androidx.annotation.Keep
import com.hereliesaz.cuedetat.view.state.TableSize

@Keep
enum class PocketId { TL, TR, BL, BR, SL, SR }

/**
 * One pocket's accumulated observation data.
 *
 * logicalPosition: current best estimate of pocket centre in logical (inch) space.
 * observationCount: number of times this pocket has been detected. More observations → lower variance.
 * variance: spread of detections (logical inches²). Shrinks with more observations.
 */
@Keep
data class PocketCluster(
    val identity: PocketId,
    val logicalPosition: PointF,
    val observationCount: Int,
    val variance: Float
)

/**
 * Persistent table model produced by the scan phase.
 *
 * Stored separately from CueDetatState (via TableScanRepository) so it survives
 * app reinstalls and does not bloat the main preferences store.
 * CueDetatState.tableScanModel holds the runtime in-memory copy only (@Transient).
 *
 * feltColorHsv: mean HSV of the table felt, sampled during scan. Used to seed
 * the colour-based edge detector during AR tracking.
 *
 * scanLatitude/scanLongitude: GPS coordinates at scan time, or null if permission
 * was denied. Used to prompt rescan when the user is > 100 m away.
 */
@Keep
data class TableScanModel(
    val pockets: List<PocketCluster>,       // 6 entries, one per PocketId in declaration order
    val lensWarpTps: TpsWarpData,
    val tableSize: TableSize,
    val feltColorHsv: List<Float>,          // 3-element [H, S, V]
    val scanLatitude: Double?,
    val scanLongitude: Double?,
    val pocketSurroundHistograms: Map<PocketId, List<Float>>? = null,
    val calibrationTimestamp: Long = 0L
)
