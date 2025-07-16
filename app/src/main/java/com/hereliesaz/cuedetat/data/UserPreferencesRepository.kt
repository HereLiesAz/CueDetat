// FILE: app/src/main/java/com/hereliesaz/cuedetat/data/UserPreferencesRepository.kt

package com.hereliesaz.cuedetat.data

import android.content.Context
import com.hereliesaz.cuedetat.view.model.CvRefinementMethod
import com.hereliesaz.cuedetat.view.model.DistanceUnit
import com.hereliesaz.cuedetat.view.model.TableSize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_TABLE_SIZE = "table_size"
        private const val KEY_HOUGH_P1 = "hough_p1"
        private const val KEY_HOUGH_P2 = "hough_p2"
        private const val KEY_CANNY_T1 = "canny_t1"
        private const val KEY_CANNY_T2 = "canny_t2"
        private const val KEY_CV_REFINEMENT = "cv_refinement"
    }

    // --- Distance Unit ---
    fun getDistanceUnit(): DistanceUnit {
        val savedName = prefs.getString(KEY_DISTANCE_UNIT, DistanceUnit.IMPERIAL.name)
        return try {
            DistanceUnit.valueOf(savedName ?: DistanceUnit.IMPERIAL.name)
        } catch (e: IllegalArgumentException) {
            DistanceUnit.IMPERIAL
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        prefs.edit().putString(KEY_DISTANCE_UNIT, unit.name).apply()
    }

    // --- Table Size ---
    fun getTableSize(): TableSize {
        val savedName = prefs.getString(KEY_TABLE_SIZE, TableSize.EIGHT_FOOT.name)
        return try {
            TableSize.valueOf(savedName ?: TableSize.EIGHT_FOOT.name)
        } catch (e: IllegalArgumentException) {
            TableSize.EIGHT_FOOT
        }
    }

    fun setTableSize(size: TableSize) {
        prefs.edit().putString(KEY_TABLE_SIZE, size.name).apply()
    }


    // --- CV Parameters ---

    fun getCvRefinementMethod(): CvRefinementMethod {
        val savedName = prefs.getString(KEY_CV_REFINEMENT, CvRefinementMethod.HOUGH.name)
        return try {
            CvRefinementMethod.valueOf(savedName ?: CvRefinementMethod.HOUGH.name)
        } catch (e: IllegalArgumentException) {
            CvRefinementMethod.HOUGH
        }
    }

    fun setCvRefinementMethod(method: CvRefinementMethod) {
        prefs.edit().putString(KEY_CV_REFINEMENT, method.name).apply()
    }


    fun getCvHoughP1(): Float = prefs.getFloat(KEY_HOUGH_P1, 200f)
    fun setCvHoughP1(value: Float) = prefs.edit().putFloat(KEY_HOUGH_P1, value).apply()

    fun getCvHoughP2(): Float = prefs.getFloat(KEY_HOUGH_P2, 25f)
    fun setCvHoughP2(value: Float) = prefs.edit().putFloat(KEY_HOUGH_P2, value).apply()

    fun getCvCannyT1(): Float = prefs.getFloat(KEY_CANNY_T1, 50f)
    fun setCvCannyT1(value: Float) = prefs.edit().putFloat(KEY_CANNY_T1, value).apply()

    fun getCvCannyT2(): Float = prefs.getFloat(KEY_CANNY_T2, 100f)
    fun setCvCannyT2(value: Float) = prefs.edit().putFloat(KEY_CANNY_T2, value).apply()
}