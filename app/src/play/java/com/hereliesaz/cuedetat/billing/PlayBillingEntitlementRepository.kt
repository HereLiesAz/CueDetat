// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/PlayBillingEntitlementRepository.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import android.util.Log
import com.hereliesaz.cuedetat.data.IntegrityRepository
import com.hereliesaz.cuedetat.data.IntegrityResult
import com.hereliesaz.cuedetat.data.UserPreferencesRepository
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    private val userPreferencesRepository: UserPreferencesRepository,
) : EntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Epoch millis the Expert trial was started; 0 = never used. */
    @Volatile
    private var trialStartedAt: Long = 0L

    private fun trialActiveNow(now: Long): Boolean =
        trialStartedAt > 0L && now < trialStartedAt + TRIAL_DURATION_MS

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
            // A one-time unlock never expires, so the cached value is used as-is
            // (no offline cap). Play will still re-confirm ownership when reachable.
            val cached = runCatching { cacheStore.read() }.getOrDefault(Entitlement.NONE)
            playEntitlement.value = cached
            Log.i(TAG, "init: cached entitlement active=${cached.active} source=${cached.source}")

            // Restore any in-flight Expert trial so a still-valid preview keeps
            // working across process restarts, and schedule its expiry.
            trialStartedAt = runCatching { userPreferencesRepository.readExpertTrialStartedAt() }.getOrDefault(0L)
            republish()
            if (trialActiveNow(System.currentTimeMillis())) scheduleTrialExpiry()

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

        // Foreground re-verification is driven solely by MainActivity.onResume →
        // MainViewModel.refreshEntitlement() (TTL-coalesced). A second ON_START observer
        // here used to double the Play query on every resume; removed. Purchase races are
        // still covered by the purchaseUpdates collector above and the cold-start refresh.
    }

    override suspend fun launchPurchase(activity: Activity) {
        val productDetails = billingClient.queryExpertProductDetails() ?: run {
            Log.w(TAG, "launchPurchase: product details unavailable for ${BillingProductIds.PRODUCT_ID_EXPERT}")
            _productDetails.tryEmit(ProductDetailsState.Error(-1, "Product details unavailable"))
            return
        }
        if (productDetails.oneTimePurchaseOfferDetails == null) {
            Log.w(TAG, "launchPurchase: '${productDetails.productId}' is not configured as a one-time product")
            _productDetails.tryEmit(
                ProductDetailsState.Error(-1, "Product not configured as a one-time purchase")
            )
            return
        }
        Log.i(TAG, "launchPurchase: productId=${productDetails.productId}")
        billingClient.launchBillingFlow(activity, productDetails)
    }

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val purchases = runCatching { billingClient.queryOwnedPurchases() }
            .getOrElse {
                // Transient failure: keep the previous (owned, never-expiring)
                // entitlement and do NOT overwrite the cache.
                Log.w(TAG, "queryOwnedPurchases failed; preserving cached entitlement", it)
                lastRefreshOutcome = "queryOwnedPurchases failed: ${it.message ?: it::class.java.simpleName}"
                republish()
                return
            }
        val snapshots = purchases.flatMap { it.toSnapshots() }
        snapshots.forEach {
            val line = "snapshot productId=${it.productId} state=${it.purchaseState} ack=${it.isAcknowledged}"
            Log.i(TAG, line)
            recordPurchaseSnapshot(line)
        }
        if (snapshots.isEmpty()) {
            val line = "snapshot none — Play returned 0 owned products"
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
        val now = System.currentTimeMillis()
        _entitlement.value = when {
            tester != null -> Entitlement(
                active = true,
                source = EntitlementSource.TESTER_LICENSE,
                expiresAtMillis = null,
                productId = play.productId,
                lastVerifiedAtMillis = now,
                isDeviceGenuine = genuine
            )
            play.active -> play.copy(isDeviceGenuine = genuine)
            trialActiveNow(now) -> Entitlement(
                active = true,
                source = EntitlementSource.TRIAL,
                expiresAtMillis = trialStartedAt + TRIAL_DURATION_MS,
                productId = play.productId,
                lastVerifiedAtMillis = now,
                isDeviceGenuine = genuine
            )
            else -> play.copy(isDeviceGenuine = genuine)
        }
    }

    override val isExpertTrialAvailable: Boolean
        get() = trialStartedAt == 0L

    override suspend fun startExpertTrial(): Boolean {
        if (trialStartedAt != 0L) {
            Log.i(TAG, "startExpertTrial: trial already used")
            return false
        }
        if (_entitlement.value.active) {
            Log.i(TAG, "startExpertTrial: already entitled; not starting trial")
            return false
        }
        val now = System.currentTimeMillis()
        trialStartedAt = now
        runCatching { userPreferencesRepository.setExpertTrialStartedAt(now) }
        Log.i(TAG, "startExpertTrial: started 1h Expert preview")
        republish()
        scheduleTrialExpiry()
        return true
    }

    /**
     * Re-publishes the entitlement once the active trial elapses, so the app
     * downgrades EXPERT→BEGINNER on time even if it stays in the foreground.
     * Resumes/refreshes also re-evaluate via [republish], so this is a
     * best-effort timer, not the sole expiry mechanism.
     */
    private fun scheduleTrialExpiry() {
        val remaining = (trialStartedAt + TRIAL_DURATION_MS) - System.currentTimeMillis()
        if (remaining <= 0L) {
            republish()
            return
        }
        scope.launch {
            delay(remaining)
            Log.i(TAG, "Expert trial expired; revoking")
            republish()
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
            val price = details.oneTimePurchaseOfferDetails?.formattedPrice
            if (price == null) {
                Log.w(TAG, "productDetails: no one-time price on ${details.productId}")
                _productDetails.tryEmit(
                    ProductDetailsState.Error(-1, "One-time price missing")
                )
                return@launch
            }
            _productDetails.tryEmit(
                ProductDetailsState.Loaded(price)
            )
        }
        return _productDetails.asSharedFlow()
    }

    companion object {
        private const val TAG = "PlayBillingEntitlement"

        /** One-time Expert preview length: one hour. */
        private const val TRIAL_DURATION_MS = 60L * 60L * 1000L
    }
}
