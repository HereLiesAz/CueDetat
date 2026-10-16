// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/UserPreferencesRepository.kt

package com.hereliesaz.cuedetat.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.hereliesaz.cuedetat.domain.CueDetatState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists [CueDetatState] as JSON in the `settings` Preferences DataStore.
 *
 * Read failures never crash the caller: an unreadable file ([IOException]) reads as empty
 * preferences, and a stored state that no longer parses reads as `null` (caller keeps its
 * defaults). In the parse-failure case the stored JSON is copied to a side key before the
 * next [saveState] overwrites it, so the user's old settings are kept for recovery rather
 * than silently destroyed.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val STATE_JSON = stringPreferencesKey("state_json")

        /** Last [STATE_JSON] value that failed to parse; written once, never overwritten. */
        val STATE_JSON_UNPARSEABLE = stringPreferencesKey("state_json_unparseable")
    }

    /** Set when [stateFlow] saw a stored state it could not parse. */
    @Volatile
    private var storedStateUnparseable = false

    /**
     * The saved state, or `null` when none is stored or it cannot be parsed. Emits on every
     * DataStore change; an IO error reading the file is logged and treated as empty.
     */
    val stateFlow: Flow<CueDetatState?> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Log.e(TAG, "Settings DataStore unreadable; using defaults", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.STATE_JSON]?.let { jsonString ->
                try {
                    gson.fromJson(jsonString, CueDetatState::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Stored state does not parse; using defaults, keeping a copy", e)
                    storedStateUnparseable = true
                    null
                }
            }
        }

    /**
     * Writes [state] (minus the per-launch `experienceMode`). If the previously stored state
     * failed to parse, it is first preserved under [PreferencesKeys.STATE_JSON_UNPARSEABLE].
     */
    suspend fun saveState(state: CueDetatState) {
        dataStore.edit { preferences ->
            if (storedStateUnparseable && preferences[PreferencesKeys.STATE_JSON_UNPARSEABLE] == null) {
                preferences[PreferencesKeys.STATE_JSON]?.let {
                    preferences[PreferencesKeys.STATE_JSON_UNPARSEABLE] = it
                }
            }
            storedStateUnparseable = false
            val stateToSave = state.copy(experienceMode = null)
            val jsonString = gson.toJson(stateToSave)
            preferences[PreferencesKeys.STATE_JSON] = jsonString
        }
    }

    private companion object {
        const val TAG = "UserPreferencesRepo"
    }
}
