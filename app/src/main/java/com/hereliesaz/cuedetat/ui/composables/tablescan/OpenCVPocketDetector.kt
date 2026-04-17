// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/OpenCVPocketDetector.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import java.io.File
import java.io.FileOutputStream

private const val MODEL_PATH = "weights/best.onnx"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val TABLE_CLASS_ID = 0
private const val HOLE_CLASS_ID = 1

/**
 * OpenCV-backed implementation of [PocketDetector] using `best.onnx`.
 */
class OpenCVPocketDetector(private val context: Context) : PocketDetector {

    private val net: Net? by lazy {
        try {
            val modelFile = getAssetFile(MODEL_PATH)
            Dnn.readNetFromONNX(modelFile.absolutePath)
        } catch (e: Exception) {
            Log.e("OpenCVPocketDetector", "Failed to load ONNX model: ${e.message}")
            null
        }
    }

    override fun detect(bitmap: Bitmap): MlTableDetection? {
        val n = net ?: return null
        return try {
            val rgbMat = Mat()
            Utils.bitmapToMat(bitmap, rgbMat)
            
            // YOLOv8 expects 640x640 RGB
            val blob = Dnn.blobFromImage(
                rgbMat,
                1.0 / 255.0,
                Size(INPUT_SIZE.toDouble(), INPUT_SIZE.toDouble()),
                null,
                true, // swapRB = true for RGB (since Utils.bitmapToMat usually gives RGBA)
                false
            )

            n.setInput(blob)
            val outputs = mutableListOf<Mat>()
            n.forward(outputs, n.unconnectedOutLayersNames)
            
            val detection = parseDetectionsFull(outputs[0], bitmap.width, bitmap.height)
            
            rgbMat.release()
            blob.release()
            outputs.forEach { it.release() }
            
            detection
        } catch (e: Exception) {
            Log.e("OpenCVPocketDetector", "Inference failed: ${e.message}")
            null
        }
    }

    private fun parseDetectionsFull(output: Mat, width: Int, height: Int): MlTableDetection {
        val pockets = mutableListOf<PointF>()
        var tableBoundary: android.graphics.RectF? = null
        var maxTableScore = 0f
        
        val rows = output.size(1) // 300
        val data = FloatArray(6)
        
        for (i in 0 until rows) {
            output.get(intArrayOf(0, i, 0), data)
            val score = data[4]
            if (score < CONFIDENCE_THRESHOLD) continue
            
            val classId = data[5].toInt()
            when (classId) {
                TABLE_CLASS_ID -> {
                    if (score > maxTableScore) {
                        maxTableScore = score
                        tableBoundary = android.graphics.RectF(
                            data[1] * width,
                            data[0] * height,
                            data[3] * width,
                            data[2] * height
                        )
                    }
                }
                HOLE_CLASS_ID -> {
                    val cx = ((data[1] + data[3]) / 2f * width)
                    val cy = ((data[0] + data[2]) / 2f * height)
                    pockets.add(PointF(cx, cy))
                }
            }
        }
        
        val pocketScore = (pockets.size.toFloat() / 6.0f).coerceAtMost(1.0f)
        val finalConfidence = (maxTableScore * 0.5f) + (pocketScore * 0.5f)

        return MlTableDetection(
            tableBoundary = tableBoundary,
            pockets = pockets,
            confidence = finalConfidence
        )
    }

    private fun parseDetections(output: Mat, width: Int, height: Int): List<PointF>? = null

    private fun getAssetFile(assetPath: String): File {
        val file = File(context.cacheDir, assetPath.substringAfterLast("/"))
        if (!file.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }
}
