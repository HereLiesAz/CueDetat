package com.hereliesaz.cuedetat.data

import android.content.Context
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import com.hereliesaz.cuedetat.view.state.TableSize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("cuedetat_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_TABLE_SIZE = "table_size"
    }

    fun getDistanceUnit(): DistanceUnit {
        val unitName = prefs.getString(KEY_DISTANCE_UNIT, DistanceUnit.IMPERIAL.name)
        return try {
            DistanceUnit.valueOf(unitName ?: DistanceUnit.IMPERIAL.name)
        } catch (e: IllegalArgumentException) {
            DistanceUnit.IMPERIAL
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        prefs.edit().putString(KEY_DISTANCE_UNIT, unit.name).apply()
    }

    fun getTableSize(): TableSize {
        // Default to 8ft table.
        val sizeName = prefs.getString(KEY_TABLE_SIZE, TableSize.EIGHT_FT.name)
        return try {
            TableSize.valueOf(sizeName ?: TableSize.EIGHT_FT.name)
        } catch (e: IllegalArgumentException) {
            TableSize.EIGHT_FT
        }
    }

    fun setTableSize(size: TableSize) {
        prefs.edit().putString(KEY_TABLE_SIZE, size.name).apply()
    }
}