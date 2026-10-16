package com.hereliesaz.cuedetat.utils

import android.media.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Robust conversion of [android.media.Image] (YUV_420_888) to OpenCV [Mat] (BGR).
 *
 * Mirrors the safe logic in [ImageProxy.toMat] to prevent native crashes
 * on devices with unusual memory alignments. Reads through `duplicate()`d buffers, so the
 * image's own plane buffers keep their positions. `cropRect` is not applied: the full
 * `width` x `height` frame is converted.
 *
 * - Chroma pixel stride 2 (semi-planar): the interleaved chroma rows are copied as NV21 or
 *   NV12, chosen by which plane buffer spans the interleaved block.
 * - Any other chroma pixel stride (planar): packed into I420 — the full Y plane, then the
 *   whole U plane (`w/2 * h/2` bytes, contiguous), then the whole V plane — honouring each
 *   plane's row and pixel stride. I420 in a `(h * 3/2) x w` Mat is that contiguous byte
 *   order, *not* U and V side by side in each chroma row.
 */
fun Image.toMat(dst: Mat = Mat()): Mat {
    val width = width
    val height = height

    val planes = planes
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()

    val yStride = yPlane.rowStride
    val uStride = uPlane.rowStride
    val vStride = vPlane.rowStride
    val uvPixelStride = uPlane.pixelStride
    val vPixelStride = vPlane.pixelStride

    val chromaW = width / 2
    val chromaH = height / 2
    val ySize = width * height
    val out = ByteArray(ySize + 2 * chromaW * chromaH)

    for (row in 0 until height) {
        yBuffer.position(row * yStride)
        yBuffer.get(out, row * width, width)
    }

    val yuvMat = Mat(height + chromaH, width, CvType.CV_8UC1)
    try {
        if (uvPixelStride == 2) {
            val mainUvBuffer = if (vBuffer.remaining() >= uBuffer.remaining()) vBuffer else uBuffer
            val isNv21 = vBuffer.remaining() >= uBuffer.remaining()
            for (row in 0 until chromaH) {
                mainUvBuffer.position(row * uStride)
                val bytesToRead = minOf(width, mainUvBuffer.remaining())
                mainUvBuffer.get(out, ySize + row * width, bytesToRead)
            }
            yuvMat.put(0, 0, out)
            Imgproc.cvtColor(yuvMat, dst, if (isNv21) Imgproc.COLOR_YUV2BGR_NV21 else Imgproc.COLOR_YUV2BGR_NV12)
        } else {
            val uBase = ySize
            val vBase = ySize + chromaW * chromaH
            for (row in 0 until chromaH) {
                for (col in 0 until chromaW) {
                    out[uBase + row * chromaW + col] = uBuffer.get(row * uStride + col * uvPixelStride)
                    out[vBase + row * chromaW + col] = vBuffer.get(row * vStride + col * vPixelStride)
                }
            }
            yuvMat.put(0, 0, out)
            Imgproc.cvtColor(yuvMat, dst, Imgproc.COLOR_YUV2BGR_I420)
        }
    } finally {
        yuvMat.release()
    }
    return dst
}
