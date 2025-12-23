package com.hereliesaz.cuedetat.utils

import android.media.Image
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

fun Image.toMat(dst: Mat = Mat()): Mat {
    val planes = this.planes
    val width = this.width
    val height = this.height

    val bgrMat = dst

    val yPlane = planes[0]
    val yBuffer = yPlane.buffer
    // ARCore uses YUV_420_888 but often with a specific layout.
    // However, the standard way to extract NV21/I420 from android.media.Image is similar to ImageProxy.

    // Create a temporary Mat for Y plane
    val yMat = Mat(height, width, CvType.CV_8UC1, yBuffer, yPlane.rowStride.toLong())

    val uPlane = planes[1]
    val vPlane = planes[2]

    // Check if we can use optimized conversion
    if (vPlane.pixelStride == 2) { // likely NV21 like structure
        val uvBuffer = vPlane.buffer
        val uvMat =
            Mat(height / 2, width / 2, CvType.CV_8UC2, uvBuffer, vPlane.rowStride.toLong())
        Imgproc.cvtColorTwoPlane(yMat, uvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21)
        uvMat.release()
    } else {
        // Fallback for generic I420
        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val data = ByteArray(ySize + uSize + vSize)

        yPlane.buffer.get(data, 0, ySize)
        uPlane.buffer.get(data, ySize, uSize)
        vPlane.buffer.get(data, ySize + uSize, vSize)

        val yuvI420Mat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvI420Mat.put(0, 0, data)
        Imgproc.cvtColor(yuvI420Mat, bgrMat, Imgproc.COLOR_YUV2BGR_I420)
        yuvI420Mat.release()
    }
    yMat.release()
    return bgrMat
}
