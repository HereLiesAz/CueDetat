package com.hereliesaz.cuedetat.data

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hereliesaz.cuedetat.domain.CueDetatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class VisionAnalyzer @Inject constructor(
    private val visionRepository: VisionRepository
) : ImageAnalysis.Analyzer {

    private val uiStateRef = AtomicReference<CueDetatState?>(null)

    private val _currentFrameBitmap = MutableStateFlow<Bitmap?>(null)
    val currentFrameBitmap: StateFlow<Bitmap?> = _currentFrameBitmap

    fun updateUiState(newState: CueDetatState) {
        uiStateRef.set(newState)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        uiStateRef.get()?.let { state ->
            if (state.experienceMode == com.hereliesaz.cuedetat.domain.ExperienceMode.HATER) {
                val bitmap = image.toBitmap()
                _currentFrameBitmap.value = bitmap
                // HATER mode only needs the frame for display — skip the full CV pipeline.
                image.close()
                return@let
            }
            if (state.isBeginnerViewLocked) {
                image.close()
            } else {
                visionRepository.processImage(image, state)
            }
        } ?: image.close() // Close the image if state is not available to prevent leaks
    }

    // Reuse output bitmap across frames to avoid per-frame allocation.
    private var outputBitmap: Bitmap? = null

    @OptIn(ExperimentalGetImage::class)
    private fun ImageProxy.toBitmap(): Bitmap? {
        val image = this.image ?: return null
        if (image.format != ImageFormat.YUV_420_888) return null

        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        // Reuse pixel array to avoid per-frame allocation.
        val pixels = IntArray(width * height)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val y = yBuf.get(row * yRowStride + col).toInt() and 0xFF
                val uvRow = row / 2
                val uvCol = col / 2
                val uvIdx = uvRow * uvRowStride + uvCol * uvPixelStride
                val u = (uBuf.get(uvIdx).toInt() and 0xFF) - 128
                val v = (vBuf.get(uvIdx).toInt() and 0xFF) - 128

                val r = (y + 1.370705f * v).toInt().coerceIn(0, 255)
                val g = (y - 0.698001f * v - 0.337633f * u).toInt().coerceIn(0, 255)
                val b = (y + 1.732446f * u).toInt().coerceIn(0, 255)
                pixels[row * width + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val bmp = outputBitmap?.takeIf { it.width == width && it.height == height }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { outputBitmap = it }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }
}