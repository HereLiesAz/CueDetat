package com.hereliesaz.cuedetat.data

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCalibrationDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val mutex = Mutex()
    private var profiles: Map<String, CalibrationProfile>? = null

    data class CalibrationProfile(
        val cameraMatrix: List<Double>,
        val distCoeffs: List<Double>
    )

    suspend fun getProfile(model: String): Pair<Mat, Mat>? {
        val loadedProfiles = mutex.withLock {
            if (profiles == null) {
                profiles = withContext(Dispatchers.IO) { loadProfiles() }
            }
            profiles
        }

        val profile = loadedProfiles?.get(model)

        return profile?.let {
            val cameraMatrix = Mat(3, 3, CvType.CV_64F).apply {
                put(0, 0, *it.cameraMatrix.toDoubleArray())
            }
            val distCoeffs = Mat(1, 5, CvType.CV_64F).apply {
                put(0, 0, *it.distCoeffs.toDoubleArray())
            }
            Pair(cameraMatrix, distCoeffs)
        }
    }

    private fun loadProfiles(): Map<String, CalibrationProfile> {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open("calibration_profiles.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<Map<String, Map<String, CalibrationProfile>>>() {}.type
            val data: Map<String, Map<String, CalibrationProfile>> = gson.fromJson(reader, type)
            reader.close()
            data["profiles"] ?: emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
}
