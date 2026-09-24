package com.hereliesaz.cuedetat.domain

import kotlin.math.max

/**
 * Where a camera-image pixel lands on screen, for the CameraX preview.
 *
 * The analysis image arrives in sensor orientation (landscape on a portrait phone) and the
 * preview shows it rotated upright and centre-cropped to fill the view
 * (`PreviewView.ScaleType.FILL_CENTER`). Mapping a detection with plain per-axis scaling, as the
 * pipeline used to, ignores both the rotation and the crop, so every detection landed in the
 * wrong place.
 *
 * Returns a row-major 3x3 affine matrix for `android.graphics.Matrix.setValues`. Pure Kotlin so
 * it runs in plain JUnit (Android's Matrix is a stub there).
 */
object CameraViewMapping {

    /**
     * @param imageW raw (sensor-orientation) image width
     * @param imageH raw image height
     * @param rotationDegrees clockwise rotation that makes the image upright (0, 90, 180, 270),
     *   as CameraX reports in `ImageInfo.rotationDegrees`
     * @param viewW view width in pixels
     * @param viewH view height in pixels
     */
    fun fillCenter(imageW: Int, imageH: Int, rotationDegrees: Int, viewW: Int, viewH: Int): FloatArray {
        val w = imageW.toFloat()
        val h = imageH.toFloat()
        // Raw -> upright (clockwise rotation, then shifted back into positive coordinates):
        //   90:  x' = h - y, y' = x        180: x' = w - x, y' = h - y        270: x' = y, y' = w - x
        val (a, b, c, d, e, f) = when (((rotationDegrees % 360) + 360) % 360) {
            90 -> Six(0f, -1f, h, 1f, 0f, 0f)
            180 -> Six(-1f, 0f, w, 0f, -1f, h)
            270 -> Six(0f, 1f, 0f, -1f, 0f, w)
            else -> Six(1f, 0f, 0f, 0f, 1f, 0f)
        }
        val sideways = rotationDegrees % 180 != 0
        val uprightW = if (sideways) h else w
        val uprightH = if (sideways) w else h

        // Upright -> view: uniform scale to cover, centred (the excess is cropped).
        val s = max(viewW / uprightW, viewH / uprightH)
        val tx = (viewW - uprightW * s) / 2f
        val ty = (viewH - uprightH * s) / 2f

        return floatArrayOf(
            a * s, b * s, c * s + tx,
            d * s, e * s, f * s + ty,
            0f, 0f, 1f,
        )
    }

    /** Applies a matrix from [fillCenter] to a point. */
    fun map(m: FloatArray, x: Float, y: Float): Pair<Float, Float> =
        Pair(m[0] * x + m[1] * y + m[2], m[3] * x + m[4] * y + m[5])

    private data class Six(val a: Float, val b: Float, val c: Float, val d: Float, val e: Float, val f: Float)
}
