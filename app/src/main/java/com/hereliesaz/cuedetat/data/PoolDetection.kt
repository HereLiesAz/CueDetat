package com.hereliesaz.cuedetat.data

import android.graphics.RectF

/**
 * One YOLO detection from the pocket/pool TFLite model.
 *
 * Class IDs come from the trained model's `data.yaml`:
 *   0 = pool-table   1 = pool-table-hole   2 = pool-table-side
 */
data class PoolDetection(
    val rect: RectF,
    val confidence: Float,
    val classId: Int,
)
