package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hereliesaz.cuedetat.ui.state.ShotType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserPreferences(
    val shotType: ShotType,
    val isDarkMode: Boolean,
    val showHelp: Boolean
)

@Singleton
class UserPreferenceRepository @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore

    private object PreferenceKeys {
        val SHOT_TYPE = stringPreferencesKey("shot_type")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SHOW_HELP = booleanPreferencesKey("show_help")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .map { preferences ->
            val shotTypeName = preferences[PreferenceKeys.SHOT_TYPE] ?: ShotType.CUT.name
            val shotType = try {
                ShotType.valueOf(shotTypeName)
            } catch (e: IllegalArgumentException) {
                ShotType.CUT
            }
            val isDarkMode = preferences[PreferenceKeys.DARK_MODE] ?: false
            val showHelp = preferences[PreferenceKeys.SHOW_HELP] ?: true

            UserPreferences(shotType, isDarkMode, showHelp)
        }

    suspend fun saveShotType(shotType: ShotType) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOT_TYPE] = shotType.name
        }
    }

    suspend fun saveDarkMode(isDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_MODE] = isDarkMode
        }
    }

    suspend fun saveShowHelp(showHelp: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_HELP] = showHelp
        }
    }
}