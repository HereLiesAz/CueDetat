// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/BillingClientWrapper.kt

package com.hereliesaz.cuedetat.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin wrapper over BillingClient.
 *
 *  - Manages connection with exponential backoff.
 *  - Exposes coroutine-friendly query and purchase APIs.
 *  - Surfaces purchase updates as a SharedFlow so multiple observers can
 *    react (the repository, primarily).
 *
 * This class knows nothing about Expert Mode or Entitlement. Mapping happens
 * in PlayBillingEntitlementRepository.
 */
@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _purchaseUpdates = MutableSharedFlow<List<Purchase>>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val purchaseUpdates: SharedFlow<List<Purchase>> = _purchaseUpdates.asSharedFlow()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        Log.i(
            TAG,
            "purchasesListener responseCode=${result.responseCode} debug='${result.debugMessage}' size=${purchases?.size ?: 0}"
        )
        purchases?.forEach { purchase ->
            // Log each item so a tester can see in logcat exactly what
            // products+states Play just reported. The repository will also
            // record this into its rolling diagnostic buffer.
            Log.i(
                TAG,
                "purchase products=${purchase.products} state=${purchase.purchaseState} " +
                        "ack=${purchase.isAcknowledged} autoRenew=${purchase.isAutoRenewing}"
            )
        }
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchaseUpdates.tryEmit(purchases)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            // USER_CANCELED is benign — the user backed out of the sheet.
            // Anything else is a real problem and the user is otherwise
            // staring at a "nothing happened" paywall. Warn so it is visible
            // in logcat without needing to dig.
            Log.w(
                TAG,
                "purchase did not result in OK; the user probably saw a failed purchase. " +
                        "responseCode=${result.responseCode} debug='${result.debugMessage}'"
            )
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    @Volatile
    private var connecting = false

    suspend fun ensureConnected() {
        if (client.isReady) return
        if (connecting) {
            // Another caller is already connecting; spin until ready or fail.
            var waited = 0L
            while (!client.isReady && waited < 30_000L) {
                delay(100)
                waited += 100
            }
            if (client.isReady) return
        }
        connecting = true
        try {
            connectWithBackoff()
        } finally {
            connecting = false
        }
    }

    private suspend fun connectWithBackoff() {
        var delayMs = 1_000L
        val maxDelayMs = 60_000L
        while (true) {
            val connected = tryConnectOnce()
            if (connected) return
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(maxDelayMs)
        }
    }

    private suspend fun tryConnectOnce(): Boolean = suspendCancellableCoroutine { cont ->
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (cont.isActive) {
                    cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }

            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    suspend fun queryOwnedPurchases(): List<Purchase> {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Throw rather than returning emptyList() — an empty list is
            // indistinguishable from "user owns nothing" and would cause the
            // repository to revoke a paying user's entitlement.
            throw BillingQueryException(
                result.billingResult.responseCode,
                result.billingResult.debugMessage
            )
        }
        Log.i(TAG, "queryOwnedPurchases ok size=${result.purchasesList.size}")
        return result.purchasesList
    }

    suspend fun queryExpertProductDetails(): ProductDetails? {
        ensureConnected()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProductIds.PRODUCT_ID_EXPERT)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }
        return result.productDetailsList?.firstOrNull()
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails
    ): BillingResult {
        // One-time products carry no offer token (that is a subscription concept).
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        return client.launchBillingFlow(activity, params)
    }

    suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) return
        ensureConnected()
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params)
    }

    companion object {
        private const val TAG = "PlayBillingEntitlement"
    }
}
