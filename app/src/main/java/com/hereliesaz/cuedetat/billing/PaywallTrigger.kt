// FILE: app/src/main/java/com/hereliesaz/cuedetat/billing/PaywallTrigger.kt

package com.hereliesaz.cuedetat.billing

/**
 * Where in the app a paywall surface was opened from. Carried with
 * ShowPaywall events so future analytics can compare conversion by surface.
 */
enum class PaywallTrigger {
    ONBOARDING,
    EXPERT_TOGGLE_TAP,
    NAV_TILE
}
