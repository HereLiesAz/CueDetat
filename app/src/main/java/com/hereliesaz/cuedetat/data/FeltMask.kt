// app/src/main/java/com/hereliesaz/cuedetat/data/FeltMask.kt
package com.hereliesaz.cuedetat.data

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import kotlin.math.max
import kotlin.math.min

/**
 * Shared felt-color HSV masking, factored out of [CvBallDetector] so
 * [CvCueDetector] can use the exact same felt prior instead of re-deriving
 * its own inRange bounds. Both detectors treat the sampled felt HSV
 * mean/stdDev as ground truth for "this pixel is the table surface";
 * everything that falls outside that range is a candidate object (ball,
 * cue, hand, rail, etc).
 */
internal object FeltMask {

    private const val DEFAULT_SD_SCALE = 2.5f

    /**
     * Writes a binary mask (255 = felt-colored) into [out]. [out] is resized/
     * retyped by [Core.inRange] as needed, matching the caller's reusable-Mat
     * pattern (no allocation if [out] already has the right size/type).
     */
    fun build(
        hsvMat: Mat,
        feltHsv: FloatArray,
        feltStdDev: FloatArray,
        out: Mat,
        sdScale: Float = DEFAULT_SD_SCALE,
    ) {
        val lower = Scalar(
            max(0.0, feltHsv[0] - sdScale * feltStdDev[0] - 5.0),
            max(40.0, (feltHsv[1] - sdScale * feltStdDev[1]).toDouble()),
            max(40.0, (feltHsv[2] - sdScale * feltStdDev[2]).toDouble()),
        )
        val upper = Scalar(
            min(180.0, feltHsv[0] + sdScale * feltStdDev[0] + 5.0),
            min(255.0, (feltHsv[1] + sdScale * feltStdDev[1]).toDouble()),
            min(255.0, (feltHsv[2] + sdScale * feltStdDev[2]).toDouble()),
        )
        Core.inRange(hsvMat, lower, upper, out)
    }
}
