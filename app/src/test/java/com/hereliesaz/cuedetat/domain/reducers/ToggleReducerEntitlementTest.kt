package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import com.hereliesaz.cuedetat.domain.ReducerUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ToggleReducerEntitlementTest {

    private val reducerUtils = ReducerUtils()

    @Test
    fun setExperienceModeExpert_whenNotEntitled_keepsExistingMode() {
        val state = CueDetatState(
            experienceMode = ExperienceMode.BEGINNER,
            isExpertEntitled = false
        )
        val result = reduceToggleAction(
            state,
            MainScreenEvent.SetExperienceMode(ExperienceMode.EXPERT),
            reducerUtils
        )
        assertEquals(ExperienceMode.BEGINNER, result.experienceMode)
    }

    @Test
    fun setExperienceModeExpert_whenEntitled_changesMode() {
        val state = CueDetatState(
            experienceMode = ExperienceMode.BEGINNER,
            isExpertEntitled = true
        )
        val result = reduceToggleAction(
            state,
            MainScreenEvent.SetExperienceMode(ExperienceMode.EXPERT),
            reducerUtils
        )
        assertEquals(ExperienceMode.EXPERT, result.experienceMode)
    }

    @Test
    fun setExperienceModeBeginner_whenNotEntitled_changesMode() {
        val state = CueDetatState(
            experienceMode = ExperienceMode.HATER,
            isExpertEntitled = false
        )
        val result = reduceToggleAction(
            state,
            MainScreenEvent.SetExperienceMode(ExperienceMode.BEGINNER),
            reducerUtils
        )
        assertEquals(ExperienceMode.BEGINNER, result.experienceMode)
    }
}
