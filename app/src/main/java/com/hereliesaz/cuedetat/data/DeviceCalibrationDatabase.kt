package com.hereliesaz.cuedetat.data

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCalibrationDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    data class CalibrationProfile(
        val model: String,
        val cameraMatrix: List<Double>,
        val distCoeffs: List<Double>
    )

    private data class ProfilesRoot(val profiles: List<CalibrationProfile>)

    // A map of device models to their calibration profiles.
    private var profiles: Map<String, CalibrationProfile>? = null
    private val mutex = Mutex()

    suspend fun getProfile(model: String): CalibrationProfile? {
        mutex.withLock {
            if (profiles == null) {
                profiles = loadProfilesFromAssets()
            }
        }
        return profiles?.get(model)
    }

    private suspend fun loadProfilesFromAssets(): Map<String, CalibrationProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("calibration_profiles.json")
                val reader = InputStreamReader(inputStream)
                val root = gson.fromJson(reader, ProfilesRoot::class.java)
                reader.close()
                root.profiles.associateBy { it.model }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to hardcoded profiles if asset loading fails
                mapOf(
                    "Generic Device" to CalibrationProfile(
                        model = "Generic Device",
                        cameraMatrix = listOf(
                            1000.0, 0.0, 500.0,
                            0.0, 1000.0, 500.0,
                            0.0, 0.0, 1.0
                        ),
                        distCoeffs = listOf(0.0, 0.0, 0.0, 0.0, 0.0)
                    ),
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
            }
        }
    }
}
