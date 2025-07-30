package com.hereliesaz.cuedetat.data

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.hereliesaz.cuedetat.ui.composables.calibration.CalibrationViewModel
import com.hereliesaz.cuedetat.utils.toMat

class CalibrationAnalyzer(
    private val viewModel: CalibrationViewModel
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mat = imageProxy.toMat()
        viewModel.processFrame(mat)
        imageProxy.close()
    }
}