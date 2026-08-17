package com.hereliesaz.cuedetat.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseToEntitlementMapperTest {

    @Test
    fun emptyList_returnsNone() {
        val result = PurchaseToEntitlementMapper.map(
            purchases = emptyList(),
            nowMillis = 1_700_000_000_000L
        )
        assertEquals(Entitlement.NONE.copy(lastVerifiedAtMillis = 1_700_000_000_000L), result)
    }

    @Test
    fun activePurchaseAcknowledged_returnsActive() {
        val result = PurchaseToEntitlementMapper.map(
            purchases = listOf(
                FakePurchaseSnapshot(
                    productId = BillingProductIds.PRODUCT_ID_EXPERT,
                    purchaseState = PurchaseSnapshot.STATE_PURCHASED,
                    isAcknowledged = true,
                    isAutoRenewing = true
                )
            ),
            nowMillis = 1_700_000_000_000L
        )
        assertTrue(result.active)
        assertEquals(EntitlementSource.PLAY_LOCAL, result.source)
        assertEquals(BillingProductIds.PRODUCT_ID_EXPERT, result.productId)
    }

    @Test
    fun activePurchaseUnacknowledged_stillReturnsActive() {
        // Acknowledgement state must not gate entitlement: Google has already
        // accepted the payment by the time purchaseState == PURCHASED. The
        // acknowledge step only prevents Google's 3-day auto-refund; it is
        // handled separately by BillingClientWrapper.acknowledgeIfNeeded().
        val result = PurchaseToEntitlementMapper.map(
            purchases = listOf(
                FakePurchaseSnapshot(
                    productId = BillingProductIds.PRODUCT_ID_EXPERT,
                    purchaseState = PurchaseSnapshot.STATE_PURCHASED,
                    isAcknowledged = false,
                    isAutoRenewing = true
                )
            ),
            nowMillis = 1_700_000_000_000L
        )
        assertTrue(result.active)
        assertEquals(EntitlementSource.PLAY_LOCAL, result.source)
    }

    @Test
    fun pendingPurchase_returnsNotActive() {
        val result = PurchaseToEntitlementMapper.map(
            purchases = listOf(
                FakePurchaseSnapshot(
                    productId = BillingProductIds.PRODUCT_ID_EXPERT,
                    purchaseState = PurchaseSnapshot.STATE_PENDING,
                    isAcknowledged = false,
                    isAutoRenewing = false
                )
            ),
            nowMillis = 1_700_000_000_000L
        )
        assertFalse(result.active)
    }

    @Test
    fun unknownProduct_isIgnored() {
        val result = PurchaseToEntitlementMapper.map(
            purchases = listOf(
                FakePurchaseSnapshot(
                    productId = "some_other_product",
                    purchaseState = PurchaseSnapshot.STATE_PURCHASED,
                    isAcknowledged = true,
                    isAutoRenewing = true
                )
            ),
            nowMillis = 1_700_000_000_000L
        )
        assertFalse(result.active)
    }

    private data class FakePurchaseSnapshot(
        override val productId: String,
        override val purchaseState: Int,
        override val isAcknowledged: Boolean,
        override val isAutoRenewing: Boolean
    ) : PurchaseSnapshot
}
