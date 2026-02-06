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

/**
 * ViewModel for the Calibration Screen.
 *
 * Manages the state of the calibration process, including:
 * - Storing detected pattern points from the analyzer.
 * - Accumulating captured frames for the final calculation.
 * - Triggering the calculation via [CalibrationRepository].
 * - Saving results to [UserPreferencesRepository].
 */
@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationRepository: CalibrationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceCalibrationDatabase: DeviceCalibrationDatabase
) : ViewModel() {

    init {
        // On initialization, check if we have calibration data or need to load a preset.
        checkAutoCalibration()
    }

    // State: Currently detected pattern points (for live UI overlay).
    private val _detectedPattern = MutableStateFlow<List<Point>?>(null)
    val detectedPattern = _detectedPattern.asStateFlow()

    // State: Number of valid frames captured so far.
    private val _capturedImageCount = MutableStateFlow(0)
    val capturedImageCount = _capturedImageCount.asStateFlow()

    // State: Whether to show the submission dialog.
    private val _showSubmissionDialog = MutableStateFlow(false)
    val showSubmissionDialog = _showSubmissionDialog.asStateFlow()

    // State: One-shot message for Toasts.
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    // Internal buffer of captured point sets.
    private val captured2DPoints = mutableListOf<MatOfPoint2f>()
    // Buffer for the most recent valid detection (used when "Capture" is clicked).
    private var latestCorners: MatOfPoint2f? = null
    // Resolution of the camera frames.
    private var imageSize: Size? = null

    /**
     * Processes a new frame from the analyzer.
     * Detects the pattern and updates the live overlay state.
     */
    fun processFrame(frame: Mat) {
        if (imageSize == null) {
            imageSize = frame.size()
        }
        viewModelScope.launch {
            val corners = calibrationRepository.findPattern(frame)
            _detectedPattern.value = corners?.toList()
            latestCorners = corners
            frame.release() // Release the mat after processing to avoid leaks.
        }
    }

    /**
     * Captures the currently detected pattern.
     * Adds the points to the dataset for calibration.
     */
    fun capturePattern() {
        latestCorners?.let {
            if (_capturedImageCount.value < 15) {
                captured2DPoints.add(it)
                _capturedImageCount.value++
            }
        }
    }

    /**
     * Finalizes the calibration process.
     * Computes the matrices and saves them if successful.
     */
    fun onCalibrationFinished() {
        val size = imageSize
        if (captured2DPoints.size < 10 || size == null) {
            // Not enough data to calibrate.
            // TODO: Better error handling.
            return
        }

        viewModelScope.launch {
            val result = calibrationRepository.calculateCalibration(captured2DPoints, size)
            if (result != null) {
                val (cameraMatrix, distCoeffs) = result
                // Save locally.
                userPreferencesRepository.saveCalibrationData(cameraMatrix, distCoeffs)
                cameraMatrix.release()
                distCoeffs.release()
                // Prompt to submit.
                _showSubmissionDialog.value = true
            } else {
                _toastMessage.value = "Calibration failed. Please try again."
            }
        }
    }

    fun onDismissSubmission() {
        _showSubmissionDialog.value = false
    }

    /**
     * Submits the saved calibration data to the backend.
     */
    fun onSubmitData() {
        viewModelScope.launch {
            try {
                // Get the latest calibration data from preferences.
                val calibrationData = userPreferencesRepository.calibrationDataFlow.firstOrNull()

                if (calibrationData != null) {
                    val cameraMatrix = calibrationData.first
                    val distCoeffs = calibrationData.second

                    if (cameraMatrix != null && distCoeffs != null) {
                        calibrationRepository.submitCalibrationData(cameraMatrix, distCoeffs)

                        // Release Mats after submission.
                        // Safe because Repository creates fresh copies from DataStore logic.
                        cameraMatrix.release()
                        distCoeffs.release()

                        _toastMessage.value = "Calibration data submitted successfully."
                        _showSubmissionDialog.value = false
                    } else {
                        _toastMessage.value = "No calibration data to submit."
                    }
                } else {
                    _toastMessage.value = "Failed to retrieve calibration data."
                }
            } catch (e: Exception) {
                android.util.Log.e("CalibrationViewModel", "Error submitting data", e)
                _toastMessage.value = "Submission failed."
            }
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    /**
     * Checks if local calibration exists. If not, attempts to load a preset based on device model.
     */
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
                // Lookup in static database.
                val profile = deviceCalibrationDatabase.getProfile(deviceModel)
                if (profile != null) {
                    val (cameraMatrix, distCoeffs) = profile
                    // Apply preset.
                    userPreferencesRepository.saveCalibrationData(cameraMatrix, distCoeffs)
                    _toastMessage.value = "Auto-calibrated for $deviceModel"
                    cameraMatrix.release()
                    distCoeffs.release()
                }
            }
        }
    }
}
