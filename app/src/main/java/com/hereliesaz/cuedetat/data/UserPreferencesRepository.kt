package com.hereliesaz.cuedetat.data

import android.content.Context
import com.hereliesaz.cuedetat.view.state.DistanceUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("cuedetat_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DISTANCE_UNIT = "distance_unit"
    }

    fun getDistanceUnit(): DistanceUnit {
        // Imperial is now the default if no preference is saved.
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
}