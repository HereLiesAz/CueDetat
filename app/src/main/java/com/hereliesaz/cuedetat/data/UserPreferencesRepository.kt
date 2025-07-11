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
        private const val KEY_CV_HOUGH_P1 = "cv_hough_p1"
        private const val KEY_CV_HOUGH_P2 = "cv_hough_p2"
        private const val KEY_CV_CANNY_T1 = "cv_canny_t1"
        private const val KEY_CV_CANNY_T2 = "cv_canny_t2"
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

    fun getCvHoughP1(): Float = prefs.getFloat(KEY_CV_HOUGH_P1, 100f)
    fun setCvHoughP1(value: Float) = prefs.edit().putFloat(KEY_CV_HOUGH_P1, value).apply()

    fun getCvHoughP2(): Float = prefs.getFloat(KEY_CV_HOUGH_P2, 20f)
    fun setCvHoughP2(value: Float) = prefs.edit().putFloat(KEY_CV_HOUGH_P2, value).apply()

    fun getCvCannyT1(): Float = prefs.getFloat(KEY_CV_CANNY_T1, 50f)
    fun setCvCannyT1(value: Float) = prefs.edit().putFloat(KEY_CV_CANNY_T1, value).apply()

    fun getCvCannyT2(): Float = prefs.getFloat(KEY_CV_CANNY_T2, 150f)
    fun setCvCannyT2(value: Float) = prefs.edit().putFloat(KEY_CV_CANNY_T2, value).apply()
}