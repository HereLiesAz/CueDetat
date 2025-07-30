// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/UserPreferencesRepository.kt

package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement
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

    val stateFlow: Flow<CueDetatState?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.STATE_JSON]?.let { jsonString ->
                try {
                    var deserializedState = gson.fromJson(jsonString, CueDetatState::class.java)

                    deserializedState?.let { state ->
                        if (state.tutorialHighlight == null) {
                            deserializedState =
                                deserializedState.copy(tutorialHighlight = TutorialHighlightElement.NONE)
                        }
                        deserializedState
                    }
                } catch (e: Exception) {
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


    suspend fun saveState(state: CueDetatState) {
        dataStore.edit { preferences ->
            val stateToSave = state.copy(experienceMode = null)
            val jsonString = gson.toJson(stateToSave)
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