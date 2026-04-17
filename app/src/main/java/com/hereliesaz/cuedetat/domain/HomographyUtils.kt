// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/HomographyUtils.kt

package com.hereliesaz.cuedetat.domain

import androidx.compose.ui.geometry.Offset
import org.opencv.core.Mat
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Decomposes a homography matrix into translation, rotation, and scale
 * relative to the centre of the image.
 *
 * Note: This is a simplification; full pose estimation requires PnP, but for 2D alignment
 * this approximation suffices for initial setup.
 *
 * Returns Triple(translation: Offset, rotationDegrees: Float, scale: Float)
 * where scale is the reciprocal of the detected zoom factor.
 */
fun decomposeHomography(h: Mat, imgWidth: Float, imgHeight: Float): Triple<Offset, Float, Float> {
    val h0 = h[0, 0][0].toFloat()
    val h1 = h[0, 1][0].toFloat()
    val h2 = h[0, 2][0].toFloat()
    val h3 = h[1, 0][0].toFloat()
    val h4 = h[1, 1][0].toFloat()
    val h5 = h[1, 2][0].toFloat()

    // Estimate scale from basis vectors.
    val scaleX = sqrt(h0 * h0 + h3 * h3)
    val scaleY = sqrt(h1 * h1 + h4 * h4)
    val scale = (scaleX + scaleY) / 2.0f

    // Estimate rotation from the first column vector.
    val rotation = -atan2(h3, h0) * (180f / PI.toFloat())

    // Calculate translation relative to center.
    val canvasCenter = Offset(imgWidth / 2f, imgHeight / 2f)
    val translation = Offset(h2, h5) - canvasCenter

    return Triple(translation, rotation, 1 / scale)
}
