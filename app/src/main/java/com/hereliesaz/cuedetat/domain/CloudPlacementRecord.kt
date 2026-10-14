package com.hereliesaz.cuedetat.domain

import kotlin.math.abs

/** One-day ARCore Cloud Anchor contract (API-key auth cannot host longer).
 *
 * This is a LOCAL reference to a Google-hosted visual map. It is not the map
 * itself. Use only after explicit user consent and do not sync it automatically.
 */
data class CloudPlacementRecord(
    val id: String,
    val expiresAtMs: Long,
    val logicalWidth: Float,
    val logicalLength: Float,
    val awaySign: Float,
) {
    fun isUsableAt(nowMs: Long, width: Float, length: Float): Boolean =
        id.isNotBlank() && nowMs < expiresAtMs &&
            logicalWidth.isFinite() && logicalLength.isFinite() &&
            width.isFinite() && length.isFinite() &&
            logicalWidth > 0f && logicalLength > 0f &&
            abs(logicalWidth - width) < 0.01f &&
            abs(logicalLength - length) < 0.01f &&
            (awaySign == 1f || awaySign == -1f)

    companion object {
        const val TTL_DAYS = 1
        const val TTL_MS = 24L * 60L * 60L * 1000L
    }
}
