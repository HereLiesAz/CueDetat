package com.hereliesaz.cuedetat.data

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * An ImageAnalysis.Analyzer that forwards camera frames to the VisionRepository for processing.
 */
class VisionAnalyzer @Inject constructor(
    private val visionRepository: VisionRepository
) : ImageAnalysis.Analyzer {

    private val expectedPixelRadius = AtomicReference(35f)
    private val lockedHsvColor = AtomicReference<FloatArray?>(null)

    fun updateExpectedRadius(radius: Float) {
        expectedPixelRadius.set(radius)
    }

    fun updateLockedHsvColor(hsv: FloatArray?) {
        lockedHsvColor.set(hsv)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        visionRepository.processImage(image, expectedPixelRadius.get(), lockedHsvColor.get())
    }
}