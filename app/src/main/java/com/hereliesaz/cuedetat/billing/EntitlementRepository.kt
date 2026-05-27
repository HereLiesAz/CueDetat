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
     * Run the Credential Manager one-tap account picker against the
     * tester-license allowlist. The picker is shown anchored to
     * `activity`; the resolved email is hashed in memory, never persisted
     * in plain text. Returns [TesterLicenseResult.Granted] on a match.
     *
     * No-op (returns NotApplicable) in FOSS, and in play builds that were
     * assembled without `GOOGLE_OAUTH_WEB_CLIENT_ID` configured.
     */
    suspend fun resolveTesterLicenseViaCredentialManager(activity: Activity): TesterLicenseResult =
        TesterLicenseResult.NotApplicable

    /**
     * Best-effort silent variant. Only returns Granted if the user has
     * previously authorized this app for a Google account that happens to
     * be on the allowlist. Safe to call on app start without prompting.
     */
    suspend fun silentlyResolveTesterLicense(activity: Activity): TesterLicenseResult =
        TesterLicenseResult.NotApplicable

    /** Whether the Credential Manager flow is available in this build. */
    val isCredentialManagerAvailable: Boolean get() = false
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
 * Single source of truth for tester-email validation. Matches what the
 * repository's [EntitlementRepository.applyTesterLicense] accepts as
 * "well-formed enough to bother hashing", so UI Submit buttons enable/
 * disable consistently with the actual accept/reject behavior. This is
 * not a full RFC 5322 check — the allowlist is just a sha256 compare,
 * so we only need a basic shape gate.
 */
fun isPlausibleTesterEmail(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.isNotEmpty() && trimmed.contains('@')
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
