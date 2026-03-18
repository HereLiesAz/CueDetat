package com.hereliesaz.cuedetat.utils

import android.media.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Converts an [android.media.Image] in YUV_420_888 format to an OpenCV BGR [Mat].
 *
 * Mirrors the logic of [ImageProxy.toMat] but operates directly on the raw media image
 * (used when frames come from ARCore rather than CameraX).
 *
 * @param dst Reusable output Mat. Allocated internally if not provided.
 */
fun Image.toMat(dst: Mat = Mat()): Mat {
    val width = width
    val height = height

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val yMat = Mat(height, width, CvType.CV_8UC1, yBuffer, yPlane.rowStride.toLong())

    if (vPlane.pixelStride == 2) {
        // NV21 / NV12 fast path — V and U are interleaved
        val uvBuffer = vPlane.buffer
        val uvMat = Mat(height / 2, width / 2, CvType.CV_8UC2, uvBuffer, vPlane.rowStride.toLong())
        Imgproc.cvtColorTwoPlane(yMat, uvMat, dst, Imgproc.COLOR_YUV2BGR_NV21)
        uvMat.release()
    } else {
        // I420 planar fallback
        val ySize = yBuffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val data = ByteArray(ySize + uSize + vSize)
        yBuffer.get(data, 0, ySize)
        uPlane.buffer.get(data, ySize, uSize)
        vPlane.buffer.get(data, ySize + uSize, vSize)
        val i420Mat = Mat(height + height / 2, width, CvType.CV_8UC1)
        i420Mat.put(0, 0, data)
        Imgproc.cvtColor(i420Mat, dst, Imgproc.COLOR_YUV2BGR_I420)
        i420Mat.release()
    }

    yMat.release()
    return dst
}
