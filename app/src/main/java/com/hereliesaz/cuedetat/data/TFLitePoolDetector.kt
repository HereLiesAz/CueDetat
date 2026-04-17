package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class PoolDetection(
    val rect: RectF,
    val confidence: Float,
    val classId: Int
)

/**
 * TFLite detector for pool_detector_pivot_fp16.tflite
 *
 * Model: pool_detector_pivot_fp16.tflite (YOLOv8, FP16, NMS built-in)
 * Input:  [1, 640, 640, 3] float32 — RGB normalized [0, 1]
 * Output: [1, 300, 6] float32 — NMS post-processed detections
 *         Each row: [y1, x1, y2, x2, score, class_id] (normalized)
 *         Classes: 0=pool-table  1=pool-ball  2=cue
 */
class TFLitePoolDetector(private val context: Context) {

    private val interpreter: Interpreter? by lazy {
        try {
            val fd = context.assets.openFd("ml/pool_detector_pivot_fp16.tflite")
            val model = FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
            Interpreter(model, Interpreter.Options().setNumThreads(2))
        } catch (_: Exception) {
            null
        }
    }

    private val inputSize = 640
    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())
    }
    // Single-thread analysis usage makes reusing arrays safe
    private val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }
    private val intValues = IntArray(inputSize * inputSize)

    fun detect(bitmap: Bitmap): List<PoolDetection> {
        val interp = interpreter ?: return emptyList()
        return try {
            val resized = if (bitmap.width == inputSize && bitmap.height == inputSize) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
            }
            fillInputBuffer(resized)
            interp.run(inputBuffer, outputBuffer)
            parseDetections(bitmap.width, bitmap.height)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fillInputBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in 0 until intValues.size) {
            val pixelValue = intValues[i]
            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        inputBuffer.rewind()
    }

    private fun parseDetections(origWidth: Int, origHeight: Int): List<PoolDetection> {
        val results = mutableListOf<PoolDetection>()
        val dets = outputBuffer[0]
        for (det in dets) {
            val score = det[4]
            if (score < 0.25f) continue // Base threshold
            val classId = det[5].toInt()
            
            // TF NMS convention: [y1, x1, y2, x2, score, classId]
            val top = det[0] * origHeight
            val left = det[1] * origWidth
            val bottom = det[2] * origHeight
            val right = det[3] * origWidth
            
            results.add(PoolDetection(RectF(left, top, right, bottom), score, classId))
        }
        return results
    }
}
