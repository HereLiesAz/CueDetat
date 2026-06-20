package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF

data class MlTableDetection(
    val tableBoundary: RectF? = null,
    val pockets: List<PointF> = emptyList(),
    val confidence: Float = 0f
)

/**
 * TFLite pocket detection interface.
 */
interface PocketDetector {
    fun detect(bitmap: Bitmap): MlTableDetection?

    /**
     * Ensures the backing model is installed and loaded, requesting the
     * on-demand dynamic feature split if necessary (Play builds). Returns true
     * when the model is ready. Default no-op (true) for detectors that have no
     * model to fetch. Safe to call repeatedly; cheap once loaded.
     */
    suspend fun ensureModelReady(): Boolean = true
}
