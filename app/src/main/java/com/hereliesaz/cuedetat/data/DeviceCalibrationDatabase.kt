package com.hereliesaz.cuedetat.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCalibrationDatabase @Inject constructor() {

    data class CalibrationProfile(
        val model: String,
        val cameraMatrix: List<Double>,
        val distCoeffs: List<Double>
    )

    // A map of device models to their calibration profiles.
    // Ideally, this would be populated from a remote server or a comprehensive local asset.
    private val profiles = mapOf(
        // Example profile for a generic device
        "Generic Device" to CalibrationProfile(
            model = "Generic Device",
            // Identity-like matrix with some assumed focal length
            cameraMatrix = listOf(
                1000.0, 0.0, 500.0,
                0.0, 1000.0, 500.0,
                0.0, 0.0, 1.0
            ),
            distCoeffs = listOf(0.0, 0.0, 0.0, 0.0, 0.0)
        ),
        // Placeholder for Pixel 6
        "Pixel 6" to CalibrationProfile(
            model = "Pixel 6",
            cameraMatrix = listOf(
                1000.0, 0.0, 500.0,
                0.0, 1000.0, 500.0,
                0.0, 0.0, 1.0
            ),
            distCoeffs = listOf(0.0, 0.0, 0.0, 0.0, 0.0)
        )
    )

    fun getProfile(model: String): CalibrationProfile? {
        return profiles[model]
    }
}
