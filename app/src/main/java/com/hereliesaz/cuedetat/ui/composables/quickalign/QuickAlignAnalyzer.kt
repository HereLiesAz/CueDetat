// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/quickalign/QuickAlignAnalyzer.kt

package com.hereliesaz.cuedetat.ui.composables.quickalign

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hereliesaz.cuedetat.utils.toMat
import java.util.concurrent.atomic.AtomicBoolean

class QuickAlignAnalyzer(
    private val viewModel: QuickAlignViewModel
) : ImageAnalysis.Analyzer {

    private val shouldCapture = AtomicBoolean(false)

    fun captureFrame() {
        shouldCapture.set(true)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (shouldCapture.compareAndSet(true, false)) {
            val mat = imageProxy.toMat()
            viewModel.onPhotoCaptured(mat)
        }
        imageProxy.close()
    }
}