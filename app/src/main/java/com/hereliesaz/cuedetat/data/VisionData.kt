// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionData.kt

package com.hereliesaz.cuedetat.data

import android.graphics.PointF
import android.graphics.Rect

/**
 * Data class to hold the results of computer vision processing.
 */
data class VisionData(
    val tableCorners: List<PointF> = emptyList(),
    val genericBalls: List<PointF> = emptyList(), // Results from the generic ML Kit model
    val customBalls: List<PointF> = emptyList(),  // Results from your custom TFLite model
    val detectedHsvColor: FloatArray? = null,
    val detectedBoundingBoxes: List<Rect> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisionData

        if (tableCorners != other.tableCorners) return false
        if (genericBalls != other.genericBalls) return false
        if (customBalls != other.customBalls) return false
        if (detectedHsvColor != null) {
            if (other.detectedHsvColor == null) return false
            if (!detectedHsvColor.contentEquals(other.detectedHsvColor)) return false
        } else if (other.detectedHsvColor != null) return false
        if (detectedBoundingBoxes != other.detectedBoundingBoxes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tableCorners.hashCode()
        result = 31 * result + genericBalls.hashCode()
        result = 31 * result + customBalls.hashCode()
        result = 31 * result + (detectedHsvColor?.contentHashCode() ?: 0)
        result = 31 * result + detectedBoundingBoxes.hashCode()
        return result
    }
}