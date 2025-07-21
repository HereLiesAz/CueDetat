package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.cuedetat.view.state.OverlayState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.opencv.core.CvType
import org.opencv.core.Mat
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val STATE_JSON = stringPreferencesKey("state_json")
        val CAMERA_MATRIX_JSON = stringPreferencesKey("camera_matrix_json")
        val DIST_COEFFS_JSON = stringPreferencesKey("dist_coeffs_json")
    }

    val stateFlow: Flow<OverlayState?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.STATE_JSON]?.let { jsonString ->
                try {
                    gson.fromJson(jsonString, OverlayState::class.java)
                } catch (e: Exception) {
                    // Handle potential deserialization errors, e.g., after a state class change
                    null
                }
            }
        }

    val calibrationDataFlow: Flow<Pair<Mat?, Mat?>> = dataStore.data
        .map { preferences ->
            try {
                val matrixJson = preferences[PreferencesKeys.CAMERA_MATRIX_JSON]
                val coeffsJson = preferences[PreferencesKeys.DIST_COEFFS_JSON]

                val cameraMatrix = matrixJson?.let {
                    val listType = object : TypeToken<List<Double>>() {}.type
                    val data = gson.fromJson<List<Double>>(it, listType).toDoubleArray()
                    Mat(3, 3, CvType.CV_64F).apply { put(0, 0, *data) }
                }

                val distCoeffs = coeffsJson?.let {
                    val listType = object : TypeToken<List<Double>>() {}.type
                    val data = gson.fromJson<List<Double>>(it, listType).toDoubleArray()
                    Mat(1, 5, CvType.CV_64F).apply { put(0, 0, *data) }
                }

                Pair(cameraMatrix, distCoeffs)
            } catch (e: Exception) {
                Pair(null, null) // Return nulls if deserialization fails
            }
        }


    suspend fun saveState(state: OverlayState) {
        dataStore.edit { preferences ->
            val jsonString = gson.toJson(state)
            preferences[PreferencesKeys.STATE_JSON] = jsonString
        }
    }

    suspend fun saveCalibrationData(cameraMatrix: Mat, distCoeffs: Mat) {
        val cameraMatrixData = DoubleArray(9)
        cameraMatrix.get(0, 0, cameraMatrixData)
        val distCoeffsData = DoubleArray(5)
        distCoeffs.get(0, 0, distCoeffsData)

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_MATRIX_JSON] = gson.toJson(cameraMatrixData.toList())
            preferences[PreferencesKeys.DIST_COEFFS_JSON] = gson.toJson(distCoeffsData.toList())
        }
    }
}