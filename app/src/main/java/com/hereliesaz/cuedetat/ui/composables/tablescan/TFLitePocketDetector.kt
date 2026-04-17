// app/src/main/java/com/hereliesaz/cuedetat/ui/composables/tablescan/TFLitePocketDetector.kt
package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.content.Context
import android.graphics.PointF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val MODEL_FILE = "pocket_detector_fp16.tflite"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val MAX_POCKETS = 6

// Class indices from metadata.yaml: pool-table=0, pool-table-hole=1, pool-table-side=2
private const val HOLE_CLASS_ID = 1

/**
 * TFLite-backed implementation of [PocketDetector].
 *
 * Model: pocket_detector_fp16.tflite (YOLOv8n, FP16, NMS built-in, mAP50=0.8123)
 * Input:  [1, 640, 640, 3] float32 — RGB normalized [0, 1]
 * Output: [1, 300, 6] float32 — NMS post-processed detections
 *         Each row: [y1, x1, y2, x2, score, class_id] (TF NMS convention, normalized)
 *         Classes: 0=pool-table  1=pool-table-hole  2=pool-table-side
 *
 * Falls back to Hough circles (returns null) when:
 * - Model fails to initialize
 * - Inference throws an exception
 * - No detections exceed [CONFIDENCE_THRESHOLD] for the hole class
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

    // Reused per-call buffers — detect() runs on a single CameraX analysis thread.
    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
    }
    private val outputBuffer = Array(1) { Array(300) { FloatArray(6) } }

    override fun detect(yBytes: ByteArray, width: Int, height: Int): List<PointF>? {
        val interp = interpreter ?: return null
        return try {
            fillInputBuffer(yBytes, width, height)
            interp.run(inputBuffer, outputBuffer)
            parseDetections(width, height)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Nearest-neighbour resize of the Y-plane to [INPUT_SIZE]×[INPUT_SIZE],
     * then replicate the single luma value across R, G, B channels.
     */
    private fun fillInputBuffer(yBytes: ByteArray, width: Int, height: Int) {
        inputBuffer.rewind()
        val scaleX = width.toFloat() / INPUT_SIZE
        val scaleY = height.toFloat() / INPUT_SIZE
        for (row in 0 until INPUT_SIZE) {
            val srcRow = (row * scaleY).toInt().coerceIn(0, height - 1)
            val rowOffset = srcRow * width
            for (col in 0 until INPUT_SIZE) {
                val srcCol = (col * scaleX).toInt().coerceIn(0, width - 1)
                val luma = (yBytes[rowOffset + srcCol].toInt() and 0xFF) / 255f
                inputBuffer.putFloat(luma) // R
                inputBuffer.putFloat(luma) // G
                inputBuffer.putFloat(luma) // B
            }
        }
        inputBuffer.rewind()
    }

    /**
     * Converts normalized [y1, x1, y2, x2, score, class_id] rows into image-space
     * centre points. Returns null (→ Hough fallback) if nothing passes the threshold.
     * Only accepts class 1 (pool-table-hole); ignores table and rail detections.
     */
    private fun parseDetections(width: Int, height: Int): List<PointF>? {
        val results = mutableListOf<PointF>()
        for (det in outputBuffer[0]) {
            if (det[4] < CONFIDENCE_THRESHOLD) continue
            if (det[5].toInt() != HOLE_CLASS_ID) continue
            // TF NMS convention: [y1, x1, y2, x2, ...]
            val cx = ((det[1] + det[3]) / 2f * width)
            val cy = ((det[0] + det[2]) / 2f * height)
            results.add(PointF(cx, cy))
            if (results.size >= MAX_POCKETS) break
        }
        return if (results.isNotEmpty()) results else null
    }
}
