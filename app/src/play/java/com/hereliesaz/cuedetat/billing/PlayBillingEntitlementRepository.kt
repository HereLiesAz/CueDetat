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
    private val cacheStore: EntitlementCacheStore
) : EntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _entitlement = MutableStateFlow(Entitlement.NONE)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    private val _productDetails = MutableSharedFlow<ProductDetailsState>(replay = 1)

    init {
        scope.launch {
            // 1. Load cache so the first emission is correct for returning users.
            val cached = runCatching { cacheStore.read() }.getOrDefault(Entitlement.NONE)
            _entitlement.value = applyOfflineCap(cached, System.currentTimeMillis())

            // 2. Refresh from Play in the background.
            refresh()

            // 3. React to purchase updates.
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
                _entitlement.value = applyOfflineCap(_entitlement.value, now)
                return
            }
        val snapshots = purchases.flatMap { it.toSnapshots() }
        snapshots.forEach {
            Log.i(TAG, "snapshot productId=${it.productId} state=${it.purchaseState} ack=${it.isAcknowledged}")
        }
        val mapped = PurchaseToEntitlementMapper.map(snapshots, now)
        Log.i(TAG, "refresh result active=${mapped.active} source=${mapped.source}")
        _entitlement.value = mapped
        runCatching { cacheStore.write(mapped) }
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
