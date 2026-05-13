package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.billing.Entitlement
import com.hereliesaz.cuedetat.billing.EntitlementSource
import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementReducerTest {

    @Test
    fun entitlementChangedActive_setsFlag() {
        val state = CueDetatState(isExpertEntitled = false)
        val event = MainScreenEvent.EntitlementChanged(activeEntitlement())
        val result = reduceEntitlementAction(state, event)
        assertTrue(result.isExpertEntitled)
    }

    @Test
    fun entitlementChangedInactive_clearsFlag() {
        val state = CueDetatState(isExpertEntitled = true)
        val event = MainScreenEvent.EntitlementChanged(Entitlement.NONE)
        val result = reduceEntitlementAction(state, event)
        assertFalse(result.isExpertEntitled)
    }

    @Test
    fun entitlementLost_whileInExpertMode_forcesBeginner() {
        val state = CueDetatState(
            isExpertEntitled = true,
            experienceMode = ExperienceMode.EXPERT
        )
        val event = MainScreenEvent.EntitlementChanged(Entitlement.NONE)
        val result = reduceEntitlementAction(state, event)
        assertEquals(ExperienceMode.BEGINNER, result.experienceMode)
        assertFalse(result.isExpertEntitled)
    }

    @Test
    fun entitlementLost_whileInBeginnerMode_keepsBeginner() {
        val state = CueDetatState(
            isExpertEntitled = true,
            experienceMode = ExperienceMode.BEGINNER
        )
        val event = MainScreenEvent.EntitlementChanged(Entitlement.NONE)
        val result = reduceEntitlementAction(state, event)
        assertEquals(ExperienceMode.BEGINNER, result.experienceMode)
    }

    @Test
    fun entitlementGained_whileInBeginnerMode_doesNotChangeMode() {
        val state = CueDetatState(
            isExpertEntitled = false,
            experienceMode = ExperienceMode.BEGINNER
        )
        val event = MainScreenEvent.EntitlementChanged(activeEntitlement())
        val result = reduceEntitlementAction(state, event)
        // Mode change is the responsibility of PaywallViewModel, not this reducer.
        assertEquals(ExperienceMode.BEGINNER, result.experienceMode)
        assertTrue(result.isExpertEntitled)
    }

    private fun activeEntitlement() = Entitlement(
        active = true,
        source = EntitlementSource.PLAY_LOCAL,
        expiresAtMillis = null,
        productId = "expert_mode",
        lastVerifiedAtMillis = 0L
    )
}
