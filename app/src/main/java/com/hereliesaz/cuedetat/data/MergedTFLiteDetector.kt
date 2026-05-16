package com.hereliesaz.cuedetat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.hereliesaz.cuedetat.ui.composables.tablescan.MlTableDetection
import com.hereliesaz.cuedetat.ui.composables.tablescan.PocketDetector
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val POCKET_MODEL_FILE = "ml/merged_pocket_detector_final_float16.tflite"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val POOL_CONFIDENCE_THRESHOLD = 0.25f
private const val MAX_DETECTIONS = 300

// Class indices from the YOLOv8n training data.yaml
//   0: pool-table   1: pool-table-hole   2: pool-table-side
private const val TABLE_CLASS_ID = 0
private const val HOLE_CLASS_ID = 1
private const val SIDE_CLASS_ID = 2

/**
 * TFLite detector for the pocket / pool-table model. Loads
 * [POCKET_MODEL_FILE] (FP16, NMS in-graph) and serves both:
 *
 *  - [detect]: returns [MlTableDetection] with pockets and the table boundary
 *    (consumed by AR setup / table scan flows).
 *  - [detectPool]: returns the raw bounding boxes (consumed by VisionRepository
 *    for ball-region overlays in dynamic beginner mode).
 *
 * Class name kept for binary compatibility with existing Hilt bindings; the
 * "merged" prefix is now historical — there is no multiplexed master file
 * any more.
 */
class MergedTFLiteDetector(private val context: Context) : PocketDetector {

    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val fd = context.assets.openFd(POCKET_MODEL_FILE)
            val channel = FileInputStream(fd.fileDescriptor).channel
            val buffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength,
            )
            interpreter = Interpreter(buffer, Interpreter.Options().setNumThreads(2))
            Log.d(TAG, "Loaded $POCKET_MODEL_FILE (${fd.declaredLength / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $POCKET_MODEL_FILE", e)
            Log.e(TAG,
                "Hint: if error mentions 'compressed', add androidResources { noCompress += \"tflite\" }")
            interpreter = null
        }
    }

    fun close() {
        runCatching { interpreter?.close() }
        interpreter = null
    }

    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
    }

    private val pocketOutput = Array(1) { Array(MAX_DETECTIONS) { FloatArray(6) } }
    private val poolOutput = Array(1) { Array(MAX_DETECTIONS) { FloatArray(6) } }
    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

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

    override fun detect(bitmap: Bitmap): MlTableDetection? {
        val interp = interpreter ?: return null
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, pocketOutput)
            parsePocketDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e(TAG, "Pocket inference failed: ${e.message}")
            null
        }
    }

    fun detectPool(bitmap: Bitmap): List<PoolDetection> {
        val interp = interpreter ?: return emptyList()
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, poolOutput)
            parsePoolDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e(TAG, "Pool inference failed: ${e.message}")
            emptyList()
        }
    }

    private fun parsePocketDetections(width: Int, height: Int): MlTableDetection {
        val pockets = mutableListOf<PointF>()
        var tableBoundary: RectF? = null
        var maxTableScore = 0f

        for (det in pocketOutput[0]) {
            val score = det[4]
            if (score < CONFIDENCE_THRESHOLD) continue
            val classId = det[5].toInt()

            when (classId) {
                TABLE_CLASS_ID -> {
                    if (score > maxTableScore) {
                        maxTableScore = score
                        tableBoundary = RectF(
                            det[1] * width,
                            det[0] * height,
                            det[3] * width,
                            det[2] * height,
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

        val pocketScore = (pockets.size.toFloat() / 6.0f).coerceAtMost(1.0f)
        val finalConfidence = (maxTableScore * 0.5f) + (pocketScore * 0.5f)

        return MlTableDetection(
            tableBoundary = tableBoundary,
            pockets = pockets,
            confidence = finalConfidence,
        )
    }

    private fun parsePoolDetections(width: Int, height: Int): List<PoolDetection> {
        val results = mutableListOf<PoolDetection>()
        for (det in poolOutput[0]) {
            val score = det[4]
            if (score < POOL_CONFIDENCE_THRESHOLD) continue
            val classId = det[5].toInt()

            val top = det[0] * height
            val left = det[1] * width
            val bottom = det[2] * height
            val right = det[3] * width
            results.add(PoolDetection(RectF(left, top, right, bottom), score, classId))
        }
        return results
    }

    companion object {
        private const val TAG = "MergedTFLiteDetector"
    }
}
