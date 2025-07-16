package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserPreferences(
    val forceLightMode: Boolean?,
    val tableSize: String,
    val glowAmount: Float,
    val luminance: Float
)

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext context: Context) {

    private object PreferencesKeys {
        val FORCE_LIGHT_MODE = booleanPreferencesKey("force_light_mode")
        val TABLE_SIZE = stringPreferencesKey("table_size")
        val GLOW_AMOUNT = floatPreferencesKey("glow_amount")
        val LUMINANCE = floatPreferencesKey("luminance")
    }

    private val dataStore = context.dataStore

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val forceLightMode = preferences[PreferencesKeys.FORCE_LIGHT_MODE]
            val tableSize = preferences[PreferencesKeys.TABLE_SIZE] ?: "EIGHT_FOOT"
            val glowAmount = preferences[PreferencesKeys.GLOW_AMOUNT] ?: 0f
            val luminance = preferences[PreferencesKeys.LUMINANCE] ?: 0f
            UserPreferences(forceLightMode, tableSize, glowAmount, luminance)
        }

    suspend fun updateForceLightMode(forceLightMode: Boolean?) {
        dataStore.edit { preferences ->
            if (forceLightMode == null) {
                preferences.remove(PreferencesKeys.FORCE_LIGHT_MODE)
            } else {
                preferences[PreferencesKeys.FORCE_LIGHT_MODE] = forceLightMode
            }
        }
    }

    suspend fun updateTableSize(tableSize: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TABLE_SIZE] = tableSize
        }
    }

    suspend fun updateGlowAmount(glowAmount: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLOW_AMOUNT] = glowAmount
        }
    }

    suspend fun updateLuminance(luminance: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LUMINANCE] = luminance
        }
    }
}