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
import java.nio.ByteBuffer
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
                if (bitmap != null) {
                    _currentFrameBitmap.value = bitmap
                }
            }
            visionRepository.processImage(image, state)
        } ?: image.close() // Close the image if state is not available to prevent leaks
    }

    @OptIn(ExperimentalGetImage::class)
    private fun ImageProxy.toBitmap(): Bitmap? {
        val image = this.image ?: return null

        if (image.format == ImageFormat.YUV_420_888) {
            val yBuffer = image.planes[0].buffer // Y
            val uBuffer = image.planes[1].buffer // U
            val vBuffer = image.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                ImageFormat.NV21,
                this.width,
                this.height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height),
                90,
                out
            )
            val imageBytes = out.toByteArray()
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } else {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}