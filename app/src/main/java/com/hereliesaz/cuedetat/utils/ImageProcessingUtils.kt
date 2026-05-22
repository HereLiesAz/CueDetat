package com.hereliesaz.cuedetat.utils

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Robust conversion of [ImageProxy] (YUV_420_888) to OpenCV [Mat] (BGR).
 *
 * This implementation avoids direct ByteBuffer wrapping of CameraX planes,
 * which can cause native SEGVs due to alignment and padding issues in OpenCV HAL.
 * Instead, it copies data row-by-row into a contiguous buffer before conversion.
 */
@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toMat(dst: Mat = Mat()): Mat {
    val image = this.image ?: return dst
    val width = image.width
    val height = image.height

    val planes = image.planes
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

    // 1. Pre-allocate a contiguous YUV Mat [Height * 1.5, Width]
    val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
    
    // 2. Copy Y plane row-by-row
    val yRowData = ByteArray(width)
    for (row in 0 until height) {
        yBuffer.position(row * yStride)
        yBuffer.get(yRowData, 0, width)
        yuvMat.put(row, 0, yRowData)
    }

    // 3. Handle UV planes based on layout
    if (uvPixelStride == 2) {
        // Interleaved layout (NV21/NV12)
        val uvRowData = ByteArray(width)
        // Use the buffer that starts at the first interleaved byte
        // For NV21, vBuffer starts at 0, uBuffer starts at 1.
        val mainUvBuffer = if (vBuffer.remaining() >= uBuffer.remaining()) vBuffer else uBuffer
        val isNv21 = vBuffer.remaining() >= uBuffer.remaining()

        for (row in 0 until height / 2) {
            mainUvBuffer.position(row * uStride)
            // Cap width to remaining buffer if necessary
            val bytesToRead = minOf(width, mainUvBuffer.remaining())
            mainUvBuffer.get(uvRowData, 0, bytesToRead)
            yuvMat.put(height + row, 0, uvRowData)
        }
        
        Imgproc.cvtColor(yuvMat, dst, if (isNv21) Imgproc.COLOR_YUV2BGR_NV21 else Imgproc.COLOR_YUV2BGR_NV12)
    } else {
        // Planar layout (I420)
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
