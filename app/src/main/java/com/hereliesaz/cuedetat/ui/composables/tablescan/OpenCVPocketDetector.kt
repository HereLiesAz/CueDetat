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
private const val MAX_POCKETS = 6
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

    override fun detect(bitmap: Bitmap): List<PointF>? {
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
            
            val detections = parseDetections(outputs[0], bitmap.width, bitmap.height)
            
            rgbMat.release()
            blob.release()
            outputs.forEach { it.release() }
            
            detections
        } catch (e: Exception) {
            Log.e("OpenCVPocketDetector", "Inference failed: ${e.message}")
            null
        }
    }

    private fun parseDetections(output: Mat, width: Int, height: Int): List<PointF>? {
        // Output format is [1, 300, 6] -> Each row: [y1, x1, y2, x2, score, class_id]
        val results = mutableListOf<PointF>()
        
        val rows = output.size(1) // 300
        val data = FloatArray(6)
        
        for (i in 0 until rows) {
            output.get(intArrayOf(0, i, 0), data)
            val score = data[4]
            if (score < CONFIDENCE_THRESHOLD) continue
            
            val classId = data[5].toInt()
            if (classId != HOLE_CLASS_ID) continue
            
            // TF NMS convention in the ONNX export too: [y1, x1, y2, x2, ...]
            val y1 = data[0]
            val x1 = data[1]
            val y2 = data[2]
            val x2 = data[3]
            
            val cx = ((x1 + x2) / 2f * width)
            val cy = ((y1 + y2) / 2f * height)
            results.add(PointF(cx, cy))
            
            if (results.size >= MAX_POCKETS) break
        }
        
        return if (results.isNotEmpty()) results else null
    }

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
