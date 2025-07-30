package com.hereliesaz.cuedetat.utils

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toMat(): Mat {
    val image = this.image ?: return Mat()
    val planes = image.planes
    val width = image.width
    val height = image.height

    val bgrMat = Mat()

    val yPlane = planes[0]
    val yBuffer = yPlane.buffer
    val yMat = Mat(height, width, CvType.CV_8UC1, yBuffer, yPlane.rowStride.toLong())

    val uPlane = planes[1]
    val vPlane = planes[2]

    if (vPlane.pixelStride == 2) {
        val uvBuffer = vPlane.buffer
        val uvMat =
            Mat(height / 2, width / 2, CvType.CV_8UC2, uvBuffer, vPlane.rowStride.toLong())
        Imgproc.cvtColorTwoPlane(yMat, uvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21)
        uvMat.release()
    } else {
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