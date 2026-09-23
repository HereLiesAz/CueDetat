package com.hereliesaz.cuedetat.feature.expert.ar

import android.graphics.Color
import android.graphics.ImageFormat
import android.media.Image

/**
 * Mean HSV of a pixel rectangle in a YUV_420_888 camera image.
 *
 * Pure read of the image planes (BT.601 full-range YUV->RGB, then Android RGBToHSV), so no
 * OpenCV dependency. Returns Android HSV (hue 0..360, s/v 0..1), or null for an unsupported
 * format or an empty rectangle. Bounds are clamped to the image.
 */
internal fun sampleYuvHsv(image: Image, x0: Int, y0: Int, x1: Int, y1: Int, samplesPerSide: Int = 16): FloatArray? {
    if (image.format != ImageFormat.YUV_420_888) return null
    val left = x0.coerceIn(0, image.width); val right = x1.coerceIn(0, image.width)
    val top = y0.coerceIn(0, image.height); val bottom = y1.coerceIn(0, image.height)
    if (right <= left || bottom <= top) return null

    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val yBuf = yPlane.buffer
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer

    var rSum = 0.0; var gSum = 0.0; var bSum = 0.0; var count = 0
    val step = ((right - left) / samplesPerSide).coerceAtLeast(1)

    var y = top
    while (y < bottom) {
        var x = left
        while (x < right) {
            val yIdx = y * yPlane.rowStride + x * yPlane.pixelStride
            val uvX = x / 2; val uvY = y / 2
            val uIdx = uvY * uPlane.rowStride + uvX * uPlane.pixelStride
            val vIdx = uvY * vPlane.rowStride + uvX * vPlane.pixelStride
            if (yIdx < yBuf.limit() && uIdx < uBuf.limit() && vIdx < vBuf.limit()) {
                val yy = (yBuf.get(yIdx).toInt() and 0xFF).toDouble()
                val uu = (uBuf.get(uIdx).toInt() and 0xFF) - 128.0
                val vv = (vBuf.get(vIdx).toInt() and 0xFF) - 128.0
                rSum += (yy + 1.402 * vv).coerceIn(0.0, 255.0)
                gSum += (yy - 0.344136 * uu - 0.714136 * vv).coerceIn(0.0, 255.0)
                bSum += (yy + 1.772 * uu).coerceIn(0.0, 255.0)
                count++
            }
            x += step
        }
        y += step
    }
    if (count == 0) return null
    val hsv = FloatArray(3)
    Color.RGBToHSV((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt(), hsv)
    return hsv
}
