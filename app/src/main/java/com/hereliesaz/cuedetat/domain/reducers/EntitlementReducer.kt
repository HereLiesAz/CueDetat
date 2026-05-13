// FILE: app/src/main/java/com/hereliesaz/cuedetat/domain/reducers/EntitlementReducer.kt

package com.hereliesaz.cuedetat.domain.reducers

import com.hereliesaz.cuedetat.domain.CueDetatState
import com.hereliesaz.cuedetat.domain.ExperienceMode
import com.hereliesaz.cuedetat.domain.MainScreenEvent

/**
 * Handles MainScreenEvent.EntitlementChanged.
 *
 * Sets isExpertEntitled. If the user just lost entitlement while in EXPERT
 * mode, also force-downgrades them to BEGINNER. Mode upgrades on entitlement
 * gain are NOT handled here — that is the responsibility of the paywall flow,
 * which has the original user intent (the tap that opened the sheet).
 */
internal fun reduceEntitlementAction(
    state: CueDetatState,
    action: MainScreenEvent.EntitlementChanged
): CueDetatState {
    val newActive = action.entitlement.active
    val mustDowngrade = !newActive && state.experienceMode == ExperienceMode.EXPERT
    return state.copy(
        isExpertEntitled = newActive,
        experienceMode = if (mustDowngrade) ExperienceMode.BEGINNER else state.experienceMode
    )
}
