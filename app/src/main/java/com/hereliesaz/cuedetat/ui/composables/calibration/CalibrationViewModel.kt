// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/calibration/CalibrationViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.CalibrationRepository
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationRepository: CalibrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _detectedPattern = MutableStateFlow<List<Point>?>(null)
    val detectedPattern = _detectedPattern.asStateFlow()

    private val _capturedImageCount = MutableStateFlow(0)
    val capturedImageCount = _capturedImageCount.asStateFlow()

    private val _showSubmissionDialog = MutableStateFlow(false)
    val showSubmissionDialog = _showSubmissionDialog.asStateFlow()

    private val captured2DPoints = mutableListOf<MatOfPoint2f>()
    private var latestCorners: MatOfPoint2f? = null
    private var imageSize: Size? = null

    fun processFrame(frame: Mat) {
        if (imageSize == null) {
            imageSize = frame.size()
        }
        viewModelScope.launch {
            val corners = calibrationRepository.findPattern(frame)
            _detectedPattern.value = corners?.toList()
            latestCorners = corners
            frame.release() // Release the mat after processing
        }
    }

    fun capturePattern() {
        latestCorners?.let {
            if (_capturedImageCount.value < 15) {
                captured2DPoints.add(it)
                _capturedImageCount.value++
            }
        }
    }

    fun onCalibrationFinished() {
        val size = imageSize
        if (captured2DPoints.size < 10 || size == null) {
            // Not enough data to calibrate, handle this with a toast in the future
            return
        }

        viewModelScope.launch {
            val result = calibrationRepository.calculateCalibration(captured2DPoints, size)
            if (result != null) {
                val (cameraMatrix, distCoeffs) = result
                userPreferencesRepository.saveCalibrationData(cameraMatrix, distCoeffs)
                cameraMatrix.release()
                distCoeffs.release()
                _showSubmissionDialog.value = true
            } else {
                // Handle calibration failure, show a toast in the future
            }
        }
    }

    fun onDismissSubmission() {
        _showSubmissionDialog.value = false
    }

    fun onSubmitData() {
        _showSubmissionDialog.value = false
    }
}