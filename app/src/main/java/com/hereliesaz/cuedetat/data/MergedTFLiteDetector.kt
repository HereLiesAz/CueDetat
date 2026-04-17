package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.hereliesaz.cuedetat.ui.composables.tablescan.PocketDetector
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val POCKET_MODEL = "ml/pocket_detector_fp16.tflite"
private const val POOL_MODEL = "ml/pool_detector_pivot_fp16.tflite"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val MAX_DETECTIONS = 300
private const val HOLE_CLASS_ID = 1

/**
 * A combined TFLite detector that runs two models (pockets and balls/cues)
 * on a single input pass. This avoids redundant resizing and normalization.
 */
class MergedTFLiteDetector(private val context: Context) : PocketDetector {

    private val pocketInterp: Interpreter? by lazy { loadModel(POCKET_MODEL) }
    private val poolInterp: Interpreter? by lazy { loadModel(POOL_MODEL) }

    // Reused input buffer shared by both models
    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
    }

    // Separate output buffers
    private val pocketOutput = Array(1) { Array(MAX_DETECTIONS) { FloatArray(6) } }
    private val poolOutput = Array(1) { Array(MAX_DETECTIONS) { FloatArray(6) } }
    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

    private fun loadModel(path: String): Interpreter? {
        return try {
            val fd = context.assets.openFd(path)
            val model = FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
            Interpreter(model, Interpreter.Options().setNumThreads(2))
        } catch (e: Exception) {
            Log.e("MergedTFLiteDetector", "Failed to load $path: ${e.message}")
            null
        }
    }

    /**
     * Common preprocessing: resize and fill the shared input buffer.
     */
    private fun preprocess(bitmap: Bitmap) {
        val resized = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        }
        
        inputBuffer.rewind()
        resized.getPixels(intValues, 0, resized.width, 0, 0, resized.width, resized.height)
        for (i in intValues.indices) {
            val pixelValue = intValues[i]
            inputBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }
        inputBuffer.rewind()
    }

    /**
     * Implements [PocketDetector] interface for table scanning.
     */
    override fun detect(bitmap: Bitmap): List<PointF>? {
        val interp = pocketInterp ?: return null
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, pocketOutput)
            parsePocketDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e("MergedTFLiteDetector", "Pocket inference failed: ${e.message}")
            null
        }
    }

    /**
     * Detects balls and cues for live AR tracking.
     */
    fun detectPool(bitmap: Bitmap): List<PoolDetection> {
        val interp = poolInterp ?: return emptyList()
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, poolOutput)
            parsePoolDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e("MergedTFLiteDetector", "Pool inference failed: ${e.message}")
            emptyList()
        }
    }

    private fun parsePocketDetections(width: Int, height: Int): List<PointF>? {
        val results = mutableListOf<PointF>()
        for (det in pocketOutput[0]) {
            if (det[4] < CONFIDENCE_THRESHOLD) continue
            if (det[5].toInt() != HOLE_CLASS_ID) continue
            
            // TF NMS convention: [y1, x1, y2, x2, score, class_id]
            val cx = ((det[1] + det[3]) / 2f * width)
            val cy = ((det[0] + det[2]) / 2f * height)
            results.add(PointF(cx, cy))
            if (results.size >= 6) break
        }
        return results.ifEmpty { null }
    }

    private fun parsePoolDetections(width: Int, height: Int): List<PoolDetection> {
        val results = mutableListOf<PoolDetection>()
        for (det in poolOutput[0]) {
            val score = det[4]
            if (score < 0.25f) continue
            val classId = det[5].toInt()
            
            val top = det[0] * height
            val left = det[1] * width
            val bottom = det[2] * height
            val right = det[3] * width
            
            results.add(PoolDetection(RectF(left, top, right, bottom), score, classId))
        }
        return results
    }
}
