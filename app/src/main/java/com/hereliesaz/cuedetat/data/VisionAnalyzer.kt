package com.hereliesaz.cuedetat.data

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hereliesaz.cuedetat.view.state.OverlayState
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * An ImageAnalysis.Analyzer that forwards camera frames and the current UI state
 * to the VisionRepository for processing.
 */
class VisionAnalyzer @Inject constructor(
    private val visionRepository: VisionRepository
) : ImageAnalysis.Analyzer {

    private val uiStateRef = AtomicReference<OverlayState?>(null)

    fun updateUiState(newState: OverlayState) {
        uiStateRef.set(newState)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        uiStateRef.get()?.let { state ->
            visionRepository.processImage(image, state)
        } ?: image.close() // Close the image if state is not available to prevent leaks
    }
}