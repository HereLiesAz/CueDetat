package com.hereliesaz.cuedetat.data

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.hereliesaz.cuedetat.network.TrajectoryPoint
import com.hereliesaz.cuedetat.network.TrajectoryResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local on-device trajectory predictor backed by the distilled MYRIAD student
 * ONNX model (assets/myriad_billiard.onnx). Replaces the previous HTTP
 * round-trip to myriad_server.py.
 *
 * The model expects:
 *   image : Float32[1, 3, 128, 128]   (CHW, normalized to [-1, 1])
 *   poke  : Float32[1, 4]             (poke_x, poke_y, poke_dx, poke_dy)
 * and emits:
 *   trajectory : Float32[1, 30, 2]    (30 (x, y) points, normalized to [0, 1])
 *
 * Public surface (fetchTrajectory) is identical to the previous implementation
 * so no callers needed to change.
 */
@Singleton
class MyriadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    @Volatile
    private var session: OrtSession? = null
    private val sessionMutex = Mutex()

    private suspend fun ensureSession(): OrtSession? = sessionMutex.withLock {
        session ?: runCatching {
            context.assets.open(MODEL_ASSET).use { input ->
                ortEnv.createSession(input.readBytes(), OrtSession.SessionOptions())
            }
        }.onFailure { it.printStackTrace() }
            .getOrNull()
            ?.also { session = it }
    }

    suspend fun fetchTrajectory(
        bitmap: Bitmap,
        pokeStart: PointF,
        pokeVector: PointF,
    ): Result<TrajectoryResponse> = withContext(Dispatchers.IO) {
        val ortSession = ensureSession()
            ?: return@withContext Result.failure(IllegalStateException("ONNX session unavailable"))
        try {
            val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
            val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
            val resized = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            }

            val imageTensor = OnnxTensor.createTensor(
                ortEnv,
                bitmapToCHWBuffer(resized),
                longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong()),
            )

            // Normalize poke to the same coordinate system the student was
            // trained on: position in [0, 1] of source image, displacement also
            // expressed as a fraction of source image (further scaled by
            // POKE_VECTOR_SCALE so the magnitude lands roughly inside the
            // training distribution which used per-dt displacement).
            val pokeBuf = FloatBuffer.allocate(4).apply {
                put((pokeStart.x / srcW).coerceIn(0f, 1f))
                put((pokeStart.y / srcH).coerceIn(0f, 1f))
                put((pokeVector.x / srcW) * POKE_VECTOR_SCALE)
                put((pokeVector.y / srcH) * POKE_VECTOR_SCALE)
                rewind()
            }
            val pokeTensor = OnnxTensor.createTensor(ortEnv, pokeBuf, longArrayOf(1, 4))

            val inputs = mapOf("image" to imageTensor, "poke" to pokeTensor)
            val response = ortSession.run(inputs).use { result ->
                @Suppress("UNCHECKED_CAST")
                val raw = result[0].value as Array<Array<FloatArray>>  // [1, 30, 2]
                val frame = raw[0]
                val points = frame.map { TrajectoryPoint(it[0], it[1]) }
                TrajectoryResponse(points = points, confidence = 1.0f)
            }

            imageTensor.close()
            pokeTensor.close()
            if (resized !== bitmap) resized.recycle()

            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun bitmapToCHWBuffer(src: Bitmap): FloatBuffer {
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        src.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        val buf = FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE)
        // Channel-major layout: write all R, then all G, then all B.
        val plane = INPUT_SIZE * INPUT_SIZE
        val r = FloatArray(plane); val g = FloatArray(plane); val b = FloatArray(plane)
        for (i in 0 until plane) {
            val px = pixels[i]
            r[i] = ((px shr 16 and 0xFF) / 127.5f) - 1f
            g[i] = ((px shr 8  and 0xFF) / 127.5f) - 1f
            b[i] = ((px        and 0xFF) / 127.5f) - 1f
        }
        buf.put(r); buf.put(g); buf.put(b); buf.rewind()
        return buf
    }

    companion object {
        private const val MODEL_ASSET = "myriad_billiard.onnx"
        private const val INPUT_SIZE = 128
        // Heuristic: the training data treated poke_dx/dy as per-dt displacement
        // (~5% of frame). Runtime sends a full impact vector that's typically
        // far larger; this scale brings it into the trained distribution. Tune
        // alongside the smoke-test of the deployed predictor.
        private const val POKE_VECTOR_SCALE = 0.1f
    }
}
