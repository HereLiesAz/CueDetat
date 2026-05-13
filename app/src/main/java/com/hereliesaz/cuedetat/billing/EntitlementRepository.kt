// FILE: app/src/main/java/com/hereliesaz/cuedetat/billing/EntitlementRepository.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for whether the user is entitled to Expert Mode.
 *
 * Implementations are flavor-specific:
 *  - play: wraps Play Billing Library, queries Google Play for purchases.
 *  - foss: returns Entitlement(active=true, source=FOSS_BUILD) permanently.
 *
 * The rest of the app consumes `entitlement.active` as a Boolean and is
 * unaware of which flavor is running.
 */
interface EntitlementRepository {

    /** Continuously updated entitlement state. Always has a value. */
    val entitlement: StateFlow<Entitlement>

    /**
     * Launches the Google Play purchase flow for the given base plan.
     * No-op in the FOSS flavor.
     *
     * Must be called from the UI thread because BillingClient.launchBillingFlow
     * requires an Activity reference.
     */
    suspend fun launchPurchase(activity: Activity, basePlan: BasePlanId)

    /** Force a re-query of purchases from Google Play. No-op in FOSS. */
    suspend fun refresh()

    /** Convenience alias for `refresh()` exposed to UI as "Restore Purchases". */
    suspend fun restorePurchases() = refresh()

    /**
     * Live product details for the paywall to render plan cards.
     * Emits Loading first, then either Loaded or Error.
     * In FOSS, emits a permanent NotApplicable state.
     */
    fun productDetails(): Flow<ProductDetailsState>
}

sealed class ProductDetailsState {
    object Loading : ProductDetailsState()
    data class Loaded(
        val monthlyFormattedPrice: String,
        val yearlyFormattedPrice: String,
        val trialDays: Int
    ) : ProductDetailsState()
    data class Error(val responseCode: Int, val message: String) : ProductDetailsState()
    /** FOSS only: paywall would never be shown so product details are not loaded. */
    object NotApplicable : ProductDetailsState()
}
