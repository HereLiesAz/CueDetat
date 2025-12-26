// FILE: app/src/main/java/com/hereliesaz/cuedetat/ui/composables/calibration/CalibrationViewModel.kt

package com.hereliesaz.cuedetat.ui.composables.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.cuedetat.data.CalibrationRepository
import com.hereliesaz.cuedetat.data.DeviceCalibrationDatabase
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.opencv.core.CvType
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
        checkAndApplyDeviceProfile()
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
        viewModelScope.launch {
            val (cameraMatrix, distCoeffs) = userPreferencesRepository.calibrationDataFlow.first()

            if (cameraMatrix != null && distCoeffs != null) {
                val success = calibrationRepository.submitCalibrationData(cameraMatrix, distCoeffs)

                if (success) {
                    _toastMessage.value = "Calibration data submitted successfully."
                    _showSubmissionDialog.value = false
                } else {
                    _toastMessage.value = "Submission failed."
                }
            } else {
                _toastMessage.value = "No calibration data to submit."
            }

            cameraMatrix?.release()
            distCoeffs?.release()
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    private fun checkAndApplyDeviceProfile() {
        viewModelScope.launch {
            // Check if calibration data already exists
            val existingData = userPreferencesRepository.calibrationDataFlow.first()
            if (existingData.first != null && existingData.second != null) {
                existingData.first?.release()
                existingData.second?.release()
                return@launch
            }
            // Release loaded mats if they were partially loaded or null (safe to call on null? no, but these are Mat objects from OpenCV)
            // The repository returns Mat objects. If they are not null, we must release them if we are not using them.
            // Wait, existingData.first and second are Mat?
            // Yes, from UserPreferencesRepository: Pair<Mat?, Mat?>
            // If we are just checking, we should release them immediately if they exist.
            existingData.first?.release()
            existingData.second?.release()

            val model = android.os.Build.MODEL
            // getProfile is now suspend and thread-safe
            val profile = deviceCalibrationDatabase.getProfile(model)

            if (profile != null) {
                val cameraMatrix = Mat(3, 3, CvType.CV_64F)
                cameraMatrix.put(0, 0, *profile.cameraMatrix.toDoubleArray())

                val distCoeffs = Mat(1, 5, CvType.CV_64F)
                distCoeffs.put(0, 0, *profile.distCoeffs.toDoubleArray())

                userPreferencesRepository.saveCalibrationData(cameraMatrix, distCoeffs)

                cameraMatrix.release()
                distCoeffs.release()

                _toastMessage.value = "Calibration profile applied for $model"
            }
        }
    }
}
