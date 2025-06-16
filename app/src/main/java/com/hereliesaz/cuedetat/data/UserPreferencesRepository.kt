// app/src/main/java/com/hereliesaz/cuedetat/data/UserPreferencesRepository.kt
package com.hereliesaz.cuedetat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hereliesaz.cuedetat.view.model.TableSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Create a DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val TABLE_SIZE = stringPreferencesKey("table_size")
    }

    val tableSize: Flow<TableSize> = context.dataStore.data
        .map { preferences ->
            val sizeName = preferences[PreferencesKeys.TABLE_SIZE] ?: TableSize.SEVEN_FOOT.name
            try {
                TableSize.valueOf(sizeName)
            } catch (e: IllegalArgumentException) {
                TableSize.SEVEN_FOOT
            }
        }

    suspend fun saveTableSize(size: TableSize) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TABLE_SIZE] = size.name
        }
    }
}
