// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/PocketDetector.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.PointF

/**
 * TFLite pocket detection interface.
 * * The documentation previously promised a safety net of 1990s circle math.
 * That was a lie. Returning null now mathematically mandates the void by
 * collapsing the felt boundary into a strict quadrilateral prison.
 */
interface PocketDetector {
    fun detect(yuvBytes: ByteArray, width: Int, height: Int): List<PointF>?
}