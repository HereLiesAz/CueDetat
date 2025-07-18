// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionData.kt

package com.hereliesaz.cuedetat.data

import android.graphics.PointF
import android.graphics.Rect
import org.opencv.core.Mat

/**
 * Data class to hold the results of computer vision processing.
 */
data class VisionData(
    val genericBalls: List<PointF> = emptyList(),
    val customBalls: List<PointF> = emptyList(),
    val detectedHsvColor: FloatArray? = null,
    val detectedBoundingBoxes: List<Rect> = emptyList(),
    val cvMask: Mat? = null // The binary mask for debugging
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisionData

        if (genericBalls != other.genericBalls) return false
        if (customBalls != other.customBalls) return false
        if (detectedHsvColor != null) {
            if (other.detectedHsvColor == null) return false
            if (!detectedHsvColor.contentEquals(other.detectedHsvColor)) return false
        } else if (other.detectedHsvColor != null) return false
        if (detectedBoundingBoxes != other.detectedBoundingBoxes) return false
        if (cvMask != other.cvMask) return false

        return true
    }

    override fun hashCode(): Int {
        var result = genericBalls.hashCode()
        result = 31 * result + customBalls.hashCode()
        result = 31 * result + (detectedHsvColor?.contentHashCode() ?: 0)
        result = 31 * result + detectedBoundingBoxes.hashCode()
        result = 31 * result + (cvMask?.hashCode() ?: 0)
        return result
    }
}