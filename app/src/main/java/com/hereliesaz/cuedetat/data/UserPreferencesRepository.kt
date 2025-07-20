package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.hereliesaz.cuedetat.view.state.OverlayState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun saveState(state: OverlayState) {
        dataStore.edit { preferences ->
            val jsonString = gson.toJson(state)
            preferences[PreferencesKeys.STATE_JSON] = jsonString
        }
    }
}