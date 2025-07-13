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

        val refinedGeneric = refineDetections(genericObjects, rotatedRgbaMat, uiState)
        val refinedCustom = refineDetections(customObjects, rotatedRgbaMat, uiState)

        val allDetectedObjects = genericObjects + customObjects
        val boundingBoxes = allDetectedObjects.map { it.boundingBox }


        val cannyEdges = Mat()
        Imgproc.Canny(rotatedRgbaMat, cannyEdges, uiState.cannyThreshold1.toDouble(), uiState.cannyThreshold2.toDouble())
        val lines = Mat()
        Imgproc.HoughLinesP(cannyEdges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)
        val tableCorners = findTableCorners(lines, rotatedRgbaMat.size())

        _visionDataFlow.value = VisionData(
            tableCorners = tableCorners,
            genericBalls = refinedGeneric,
            customBalls = refinedCustom,
            detectedBoundingBoxes = boundingBoxes
        )

        yuvMat.release()
        rgbaMat.release()
        rotatedRgbaMat.release()
        cannyEdges.release()
        lines.release()
        imageProxy.close()
    }

    private fun refineDetections(detectedObjects: List<DetectedObject>, imageMat: Mat, uiState: OverlayState): List<PointF> {
        return when (uiState.cvRefinementMethod) {
            CvRefinementMethod.HOUGH -> refineWithHoughCircles(detectedObjects, imageMat, uiState)
            CvRefinementMethod.CONTOUR -> refineWithContours(detectedObjects, imageMat)
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

    private fun findTableCorners(lines: Mat, imageSize: Size): List<PointF> {
        if (lines.empty()) return emptyList()
        var top: DoubleArray? = null
        var bottom: DoubleArray? = null
        var left: DoubleArray? = null
        var right: DoubleArray? = null
        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val x1 = line[0]; val y1 = line[1]; val x2 = line[2]; val y2 = line[3]
            val angle = atan2(y2 - y1, x2 - x1) * 180 / Math.PI
            if (kotlin.math.abs(angle) < 20 || kotlin.math.abs(angle - 180) < 20) {
                if (top == null || y1 < top[1]) top = line
                if (bottom == null || y1 > bottom[1]) bottom = line
            } else if (kotlin.math.abs(angle - 90) < 20 || kotlin.math.abs(angle + 90) < 20) {
                if (left == null || x1 < left[0]) left = line
                if (right == null || x1 > right[0]) right = line
            }
        }
        if (top == null || bottom == null || left == null || right == null) return emptyList()
        val corners = mutableListOf<PointF>()
        corners.add(lineIntersection(left, top) ?: PointF(0f, 0f))
        corners.add(lineIntersection(right, top) ?: PointF(0f, 0f))
        corners.add(lineIntersection(right, bottom) ?: PointF(0f, 0f))
        corners.add(lineIntersection(left, bottom) ?: PointF(0f, 0f))
        return corners
    }

    private fun lineIntersection(line1: DoubleArray, line2: DoubleArray): PointF? {
        val x1 = line1[0]; val y1 = line1[1]; val x2 = line1[2]; val y2 = line1[3]
        val x3 = line2[0]; val y3 = line2[1]; val x4 = line2[2]; val y4 = line2[3]
        val den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (den == 0.0) return null
        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
        val intersectX = x1 + t * (x2 - x1)
        val intersectY = y1 + t * (y2 - y1)
        return PointF(intersectX.toFloat(), intersectY.toFloat())
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