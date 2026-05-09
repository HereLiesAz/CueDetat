// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/BillingQueryException.kt

package com.hereliesaz.cuedetat.billing

/**
 * Thrown when a Play Billing query returns a non-OK BillingResponseCode.
 *
 * Without this, the wrapper would return an empty list on transient failures
 * (network blip, billing service disconnect) — and the repository would map
 * the empty list to Entitlement.NONE, revoking a paying user's entitlement.
 */
class BillingQueryException(
    val responseCode: Int,
    val debugMessage: String?
) : Exception("Billing query failed: code=$responseCode message=$debugMessage")
