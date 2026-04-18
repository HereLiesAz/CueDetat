package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log

/**
 * Runs multiple pocket detectors side-by-side and combines their results.
 * This is useful for comparing TFLite vs ONNX performance or increasing recall.
 */
class CompositePocketDetector(
    private val detectors: List<PocketDetector>
) : PocketDetector {

    override fun detect(bitmap: Bitmap): MlTableDetection? {
        val allPockets = mutableListOf<PointF>()
        var tableBoundary: android.graphics.RectF? = null
        var totalConfidence = 0f
        var detectorCount = 0
        
        detectors.forEachIndexed { index, detector ->
            try {
                val result = detector.detect(bitmap)
                if (result != null) {
                    Log.d("CompositePocketDetector", "Detector $index found ${result.pockets.size} pockets with confidence ${result.confidence}")
                    allPockets.addAll(result.pockets)
                    if (result.tableBoundary != null && tableBoundary == null) {
                        tableBoundary = result.tableBoundary
                    }
                    totalConfidence += result.confidence
                    detectorCount++
                }
            } catch (e: Exception) {
                Log.e("CompositePocketDetector", "Detector $index failed: ${e.message}")
            }
        }
        
        val uniquePockets = deduplicate(allPockets)
        val avgConfidence = if (detectorCount > 0) totalConfidence / detectorCount else 0f

        return if (uniquePockets.isNotEmpty() || tableBoundary != null) {
            MlTableDetection(
                tableBoundary = tableBoundary,
                pockets = uniquePockets,
                confidence = avgConfidence
            )
        } else null
    }

    private fun deduplicate(points: List<PointF>): List<PointF> {
        val unique = mutableListOf<PointF>()
        val thresholdSq = 400f // 20px squared
        
        for (p in points) {
            var foundDuplicate = false
            for (u in unique) {
                val dx = p.x - u.x
                val dy = p.y - u.y
                if (dx * dx + dy * dy < thresholdSq) {
                    foundDuplicate = true
                    break
                }
            }
            if (!foundDuplicate) {
                unique.add(p)
            }
        }
        return unique
    }
}
