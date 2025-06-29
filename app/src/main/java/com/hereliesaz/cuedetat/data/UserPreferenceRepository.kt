package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hereliesaz.cuedetat.ui.state.ShotType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferenceRepository(context: Context) {

    private val dataStore = context.dataStore

    private object PreferenceKeys {
        val SHOT_TYPE = stringPreferencesKey("shot_type")
        val DARK_MODE = stringPreferencesKey("dark_mode")
    }

    val shotType: Flow<ShotType> = dataStore.data
        .map { preferences ->
            val shotTypeName = preferences[PreferenceKeys.SHOT_TYPE] ?: ShotType.CUT.name
            try {
                ShotType.valueOf(shotTypeName)
            } catch (e: IllegalArgumentException) {
                ShotType.CUT
            }
        }

    suspend fun saveShotType(shotType: ShotType) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOT_TYPE] = shotType.name
        }
    }

    val darkMode: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.DARK_MODE] ?: "System"
        }

    suspend fun saveDarkMode(darkMode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_MODE] = darkMode
        }
    }

}
