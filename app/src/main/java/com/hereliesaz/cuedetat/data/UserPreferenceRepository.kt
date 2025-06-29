package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hereliesaz.cuedetat.ui.ShotType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val SHOT_TYPE = stringPreferencesKey("shot_type")
        val TABLE_ROTATION = floatPreferencesKey("table_rotation")
    }

    val userPreferencesFlow = dataStore.data.map { preferences ->
        val shotType = ShotType.valueOf(preferences[PreferencesKeys.SHOT_TYPE] ?: ShotType.CUT.name)
        val tableRotation = preferences[PreferencesKeys.TABLE_ROTATION] ?: 0f
        UserPreferences(shotType, tableRotation)
    }

    suspend fun updateShotType(shotType: ShotType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOT_TYPE] = shotType.name
        }
    }

    suspend fun updateTableRotation(degrees: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TABLE_ROTATION] = degrees
        }
    }
}

data class UserPreferences(
    val shotType: ShotType,
    val tableRotation: Float
)