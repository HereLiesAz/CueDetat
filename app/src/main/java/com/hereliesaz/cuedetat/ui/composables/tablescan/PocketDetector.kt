// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/PocketDetector.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.PointF

/**
 * Strategy interface for pocket detection.
 *
 * The default implementation used in 1.3 is the Hough-circle fallback built
 * into [TableScanAnalyzer]. In v1.4, a TFLite-backed implementation can be
 * provided and passed to [TableScanAnalyzer] without touching any other code.
 *
 * @see TableScanAnalyzer
 */
interface PocketDetector {

    /**
     * Detect pocket centre positions in a single camera frame.
     *
     * @param yBytes  Raw Y-plane bytes from a YUV_420_888 [android.media.Image].
     * @param width   Original image width in pixels.
     * @param height  Original image height in pixels.
     * @return List of detected pocket centres in original image coordinates,
     *         or `null` if the detector cannot produce a result for this frame
     *         (e.g. model not ready, inference failed). Returning `null` causes
     *         [TableScanAnalyzer] to fall back to the Hough-circle pipeline.
     */
    fun detect(yBytes: ByteArray, width: Int, height: Int): List<PointF>?
}
