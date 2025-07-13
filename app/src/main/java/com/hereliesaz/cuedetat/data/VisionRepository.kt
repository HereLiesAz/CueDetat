// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/VisionRepository.kt

package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.hereliesaz.cuedetat.di.GenericDetector
import com.hereliesaz.cuedetat.view.state.CvRefinementMethod
import com.hereliesaz.cuedetat.view.state.OverlayState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2

@Singleton
class VisionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @GenericDetector private val genericDetector: ObjectDetector,
) {

    private val _visionDataFlow = MutableStateFlow(VisionData())
    val visionDataFlow = _visionDataFlow.asStateFlow()
    private val customDetector: ObjectDetector?

    init {
        val modelFile = "model.tflite"
        val assetExists = try {
            context.assets.open(modelFile).close()
            true
        } catch (e: IOException) {
            false
        }

        customDetector = if (assetExists) {
            val localModel = LocalModel.Builder().setAssetFilePath(modelFile).build()
            val customOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()
            ObjectDetection.getClient(customOptions)
        } else {
            null
        }
    }


    @SuppressLint("UnsafeOptInUsageError")
    fun processImage(imageProxy: ImageProxy, uiState: OverlayState) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        var genericResults: List<DetectedObject> = emptyList()
        var customResults: List<DetectedObject> = emptyList()
        var processedCount = 0

        val detectorsToRun = listOfNotNull(genericDetector, if (uiState.useCustomModel) customDetector else null)
        if (detectorsToRun.isEmpty()) {
            imageProxy.close()
            return
        }

        val onComplete = {
            processedCount++
            if (processedCount == detectorsToRun.size) {
                processWithOpenCV(imageProxy, genericResults, customResults, uiState)
            }
        }

        genericDetector.process(inputImage)
            .addOnSuccessListener { genericResults = it }
            .addOnFailureListener { Log.e("VisionRepo", "Generic detector failed.", it) }
            .addOnCompleteListener { onComplete() }

        if (uiState.useCustomModel && customDetector != null) {
            customDetector.process(inputImage)
                .addOnSuccessListener { customResults = it }
                .addOnFailureListener { Log.e("VisionRepo", "Custom detector failed.", it) }
                .addOnCompleteListener { onComplete() }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processWithOpenCV(imageProxy: ImageProxy, genericObjects: List<DetectedObject>, customObjects: List<DetectedObject>, uiState: OverlayState) {
        val yBuffer: ByteBuffer = imageProxy.planes[0].buffer
        val uBuffer: ByteBuffer = imageProxy.planes[1].buffer
        val vBuffer: ByteBuffer = imageProxy.planes[2].buffer
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvMat = Mat(imageProxy.height + imageProxy.height / 2, imageProxy.width, org.opencv.core.CvType.CV_8UC1)
        yuvMat.put(0, 0, nv21)
        val rgbaMat = Mat()
        Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21, 4)
        val rotatedRgbaMat = Mat()
        Core.rotate(rgbaMat, rotatedRgbaMat, imageProxy.imageInfo.rotationDegrees.toCvRotateCode())

        val tableBoundaries = if (uiState.showTable) {
            val referenceRadius = uiState.onPlaneBall?.radius ?: uiState.protractorUnit.radius
            val tableToBallRatioLong = uiState.tableSize.getTableToBallRatioLong()
            val tableToBallRatioShort = tableToBallRatioLong / uiState.tableSize.aspectRatio
            val tablePlayingSurfaceWidth = tableToBallRatioLong * referenceRadius
            val tablePlayingSurfaceHeight = tableToBallRatioShort * referenceRadius
            val canvasCenterX = uiState.viewWidth / 2f
            val canvasCenterY = uiState.viewHeight / 2f
            Rect(
                (canvasCenterX - tablePlayingSurfaceWidth / 2).toInt(),
                (canvasCenterY - tablePlayingSurfaceHeight / 2).toInt(),
                (canvasCenterX + tablePlayingSurfaceWidth / 2).toInt(),
                (canvasCenterY + tablePlayingSurfaceHeight / 2).toInt()
            )
        } else {
            null
        }

        val refinedGeneric = refineDetections(genericObjects, rotatedRgbaMat, uiState, tableBoundaries)
        val refinedCustom = refineDetections(customObjects, rotatedRgbaMat, uiState, tableBoundaries)

        val allDetectedObjects = genericObjects + customObjects
        val boundingBoxes = allDetectedObjects.map { it.boundingBox }

        _visionDataFlow.value = VisionData(
            genericBalls = refinedGeneric,
            customBalls = refinedCustom,
            detectedBoundingBoxes = boundingBoxes
        )

        yuvMat.release()
        rgbaMat.release()
        rotatedRgbaMat.release()
        imageProxy.close()
    }

    private fun refineDetections(detectedObjects: List<DetectedObject>, imageMat: Mat, uiState: OverlayState, tableBounds: Rect?): List<PointF> {
        val filteredObjects = if (tableBounds != null) {
            detectedObjects.filter { tableBounds.contains(it.boundingBox) }
        } else {
            detectedObjects
        }

        return when (uiState.cvRefinementMethod) {
            CvRefinementMethod.HOUGH -> refineWithHoughCircles(filteredObjects, imageMat, uiState)
            CvRefinementMethod.CONTOUR -> refineWithContours(filteredObjects, imageMat)
        }
    }

    private fun refineWithHoughCircles(detectedObjects: List<DetectedObject>, imageMat: Mat, uiState: OverlayState): List<PointF> {
        val refinedPoints = mutableListOf<PointF>()
        val grayMat = Mat()
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        for (obj in detectedObjects) {
            val roiRect = obj.boundingBox.toOpenCvRect()
            if (roiRect.x >= 0 && roiRect.y >= 0 && roiRect.x + roiRect.width <= grayMat.cols() && roiRect.y + roiRect.height <= grayMat.rows() && roiRect.width > 0 && roiRect.height > 0) {
                val roi = Mat(grayMat, roiRect)
                val circles = Mat()
                Imgproc.HoughCircles(
                    roi, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                    roiRect.width.toDouble(),
                    uiState.houghP1.toDouble(),
                    uiState.houghP2.toDouble(),
                    5,
                    roiRect.width.coerceAtLeast(6)
                )
                if (!circles.empty()) {
                    val circle = circles.get(0, 0)
                    refinedPoints.add(PointF((circle[0] + roiRect.x).toFloat(), (circle[1] + roiRect.y).toFloat()))
                }
                roi.release()
                circles.release()
            }
        }
        grayMat.release()
        return refinedPoints
    }

    private fun refineWithContours(detectedObjects: List<DetectedObject>, imageMat: Mat): List<PointF> {
        val refinedPoints = mutableListOf<PointF>()
        val grayMat = Mat()
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        for (obj in detectedObjects) {
            val roiRect = obj.boundingBox.toOpenCvRect()
            if (roiRect.x >= 0 && roiRect.y >= 0 && roiRect.x + roiRect.width <= grayMat.cols() && roiRect.y + roiRect.height <= grayMat.rows() && roiRect.width > 0 && roiRect.height > 0) {
                val roi = Mat(grayMat, roiRect)
                val binaryRoi = Mat()

                Imgproc.threshold(roi, binaryRoi, 100.0, 255.0, Imgproc.THRESH_BINARY)

                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()

                Imgproc.findContours(binaryRoi, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                contours.maxByOrNull { Imgproc.contourArea(it) }?.let { bestContour ->
                    val contour2f = MatOfPoint2f(*bestContour.toArray())
                    val center = Point()
                    val radius = FloatArray(1)
                    Imgproc.minEnclosingCircle(contour2f, center, radius)

                    refinedPoints.add(PointF((center.x + roiRect.x).toFloat(), (center.y + roiRect.y).toFloat()))
                }

                roi.release()
                binaryRoi.release()
                hierarchy.release()
            }
        }
        grayMat.release()
        return refinedPoints
    }

    private fun Int.toCvRotateCode(): Int = when (this) {
        90 -> Core.ROTATE_90_CLOCKWISE
        180 -> Core.ROTATE_180
        270 -> Core.ROTATE_90_COUNTERCLOCKWISE
        else -> -1
    }

    private fun android.graphics.Rect.toOpenCvRect(): org.opencv.core.Rect {
        return org.opencv.core.Rect(this.left, this.top, this.width(), this.height())
    }
}