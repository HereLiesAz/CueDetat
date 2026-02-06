// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignAnalyzer.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hereliesaz.cuedetat.utils.toMat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ImageAnalysis.Analyzer for the Quick Align feature.
 *
 * It listens for a "capture" trigger from the ViewModel. When triggered,
 * it converts the current frame to an OpenCV Mat and passes it to the ViewModel
 * for processing (feature extraction or display).
 */
class QuickAlignAnalyzer(
    private val viewModel: QuickAlignViewModel
) : ImageAnalysis.Analyzer {

    // Thread-safe flag to trigger a single frame capture.
    private val shouldCapture = AtomicBoolean(false)

    /**
     * Signals the analyzer to capture the next available frame.
     */
    fun captureFrame() {
        shouldCapture.set(true)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // Check if capture is requested and atomically set back to false.
        if (shouldCapture.compareAndSet(true, false)) {
            // Convert ImageProxy to OpenCV Mat.
            val mat = imageProxy.toMat()
            // Send to VM.
            viewModel.onPhotoCaptured(mat)
        }
        // Always close the proxy to allow the camera to produce the next frame.
        imageProxy.close()
    }
}
