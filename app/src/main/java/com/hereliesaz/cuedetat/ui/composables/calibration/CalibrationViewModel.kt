// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/calibration/CalibrationViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.os.Build
import com.hereliesaz.cuedetat.data.CalibrationRepository
import com.hereliesaz.cuedetat.data.DeviceCalibrationDatabase
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationRepository: CalibrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceCalibrationDatabase: DeviceCalibrationDatabase
) : ViewModel() {

    init {
        checkAutoCalibration()
    }

    private val _detectedPattern = MutableStateFlow<List<Point>?>(null)
    val detectedPattern = _detectedPattern.asStateFlow()

    private val _capturedImageCount = MutableStateFlow(0)
    val capturedImageCount = _capturedImageCount.asStateFlow()

    private val _showSubmissionDialog = MutableStateFlow(false)
    val showSubmissionDialog = _showSubmissionDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

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
                _toastMessage.value = "Calibration failed. Please try again."
            }
        }
    }

    fun onDismissSubmission() {
        _showSubmissionDialog.value = false
    }

    fun onSubmitData() {
        // TODO: Implement backend submission logic
        _toastMessage.value = "Calibration data submitted successfully."
        _showSubmissionDialog.value = false
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    private fun checkAutoCalibration() {
        viewModelScope.launch {
            val existingData = userPreferencesRepository.calibrationDataFlow.firstOrNull()
            val (existingMatrix, existingCoeffs) = existingData ?: Pair(null, null)

            var hasCalibration = false
            if (existingMatrix != null && existingCoeffs != null) {
                hasCalibration = true
                existingMatrix.release()
                existingCoeffs.release()
            } else {
                existingMatrix?.release()
                existingCoeffs?.release()
            }

            if (!hasCalibration) {
                val deviceModel = Build.MODEL
                val profile = deviceCalibrationDatabase.getProfile(deviceModel)
                if (profile != null) {
                    val (cameraMatrix, distCoeffs) = profile
                    userPreferencesRepository.saveCalibrationData(cameraMatrix, distCoeffs)
                    _toastMessage.value = "Auto-calibrated for $deviceModel"
                    cameraMatrix.release()
                    distCoeffs.release()
                }
            }
        }
    }
}