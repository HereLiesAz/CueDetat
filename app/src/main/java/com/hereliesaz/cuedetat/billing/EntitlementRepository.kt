// FILE: app/src/main/java/com/hereliesaz/cuedetat/billing/EntitlementRepository.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import android.content.Intent
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

    /**
     * Try to grant a tester license by matching `email` against the
     * build-baked allowlist. Returns the result so UI can show feedback.
     * No-op in FOSS — that flavor is already entitled.
     */
    suspend fun applyTesterLicense(email: String): TesterLicenseResult = TesterLicenseResult.NotApplicable

    /** Forget a previously-applied tester license. No-op in FOSS. */
    suspend fun clearTesterLicense() {}

    /** Public diagnostic snapshot — used by the debug surface in the paywall. */
    fun diagnostics(): EntitlementDiagnostics = EntitlementDiagnostics(emptyList(), "n/a", false)

    /**
     * Returns a Google Sign-In intent the caller should launch via
     * [Activity.startActivityForResult] / `rememberLauncherForActivityResult`,
     * then feed the result back through [applyTesterLicenseFromSignInResult].
     *
     * Null in FOSS, and null in play if Sign-In isn't available (e.g. Play
     * Services missing).
     */
    fun googleSignInIntent(): Intent? = null

    /**
     * Pull the Google account email out of a Sign-In activity result and
     * try the tester-license allowlist against it. Single call so the UI
     * doesn't have to mention the email at all.
     */
    suspend fun applyTesterLicenseFromSignInResult(data: Intent?): TesterLicenseResult =
        TesterLicenseResult.NotApplicable
}

sealed class TesterLicenseResult {
    /** Email matched the allowlist; entitlement granted. */
    object Granted : TesterLicenseResult()
    /** Email did not match the allowlist baked into this build. */
    object NotOnAllowlist : TesterLicenseResult()
    /** This build wasn't assembled with any tester emails (e.g. local dev). */
    object AllowlistEmpty : TesterLicenseResult()
    /** Caller passed an empty / malformed email. */
    object InvalidEmail : TesterLicenseResult()
    /** FOSS flavor — no tester license concept. */
    object NotApplicable : TesterLicenseResult()
}

/**
 * Snapshot of recent billing state for the in-app diagnostic surface. Strings
 * only — this is rendered as-is for the user to copy into a bug report.
 */
data class EntitlementDiagnostics(
    val recentPurchaseSnapshots: List<String>,
    val lastRefreshOutcome: String,
    val testerAllowlistConfigured: Boolean,
)

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
