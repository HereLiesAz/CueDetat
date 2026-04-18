package com.hereliesaz.cuedetat.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

/**
 * Robust conversion of [ImageProxy] (YUV_420_888) to OpenCV [Mat] (BGR).
 * This uses OpenCV's native cvtColorTwoPlane for maximum performance and accuracy.
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

    // Create Mat wrappers for the buffers
    // Y plane is always the first plane
    val yMat = Mat(height, width, CvType.CV_8UC1, yBuffer, yPlane.rowStride.toLong())

    // For UV planes, we need to check if they are interleaved (pixelStride == 2)
    // Most Android devices use NV21/NV12 where U and V are interleaved in the same buffer.
    if (uPlane.pixelStride == 2 && vPlane.pixelStride == 2) {
        // Interleaved: UV Mat has height/2 and width/2, but 2 channels (CV_8UC2)
        // We use the buffer that starts earliest (usually V for NV21, U for NV12)
        val uPos = uBuffer.position()
        val vPos = vBuffer.position()
        
        if (vPos < uPos) {
            // NV21 layout: V U V U ...
            val uvMat = Mat(height / 2, width / 2, CvType.CV_8UC2, vBuffer, vPlane.rowStride.toLong())
            Imgproc.cvtColorTwoPlane(yMat, uvMat, dst, Imgproc.COLOR_YUV2BGR_NV21)
            uvMat.release()
        } else {
            // NV12 layout: U V U V ...
            val uvMat = Mat(height / 2, width / 2, CvType.CV_8UC2, uBuffer, uPlane.rowStride.toLong())
            Imgproc.cvtColorTwoPlane(yMat, uvMat, dst, Imgproc.COLOR_YUV2BGR_NV12)
            uvMat.release()
        }
    } else {
        // Separate planes (I420 or YV12)
        // Construct a single contiguous YUV Mat and use standard cvtColor
        val yuvData = ByteArray(width * height * 3 / 2)
        
        // Fast copy Y
        val yBuf = yBuffer.duplicate()
        for (row in 0 until height) {
            yBuf.position(row * yPlane.rowStride)
            yBuf.get(yuvData, row * width, width)
        }
        
        // Copy U and V
        val uBuf = uBuffer.duplicate()
        val vBuf = vBuffer.duplicate()
        val uvSize = (width / 2) * (height / 2)
        var uOffset = width * height
        var vOffset = width * height + uvSize
        
        for (row in 0 until height / 2) {
            uBuf.position(row * uPlane.rowStride)
            uBuf.get(yuvData, uOffset + row * (width / 2), width / 2)
            
            vBuf.position(row * vPlane.rowStride)
            vBuf.get(yuvData, vOffset + row * (width / 2), width / 2)
        }
        
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, yuvData)
        Imgproc.cvtColor(yuvMat, dst, Imgproc.COLOR_YUV2BGR_I420)
        yuvMat.release()
    }

    yMat.release()
    return dst
}
