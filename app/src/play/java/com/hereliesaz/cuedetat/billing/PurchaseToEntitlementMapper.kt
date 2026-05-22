// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/PurchaseToEntitlementMapper.kt

package com.hereliesaz.cuedetat.billing

import com.android.billingclient.api.Purchase

/**
 * A minimal projection of com.android.billingclient.api.Purchase that we can
 * fake in unit tests. The Play Billing Library's Purchase class is final and
 * has no public constructor, so we cannot instantiate it directly in tests.
 * Production code adapts the real Purchase to PurchaseSnapshot below.
 */
interface PurchaseSnapshot {
    val productId: String
    val purchaseState: Int
    val isAcknowledged: Boolean
    val isAutoRenewing: Boolean

    companion object {
        const val STATE_PURCHASED = 1
        const val STATE_PENDING = 2
    }
}

/**
 * Adapter from the SDK's Purchase to our test-friendly snapshot. Each Purchase
 * may contain multiple products; we expand to one snapshot per product.
 */
fun Purchase.toSnapshots(): List<PurchaseSnapshot> = products.map { productIdValue ->
    val src = this
    object : PurchaseSnapshot {
        override val productId = productIdValue
        override val purchaseState = src.purchaseState
        override val isAcknowledged = src.isAcknowledged
        override val isAutoRenewing = src.isAutoRenewing
    }
}

/**
 * Pure mapping from a list of purchase snapshots to an Entitlement.
 * We treat the user as entitled if there is any active subscription to
 * PRODUCT_ID_EXPERT. Pending purchases do NOT grant entitlement.
 *
 * Acknowledgement state does not gate entitlement: Google has already
 * accepted the payment by the time we see PURCHASED. The acknowledge step
 * only prevents auto-refund.
 */
object PurchaseToEntitlementMapper {

    fun map(purchases: List<PurchaseSnapshot>, nowMillis: Long): Entitlement {
        val expert = purchases.firstOrNull { snapshot ->
            snapshot.productId == BillingProductIds.PRODUCT_ID_EXPERT &&
                    snapshot.purchaseState == PurchaseSnapshot.STATE_PURCHASED
        }
        return if (expert != null) {
            Entitlement(
                active = true,
                source = EntitlementSource.PLAY_LOCAL,
                expiresAtMillis = null,
                productId = expert.productId,
                lastVerifiedAtMillis = nowMillis,
                isDeviceGenuine = true // Default to true here; PlayBillingEntitlementRepository will update it
            )
        } else {
            Entitlement.NONE.copy(lastVerifiedAtMillis = nowMillis)
        }
    }
}
