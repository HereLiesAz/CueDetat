// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt

package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetector
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.view.model.Perspective
import com.hereliesaz.cuedetat.view.state.OverlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisionRepository @Inject constructor(
    @GenericDetector private val genericObjectDetector: ObjectDetector,
) {

    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()

    private var lastFrameTime = 0L

    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, state: OverlayState) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < 33) { // ~30 FPS cap
            imageProxy.close()
            return
        }
        lastFrameTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val matrix = getTransformationMatrix(
                inputImage.width, inputImage.height,
                state.viewWidth, state.viewHeight
            )

            genericObjectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    val hsv = if (state.lockedHsvColor != null) {
                        state.lockedHsvColor
                    } else {
                        val mat = imageToMat(inputImage)
                        val hsvMat = Mat()
                        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)
                        val centerColor = hsvMat[hsvMat.rows() / 2, hsvMat.cols() / 2]
                        floatArrayOf(centerColor[0].toFloat(), centerColor[1].toFloat(), centerColor[2].toFloat())
                    }

                    val detectedScreenPoints = detectedObjects.map {
                        val transformedRect = RectF(it.boundingBox)
                        matrix.mapRect(transformedRect)
                        PointF(transformedRect.centerX(), transformedRect.centerY())
                    }

                    // Convert screen points to logical points before filtering
                    val detectedLogicalPoints = if (state.hasInverseMatrix) {
                        detectedScreenPoints.map { screenPoint ->
                            Perspective.screenToLogical(screenPoint, state.inversePitchMatrix)
                        }
                    } else {
                        emptyList()
                    }


                    val filteredBalls = if (state.table.isVisible) {
                        detectedLogicalPoints.filter { state.table.isPointInside(it) }
                    } else {
                        detectedLogicalPoints
                    }

                    _visionDataFlow.value = VisionData(
                        genericBalls = filteredBalls,
                        detectedHsvColor = hsv
                    )

                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun imageToMat(image: InputImage): Mat {
        val yBuffer = image.planes?.get(0)?.buffer
        val uBuffer = image.planes?.get(1)?.buffer
        val vBuffer = image.planes?.get(2)?.buffer

        val ySize = yBuffer?.remaining() ?: 0
        val uSize = uBuffer?.remaining() ?: 0
        val vSize = vBuffer?.remaining() ?: 0

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer?.get(nv21, 0, ySize)
        vBuffer?.get(nv21, ySize, vSize)
        uBuffer?.get(nv21, ySize + vSize, uSize)

        val yuvImage = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        yuvImage.put(0, 0, nv21)
        val mat = Mat()
        Imgproc.cvtColor(yuvImage, mat, Imgproc.COLOR_YUV2BGR_NV21, 3)
        return mat
    }

    private fun getTransformationMatrix(
        sourceWidth: Int, sourceHeight: Int,
        destWidth: Int, destHeight: Int
    ): Matrix {
        val matrix = Matrix()
        val sx = destWidth.toFloat() / sourceWidth.toFloat()
        val sy = destHeight.toFloat() / sourceHeight.toFloat()
        matrix.postScale(sx, sy)
        return matrix
    }
}