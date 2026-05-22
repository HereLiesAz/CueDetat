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
}
