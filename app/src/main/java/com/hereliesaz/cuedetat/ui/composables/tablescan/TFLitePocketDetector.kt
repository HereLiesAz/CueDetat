// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TFLitePocketDetector.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val MODEL_FILE = "ml/merged_pocket_detector_final_float16.tflite"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val MAX_POCKETS = 6

// Class indices from metadata.yaml: pool-table=0, pool-table-hole=1, pool-table-side=2
private const val TABLE_CLASS_ID = 0
private const val HOLE_CLASS_ID = 1
private const val SIDE_CLASS_ID = 2

/**
 * TFLite-backed implementation of [PocketDetector].
 *
 * Model: pocket_detector_fp16.tflite (YOLOv8n, FP16, NMS built-in, mAP50=0.8123)
 * Input:  [1, 640, 640, 3] float32 — RGB normalized [0, 1]
 * Output: [1, 300, 6] float32 — NMS post-processed detections
 *         Each row: [y1, x1, y2, x2, score, class_id] (TF NMS convention, normalized)
 *         Classes: 0=pool-table  1=pool-table-hole  2=pool-table-side
 */
class TFLitePocketDetector(private val context: Context) : PocketDetector {

    private val interpreter: Interpreter? by lazy {
        try {
            val fd = context.assets.openFd(MODEL_FILE)
            val model = FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
            Interpreter(model, Interpreter.Options().setNumThreads(2))
        } catch (_: Exception) {
            null
        }
    }

    // Reused per-call buffers
    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
    }
    private val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }
    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

    override fun detect(bitmap: Bitmap): MlTableDetection? {
        val interp = interpreter ?: return null
        return try {
            val resized = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            }
            fillInputBuffer(resized)
            interp.run(inputBuffer, outputBuffer)
            parseDetectionsFull(bitmap.width, bitmap.height)
        } catch (_: Exception) {
            null
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

    private fun parseDetectionsFull(width: Int, height: Int): MlTableDetection {
        val pockets = mutableListOf<PointF>()
        var tableBoundary: android.graphics.RectF? = null
        var maxTableScore = 0f

        for (det in outputBuffer[0]) {
            val score = det[4]
            if (score < CONFIDENCE_THRESHOLD) continue
            val classId = det[5].toInt()

            when (classId) {
                TABLE_CLASS_ID -> {
                    if (score > maxTableScore) {
                        maxTableScore = score
                        // YOLO normalized [y1, x1, y2, x2]
                        tableBoundary = android.graphics.RectF(
                            det[1] * width,
                            det[0] * height,
                            det[3] * width,
                            det[2] * height
                        )
                    }
                }
                HOLE_CLASS_ID, SIDE_CLASS_ID -> {
                    val cx = ((det[1] + det[3]) / 2f * width)
                    val cy = ((det[0] + det[2]) / 2f * height)
                    pockets.add(PointF(cx, cy))
                }
            }
        }

        // Scoring: 50% for table presence, 50% for pocket count (normalized to 6)
        val pocketScore = (pockets.size.toFloat() / MAX_POCKETS).coerceAtMost(1.0f)
        val finalConfidence = (maxTableScore * 0.5f) + (pocketScore * 0.5f)

        return MlTableDetection(
            tableBoundary = tableBoundary,
            pockets = pockets,
            confidence = finalConfidence
        )
    }

    // Deprecated
    private fun parseDetections(width: Int, height: Int): List<PointF>? = null
}
