// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/PlayBillingEntitlementRepository.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hereliesaz.cuedetat.data.IntegrityRepository
import com.hereliesaz.cuedetat.data.IntegrityResult
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play-flavor implementation of EntitlementRepository.
 *
 *  - On construction, reads the cache so the initial value is non-NONE for
 *    paying users.
 *  - Connects BillingClient asynchronously, then queries purchases.
 *  - Reacts to purchase updates from the BillingClientWrapper SharedFlow.
 *  - Persists every successful query to the cache.
 *  - Applies a 14-day cap on cached entitlement when refresh has not
 *    succeeded recently.
 */
@Singleton
class PlayBillingEntitlementRepository @Inject constructor(
    private val billingClient: BillingClientWrapper,
    private val cacheStore: EntitlementCacheStore,
    private val testLicenseAllowlist: TestLicenseAllowlist,
    private val testLicenseStore: TestLicenseStore,
    private val integrityRepository: IntegrityRepository,
    private val googleAccountResolver: GoogleAccountResolver,
) : EntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Last computed Play entitlement. Combined with the tester license to
     *  produce the public `entitlement` value. */
    private val playEntitlement = MutableStateFlow(Entitlement.NONE)

    /** Currently-verified tester hash, or null when no tester license. */
    @Volatile
    private var verifiedTesterHash: String? = null

    /** Whether the last integrity check succeeded. */
    private var isDeviceGenuine = true

    /** Rolling log of recent purchase snapshot lines for the in-app diagnostic. */
    private val recentPurchaseLog = ArrayDeque<String>()
    private val recentPurchaseLogMaxSize = 30

    /** Human-readable summary of the last refresh outcome for the diagnostic. */
    @Volatile
    private var lastRefreshOutcome: String = "not yet attempted"

    private val _entitlement = MutableStateFlow(Entitlement.NONE)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    private val _productDetails = MutableSharedFlow<ProductDetailsState>(replay = 1)

    init {
        scope.launch {
            // 1. Load cache so the first emission is correct for returning users.
            val cached = runCatching { cacheStore.read() }.getOrDefault(Entitlement.NONE)
            playEntitlement.value = applyOfflineCap(cached, System.currentTimeMillis())
            Log.i(TAG, "init: cached entitlement active=${cached.active} source=${cached.source}")
            republish()

            // 2. Restore any previously-verified tester license. If the
            //    allowlist no longer contains the stored hash (group membership
            //    changed between builds) we drop it on the floor and clear it.
            testLicenseStore.verifiedHash.collect { hash ->
                val accepted = hash?.takeIf(testLicenseAllowlist::containsHash)
                if (hash != null && accepted == null) {
                    Log.i(TAG, "stored tester hash no longer on allowlist; clearing")
                    runCatching { testLicenseStore.clear() }
                } else if (accepted != null) {
                    Log.i(TAG, "tester license restored from store")
                }
                verifiedTesterHash = accepted
                republish()
            }
        }
        scope.launch {
            // 3. Monitor integrity status.
            integrityRepository.result.collect { result ->
                isDeviceGenuine = when (result) {
                    is IntegrityResult.Success -> true
                    is IntegrityResult.Failure -> false
                    else -> isDeviceGenuine // Preserve last known state while pending/idle
                }
                republish()
            }
        }
        scope.launch {
            // 4. Refresh from Play in the background.
            refresh()

            // (Silent tester-license auto-resolve is now Activity-bound and
            //  triggered by MainActivity once the first activity is alive.
            //  Credential Manager requires an Activity context; see
            //  autoResolveOnNextResume.)

            // 5. React to purchase updates.
            billingClient.purchaseUpdates.collect { purchases ->
                Log.i(TAG, "purchaseUpdates: ${purchases.size} purchases received")
                purchases.forEach { runCatching { billingClient.acknowledgeIfNeeded(it) } }
                refresh()
            }
        }

        // 7. Re-verify on every process foreground. Recovers from a transient
        //    failure during the cold-start refresh and from races where Play
        //    Store registers a purchase moments after our initial query.
        scope.launch {
            withContext(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(
                    LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
                        if (event == Lifecycle.Event.ON_START) {
                            scope.launch { refresh() }
                        }
                    }
                )
            }
        }
    }

    override suspend fun launchPurchase(activity: Activity, basePlan: BasePlanId) {
        val productDetails = billingClient.queryExpertProductDetails() ?: run {
            Log.w(TAG, "launchPurchase: product details unavailable for ${BillingProductIds.PRODUCT_ID_EXPERT}")
            _productDetails.tryEmit(ProductDetailsState.Error(-1, "Product details unavailable"))
            return
        }
        val offer = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == basePlan.tag }
            ?: run {
                Log.w(TAG, "launchPurchase: base plan '${basePlan.tag}' not configured in Play Console")
                _productDetails.tryEmit(
                    ProductDetailsState.Error(-1, "Base plan ${basePlan.tag} not configured")
                )
                return
            }
        Log.i(TAG, "launchPurchase: basePlan=${basePlan.tag} productId=${productDetails.productId}")
        billingClient.launchBillingFlow(activity, productDetails, offer.offerToken)
    }

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val purchases = runCatching { billingClient.queryActiveSubscriptions() }
            .getOrElse {
                // Transient failure: keep the previous entitlement (subject to
                // the 14-day offline cap) and do NOT overwrite the cache.
                Log.w(TAG, "queryActiveSubscriptions failed; preserving cached entitlement", it)
                lastRefreshOutcome = "queryActiveSubscriptions failed: ${it.message ?: it::class.java.simpleName}"
                playEntitlement.value = applyOfflineCap(playEntitlement.value, now)
                republish()
                return
            }
        val snapshots = purchases.flatMap { it.toSnapshots() }
        snapshots.forEach {
            val line = "snapshot productId=${it.productId} state=${it.purchaseState} ack=${it.isAcknowledged} autoRenew=${it.isAutoRenewing}"
            Log.i(TAG, line)
            recordPurchaseSnapshot(line)
        }
        if (snapshots.isEmpty()) {
            val line = "snapshot none — Play returned 0 active subscriptions"
            Log.i(TAG, line)
            recordPurchaseSnapshot(line)
        }
        val mapped = PurchaseToEntitlementMapper.map(snapshots, now)
        val outcome = "active=${mapped.active} source=${mapped.source} productId=${mapped.productId ?: "n/a"} " +
                "(expected productId=${BillingProductIds.PRODUCT_ID_EXPERT})"
        Log.i(TAG, "refresh result $outcome")
        lastRefreshOutcome = outcome
        playEntitlement.value = mapped
        runCatching { cacheStore.write(mapped) }
        republish()
    }

    /**
     * Attempt to grant a tester license by hashing `email` and matching the
     * baked-in allowlist. Public via [EntitlementRepository.applyTesterLicense]
     * so the paywall can offer a manual fallback.
     */
    suspend fun tryApplyTesterLicense(email: String): Boolean {
        if (!testLicenseAllowlist.isAllowed(email)) return false
        val hash = TestLicenseAllowlist.sha256Hex(email.trim().lowercase())
        testLicenseStore.setVerifiedHash(hash)
        verifiedTesterHash = hash
        republish()
        Log.i(TAG, "tryApplyTesterLicense: granted (hash=$hash)")
        return true
    }

    override suspend fun applyTesterLicense(email: String): TesterLicenseResult {
        if (!isPlausibleTesterEmail(email)) {
            Log.i(TAG, "applyTesterLicense: invalid email rejected")
            return TesterLicenseResult.InvalidEmail
        }
        if (!testLicenseAllowlist.isConfigured) {
            Log.i(TAG, "applyTesterLicense: allowlist not configured in this build")
            return TesterLicenseResult.AllowlistEmpty
        }
        return if (tryApplyTesterLicense(email)) {
            TesterLicenseResult.Granted
        } else {
            Log.i(TAG, "applyTesterLicense: hash not on allowlist")
            TesterLicenseResult.NotOnAllowlist
        }
    }

    override suspend fun clearTesterLicense() {
        testLicenseStore.clear()
        verifiedTesterHash = null
        republish()
        Log.i(TAG, "clearTesterLicense: revoked")
    }

    override fun diagnostics(): EntitlementDiagnostics {
        val snapshots = synchronized(recentPurchaseLog) { recentPurchaseLog.toList() }
        return EntitlementDiagnostics(
            recentPurchaseSnapshots = snapshots,
            lastRefreshOutcome = lastRefreshOutcome,
            testerAllowlistConfigured = testLicenseAllowlist.isConfigured,
        )
    }

    override val isCredentialManagerAvailable: Boolean
        get() = googleAccountResolver.isConfigured && testLicenseAllowlist.isConfigured

    override suspend fun resolveTesterLicenseViaCredentialManager(
        activity: android.app.Activity,
    ): TesterLicenseResult {
        if (!testLicenseAllowlist.isConfigured) {
            Log.i(TAG, "resolveTesterLicenseViaCredentialManager: allowlist not configured")
            return TesterLicenseResult.AllowlistEmpty
        }
        if (!googleAccountResolver.isConfigured) {
            Log.i(TAG, "resolveTesterLicenseViaCredentialManager: Credential Manager not configured")
            return TesterLicenseResult.NotApplicable
        }
        val email = googleAccountResolver.resolveInteractive(activity)
        if (email == null) {
            Log.i(TAG, "resolveTesterLicenseViaCredentialManager: no email returned from picker")
            return TesterLicenseResult.InvalidEmail
        }
        return applyTesterLicense(email)
    }

    override suspend fun silentlyResolveTesterLicense(
        activity: android.app.Activity,
    ): TesterLicenseResult {
        if (verifiedTesterHash != null) return TesterLicenseResult.Granted
        if (!testLicenseAllowlist.isConfigured) return TesterLicenseResult.AllowlistEmpty
        if (!googleAccountResolver.isConfigured) return TesterLicenseResult.NotApplicable
        val email = googleAccountResolver.resolveAuthorizedSilently(activity)
            ?: return TesterLicenseResult.InvalidEmail
        return applyTesterLicense(email)
    }

    private fun recordPurchaseSnapshot(line: String) {
        synchronized(recentPurchaseLog) {
            recentPurchaseLog.addLast("${System.currentTimeMillis()}: $line")
            while (recentPurchaseLog.size > recentPurchaseLogMaxSize) {
                recentPurchaseLog.removeFirst()
            }
        }
    }

    /**
     * Combine the Play entitlement and the tester license into the public
     * `entitlement` value. Tester license wins when both are active so the
     * UI can render an "active tester" badge even if a real subscription
     * also exists.
     */
    private fun republish() {
        val play = playEntitlement.value
        val tester = verifiedTesterHash
        val genuine = isDeviceGenuine
        _entitlement.value = if (tester != null) {
            Entitlement(
                active = true,
                source = EntitlementSource.TESTER_LICENSE,
                expiresAtMillis = null,
                productId = play.productId,
                lastVerifiedAtMillis = System.currentTimeMillis(),
                isDeviceGenuine = genuine
            )
        } else {
            play.copy(isDeviceGenuine = genuine)
        }
    }

    override fun productDetails(): Flow<ProductDetailsState> {
        scope.launch {
            _productDetails.tryEmit(ProductDetailsState.Loading)
            val details = runCatching { billingClient.queryExpertProductDetails() }
                .getOrNull()
            if (details == null) {
                Log.w(TAG, "productDetails: query returned null")
                _productDetails.tryEmit(
                    ProductDetailsState.Error(-1, "Product details query failed")
                )
                return@launch
            }
            val monthly = details.findFormattedPrice(BasePlanId.MONTHLY)
            val yearly = details.findFormattedPrice(BasePlanId.YEARLY)
            val trialDays = details.findTrialDays()
            if (monthly == null || yearly == null) {
                Log.w(TAG, "productDetails: missing base plan price (monthly=$monthly yearly=$yearly)")
                _productDetails.tryEmit(
                    ProductDetailsState.Error(-1, "Configured base plans missing")
                )
                return@launch
            }
            _productDetails.tryEmit(
                ProductDetailsState.Loaded(monthly, yearly, trialDays)
            )
        }
        return _productDetails.asSharedFlow()
    }

    private fun ProductDetails.findFormattedPrice(basePlan: BasePlanId): String? {
        val offer = subscriptionOfferDetails?.firstOrNull { it.basePlanId == basePlan.tag }
            ?: return null
        return offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
    }

    private fun ProductDetails.findTrialDays(): Int {
        val offer = subscriptionOfferDetails?.firstOrNull() ?: return 0
        val trial = offer.pricingPhases.pricingPhaseList
            .firstOrNull { it.priceAmountMicros == 0L }
            ?: return 0
        val period = trial.billingPeriod
        return runCatching {
            period.removePrefix("P").removeSuffix("D").toInt()
        }.getOrDefault(0)
    }

    companion object {
        private const val TAG = "PlayBillingEntitlement"
    }
}

internal const val OFFLINE_CAP_MILLIS: Long = 14L * 24L * 60L * 60L * 1000L // 14 days

internal fun applyOfflineCap(entitlement: Entitlement, nowMillis: Long): Entitlement {
    val verified = entitlement.lastVerifiedAtMillis ?: return entitlement
    val ageMillis = nowMillis - verified
    return if (ageMillis > OFFLINE_CAP_MILLIS) {
        Entitlement.NONE.copy(lastVerifiedAtMillis = verified)
    } else if (entitlement.active && entitlement.source != EntitlementSource.OFFLINE_CACHED) {
        entitlement.copy(source = EntitlementSource.OFFLINE_CACHED)
    } else {
        entitlement
    }
}
