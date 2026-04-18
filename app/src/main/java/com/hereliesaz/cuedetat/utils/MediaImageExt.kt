package com.hereliesaz.cuedetat.utils

import android.media.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Robust conversion of [android.media.Image] (YUV_420_888) to OpenCV [Mat] (BGR).
 *
 * Mirrors the safe logic in [ImageProxy.toMat] to prevent native crashes
 * on devices with unusual memory alignments.
 */
fun Image.toMat(dst: Mat = Mat()): Mat {
    val width = width
    val height = height

    val planes = planes
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val yStride = yPlane.rowStride
    val uStride = uPlane.rowStride
    val vStride = vPlane.rowStride
    val uvPixelStride = uPlane.pixelStride

    val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
    
    val yRowData = ByteArray(width)
    for (row in 0 until height) {
        yBuffer.position(row * yStride)
        yBuffer.get(yRowData, 0, width)
        yuvMat.put(row, 0, yRowData)
    }

    if (uvPixelStride == 2) {
        val uvRowData = ByteArray(width)
        val mainUvBuffer = if (vBuffer.remaining() >= uBuffer.remaining()) vBuffer else uBuffer
        val isNv21 = vBuffer.remaining() >= uBuffer.remaining()

        for (row in 0 until height / 2) {
            mainUvBuffer.position(row * uStride)
            val bytesToRead = minOf(width, mainUvBuffer.remaining())
            mainUvBuffer.get(uvRowData, 0, bytesToRead)
            yuvMat.put(height + row, 0, uvRowData)
        }
        
        Imgproc.cvtColor(yuvMat, dst, if (isNv21) Imgproc.COLOR_YUV2BGR_NV21 else Imgproc.COLOR_YUV2BGR_NV12)
    } else {
        val uRowData = ByteArray(width / 2)
        val vRowData = ByteArray(width / 2)
        for (row in 0 until height / 2) {
            uBuffer.position(row * uStride)
            uBuffer.get(uRowData, 0, width / 2)
            yuvMat.put(height + row, 0, uRowData)

            vBuffer.position(row * vStride)
            vBuffer.get(vRowData, 0, width / 2)
            yuvMat.put(height + row, width / 2, vRowData)
        }
        Imgproc.cvtColor(yuvMat, dst, Imgproc.COLOR_YUV2BGR_I420)
    }

    yuvMat.release()
    return dst
}
