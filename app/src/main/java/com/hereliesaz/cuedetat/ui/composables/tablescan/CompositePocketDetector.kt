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

    override fun detect(bitmap: Bitmap): List<PointF>? {
        val allResults = mutableListOf<PointF>()
        
        detectors.forEachIndexed { index, detector ->
            try {
                val results = detector.detect(bitmap)
                if (results != null) {
                    Log.d("CompositePocketDetector", "Detector $index found ${results.size} pockets")
                    allResults.addAll(results)
                }
            } catch (e: Exception) {
                Log.e("CompositePocketDetector", "Detector $index failed: ${e.message}")
            }
        }
        
        // Simple deduplication based on distance (e.g., 20px)
        return if (allResults.isNotEmpty()) {
            deduplicate(allResults)
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
