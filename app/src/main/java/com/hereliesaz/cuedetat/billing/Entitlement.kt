// FILE: app/src/main/java/com/hereliesaz/cuedetat/billing/Entitlement.kt

package com.hereliesaz.cuedetat.billing

/**
 * Whether the user is entitled to Expert Mode features, and where that
 * answer came from.
 *
 * The rest of the app should only consume `active`. The other fields exist
 * for diagnostics, cache management, and future UI (e.g. expiry warnings).
 */
data class Entitlement(
    val active: Boolean,
    val source: EntitlementSource,
    val expiresAtMillis: Long?,
    val productId: String?,
    val lastVerifiedAtMillis: Long?,
    val isDeviceGenuine: Boolean = true
) {
    companion object {
        val NONE = Entitlement(
            active = false,
            source = EntitlementSource.NONE,
            expiresAtMillis = null,
            productId = null,
            lastVerifiedAtMillis = null,
            isDeviceGenuine = true
        )
    }
}

enum class EntitlementSource {
    /** Never entitled. */
    NONE,
    /** Play Billing client confirmed an active purchase in this session. */
    PLAY_LOCAL,
    /** Operating from cached entitlement; refresh has not succeeded recently. */
    OFFLINE_CACHED,
    /** FOSS flavor. Permanently active; billing code not present in this APK. */
    FOSS_BUILD,
    /**
     * Granted because the device's verified tester email matched the
     * build-baked allowlist (sourced from the tester Google Group at CI time).
     * Coexists with PLAY_LOCAL for diagnostics; tester license wins when both
     * apply.
     */
    TESTER_LICENSE,
}
