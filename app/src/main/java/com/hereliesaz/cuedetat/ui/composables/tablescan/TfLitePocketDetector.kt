package com.hereliesaz.cuedetat.ui.composables.tablescan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val POCKET_MODEL = "ml/merged_pocket_detector_final_float16.tflite"
private const val INPUT_SIZE = 640
private const val CONFIDENCE_THRESHOLD = 0.30f
private const val MAX_DETECTIONS = 300
private const val TABLE_CLASS_ID = 0
private const val HOLE_CLASS_ID = 1
private const val SIDE_CLASS_ID = 2

/**
 * TFLite-backed implementation of [PocketDetector] using the merged pocket model.
 * This replaces the failing OpenCVPocketDetector.
 */
class TfLitePocketDetector(private val context: Context) : PocketDetector {

    private val interpreter: Interpreter? by lazy {
        try {
            val fd = context.assets.openFd(POCKET_MODEL)
            val model = FileInputStream(fd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
            Log.d("TfLitePocketDetector", "TFLite model loaded successfully: $POCKET_MODEL")
            Interpreter(model, Interpreter.Options().setNumThreads(2))
        } catch (e: Exception) {
            Log.e("TfLitePocketDetector", "Failed to load $POCKET_MODEL: ${e.message}")
            null
        }
    }

    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())
    }

    private val outputBuffer = Array(1) { Array(MAX_DETECTIONS) { FloatArray(6) } }
    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

    override fun detect(bitmap: Bitmap): MlTableDetection? {
        val interp = interpreter ?: return null
        return try {
            preprocess(bitmap)
            interp.run(inputBuffer, outputBuffer)
            parseDetections(bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e("TfLitePocketDetector", "Inference failed: ${e.message}")
            null
        }
    }

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
            // RGB normalization
            inputBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }
        inputBuffer.rewind()
    }

    private fun parseDetections(width: Int, height: Int): MlTableDetection {
        val pockets = mutableListOf<PointF>()
        var tableBoundary: RectF? = null
        var maxTableScore = 0f

        for (det in outputBuffer[0]) {
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
}
