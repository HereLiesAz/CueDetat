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

private const val MASTER_MODEL_FILE = "ml/MASTER_POOL_MODEL.tflite"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val MAX_DETECTIONS = 300

// Class indices for the pocket detector models
private const val TABLE_CLASS_ID = 0
private const val HOLE_CLASS_ID = 1
private const val SIDE_CLASS_ID = 2

/**
 * A master TFLite detector that runs FOUR models from a single binary package.
 * It maps the file segments to separate interpreters for maximum stability.
 */
class MergedTFLiteDetector(private val context: Context) : PocketDetector {

    // Head mapping indices based on MASTER_POOL_MODEL.tflite.meta
    private val HEAD_POCKET_50E = 0 // pocket_detector_50e_fp16
    private val HEAD_POOL_PIVOT = 2 // pool_detector_pivot_fp16

    private val interpreters = mutableMapOf<Int, Interpreter>()

    init {
        loadMasterPackage()
    }

    private fun loadMasterPackage() {
        try {
            val fd = context.assets.openFd(MASTER_MODEL_FILE)
            val fullChannel = FileInputStream(fd.fileDescriptor).channel
            
            // Offsets from MASTER_POOL_MODEL.tflite.meta
            val modelOffsets = listOf(0L, 6242868L, 12485737L, 18729068L)
            val modelSizes = listOf(6242868L, 6242869L, 6243331L, 6242869L)

            for (i in 0 until 4) {
                val offset = fd.startOffset + modelOffsets[i]
                val size = modelSizes[i]
                val buffer = fullChannel.map(FileChannel.MapMode.READ_ONLY, offset, size)
                interpreters[i] = Interpreter(buffer, Interpreter.Options().setNumThreads(2))
            }
            Log.d("MergedTFLiteDetector", "Master package loaded: 4 interpreters active.")
        } catch (e: Exception) {
            Log.e("MergedTFLiteDetector", "Failed to load master binary: ${e.message}")
        }
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
        val interp = interpreters[HEAD_POCKET_50E] ?: return null
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, pocketOutput)
            parsePocketDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e("MergedTFLiteDetector", "Pocket inference failed: ${e.message}")
            null
        }
    }

    fun detectPool(bitmap: Bitmap): List<PoolDetection> {
        val interp = interpreters[HEAD_POOL_PIVOT] ?: return emptyList()
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, poolOutput)
            parsePoolDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e("MergedTFLiteDetector", "Pool inference failed: ${e.message}")
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

        val pocketScore = (pockets.size.toFloat() / 6.0f).coerceAtMost(1.0f)
        val finalConfidence = (maxTableScore * 0.5f) + (pocketScore * 0.5f)

        return MlTableDetection(
            tableBoundary = tableBoundary,
            pockets = pockets,
            confidence = finalConfidence
        )
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
