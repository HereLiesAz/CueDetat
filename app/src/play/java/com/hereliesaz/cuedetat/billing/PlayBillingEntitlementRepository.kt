// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/PlayBillingEntitlementRepository.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
) : EntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Last computed Play entitlement. Combined with the tester license to
     *  produce the public `entitlement` value. */
    private val playEntitlement = MutableStateFlow(Entitlement.NONE)

    /** Currently-verified tester hash, or null when no tester license. */
    @Volatile
    private var verifiedTesterHash: String? = null

    private val _entitlement = MutableStateFlow(Entitlement.NONE)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    private val _productDetails = MutableSharedFlow<ProductDetailsState>(replay = 1)

    init {
        scope.launch {
            // 1. Load cache so the first emission is correct for returning users.
            val cached = runCatching { cacheStore.read() }.getOrDefault(Entitlement.NONE)
            playEntitlement.value = applyOfflineCap(cached, System.currentTimeMillis())
            republish()

            // 2. Restore any previously-verified tester license. If the
            //    allowlist no longer contains the stored hash (group membership
            //    changed between builds) we drop it on the floor and clear it.
            testLicenseStore.verifiedHash.collect { hash ->
                verifiedTesterHash = hash?.takeIf(testLicenseAllowlist::containsHash)
                if (hash != null && verifiedTesterHash == null) {
                    runCatching { testLicenseStore.clear() }
                }
                republish()
            }
        }
        scope.launch {
            // 3. Refresh from Play in the background.
            refresh()

            // 4. React to purchase updates.
            billingClient.purchaseUpdates.collect { purchases ->
                purchases.forEach { runCatching { billingClient.acknowledgeIfNeeded(it) } }
                refresh()
            }
        }

        // 4. Re-verify on every process foreground. Recovers from a transient
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
            _productDetails.tryEmit(ProductDetailsState.Error(-1, "Product details unavailable"))
            return
        }
        val offer = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == basePlan.tag }
            ?: run {
                _productDetails.tryEmit(
                    ProductDetailsState.Error(-1, "Base plan ${basePlan.tag} not configured")
                )
                return
            }
        billingClient.launchBillingFlow(activity, productDetails, offer.offerToken)
    }

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        val purchases = runCatching { billingClient.queryActiveSubscriptions() }
            .getOrElse {
                // Transient failure: keep the previous entitlement (subject to
                // the 14-day offline cap) and do NOT overwrite the cache.
                Log.w(TAG, "queryActiveSubscriptions failed; preserving cached entitlement", it)
                playEntitlement.value = applyOfflineCap(playEntitlement.value, now)
                republish()
                return
            }
        val snapshots = purchases.flatMap { it.toSnapshots() }
        snapshots.forEach {
            Log.i(TAG, "snapshot productId=${it.productId} state=${it.purchaseState} ack=${it.isAcknowledged}")
        }
        val mapped = PurchaseToEntitlementMapper.map(snapshots, now)
        Log.i(TAG, "refresh result active=${mapped.active} source=${mapped.source}")
        playEntitlement.value = mapped
        runCatching { cacheStore.write(mapped) }
        republish()
    }

    /**
     * Attempt to grant a tester license. Returns true if `email` matched the
     * baked-in allowlist; the verified hash is persisted so future launches
     * stay entitled without re-entry.
     */
    suspend fun tryApplyTesterLicense(email: String): Boolean {
        if (!testLicenseAllowlist.isAllowed(email)) return false
        val hash = TestLicenseAllowlist.sha256Hex(email.trim().lowercase())
        testLicenseStore.setVerifiedHash(hash)
        // The store collector will pick up the change and call republish(),
        // but apply immediately so callers see the updated state on return.
        verifiedTesterHash = hash
        republish()
        return true
    }

    /** Revoke the tester license stored on this device. */
    suspend fun clearTesterLicense() {
        testLicenseStore.clear()
        verifiedTesterHash = null
        republish()
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
        _entitlement.value = if (tester != null) {
            Entitlement(
                active = true,
                source = EntitlementSource.TESTER_LICENSE,
                expiresAtMillis = null,
                productId = play.productId,
                lastVerifiedAtMillis = System.currentTimeMillis(),
            )
        } else {
            play
        }
    }

    override fun productDetails(): Flow<ProductDetailsState> {
        scope.launch {
            _productDetails.tryEmit(ProductDetailsState.Loading)
            val details = runCatching { billingClient.queryExpertProductDetails() }
                .getOrNull()
            if (details == null) {
                _productDetails.tryEmit(
                    ProductDetailsState.Error(-1, "Product details query failed")
                )
                return@launch
            }
            val monthly = details.findFormattedPrice(BasePlanId.MONTHLY)
            val yearly = details.findFormattedPrice(BasePlanId.YEARLY)
            val trialDays = details.findTrialDays()
            if (monthly == null || yearly == null) {
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
