// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/UserPreferencesRepository.kt

package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.view.state.TutorialHighlightElement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

        // Tutorial-seen flags are stored as dedicated keys (not embedded in
        // STATE_JSON) so they survive the 2-second debounce on the main state
        // save. The state save can be cancelled indefinitely by the high-rate
        // FullOrientationChanged sensor events; these flags must reach disk
        // the moment the user taps "Done", or the tutorial fires again on
        // every fresh launch.
        val HAS_SEEN_BEGINNER_TUTORIAL = booleanPreferencesKey("has_seen_beginner_tutorial")
        val HAS_SEEN_DYNAMIC_BEGINNER_TUTORIAL = booleanPreferencesKey("has_seen_dynamic_beginner_tutorial")
        val HAS_SEEN_EXPERT_TUTORIAL = booleanPreferencesKey("has_seen_expert_tutorial")

        // Epoch millis the one-time Expert preview/trial was started. 0 (absent)
        // means the trial has never been used. A non-zero value both gates the
        // trial (one per install) and lets the entitlement repo compute expiry.
        val EXPERT_TRIAL_STARTED_AT = longPreferencesKey("expert_trial_started_at")
    }

    /** Epoch millis the Expert trial was started, or 0 if it never was. */
    suspend fun readExpertTrialStartedAt(): Long =
        dataStore.data.first()[PreferencesKeys.EXPERT_TRIAL_STARTED_AT] ?: 0L

    /** Records the Expert trial start time. Persisted immediately so a kill mid-trial can't reset it. */
    suspend fun setExpertTrialStartedAt(millis: Long) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.EXPERT_TRIAL_STARTED_AT] = millis }
    }

    data class TutorialSeenFlags(
        val beginner: Boolean,
        val dynamicBeginner: Boolean,
        val expert: Boolean,
    )

    suspend fun readTutorialSeenFlags(): TutorialSeenFlags {
        val prefs = dataStore.data.first()
        return TutorialSeenFlags(
            beginner = prefs[PreferencesKeys.HAS_SEEN_BEGINNER_TUTORIAL] ?: false,
            dynamicBeginner = prefs[PreferencesKeys.HAS_SEEN_DYNAMIC_BEGINNER_TUTORIAL] ?: false,
            expert = prefs[PreferencesKeys.HAS_SEEN_EXPERT_TUTORIAL] ?: false,
        )
    }

    /**
     * Persists only the flags that are set to `true`. Designed to be called
     * with the latest CueDetatState's seen flags whenever any of them flips.
     * Idempotent — re-writing `true` for an already-true flag is harmless.
     */
    suspend fun setTutorialSeenFlags(
        beginner: Boolean,
        dynamicBeginner: Boolean,
        expert: Boolean,
    ) {
        dataStore.edit { prefs ->
            if (beginner) prefs[PreferencesKeys.HAS_SEEN_BEGINNER_TUTORIAL] = true
            if (dynamicBeginner) prefs[PreferencesKeys.HAS_SEEN_DYNAMIC_BEGINNER_TUTORIAL] = true
            if (expert) prefs[PreferencesKeys.HAS_SEEN_EXPERT_TUTORIAL] = true
        }
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
            // isExpertEntitled is sourced live from EntitlementRepository on every
            // launch — persisting it lets stale values clobber the FOSS build's
            // always-true entitlement (and a Play user's just-redeemed purchase)
            // when the saved state is reloaded after the entitlement event has
            // already updated _uiState.
            val stateToSave = state.copy(experienceMode = null, isExpertEntitled = false)
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
