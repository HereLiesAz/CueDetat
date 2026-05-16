package com.hereliesaz.cuedetat.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for the transient-failure path in
 * PlayBillingEntitlementRepository.refresh: when queryActiveSubscriptions
 * throws, the repository feeds the previous entitlement through
 * applyOfflineCap rather than overwriting it with Entitlement.NONE.
 *
 * If applyOfflineCap ever clobbers an active entitlement before the 14-day
 * cap, a single network blip on app start would silently revoke a paying
 * user's Expert Mode access until the next successful refresh. These tests
 * pin that behavior down.
 */
class OfflineCapTest {

    private val now = 1_700_000_000_000L

    @Test
    fun activeEntitlement_recentlyVerified_isPreserved() {
        val active = Entitlement(
            active = true,
            source = EntitlementSource.PLAY_LOCAL,
            expiresAtMillis = null,
            productId = BillingProductIds.PRODUCT_ID_EXPERT,
            lastVerifiedAtMillis = now - (60L * 1000L) // 1 minute ago
        )

        val capped = applyOfflineCap(active, now)

        assertTrue("Active entitlement must survive transient failure", capped.active)
        assertEquals(EntitlementSource.OFFLINE_CACHED, capped.source)
        assertEquals(BillingProductIds.PRODUCT_ID_EXPERT, capped.productId)
    }

    @Test
    fun activeEntitlement_olderThanCap_isRevoked() {
        val stale = Entitlement(
            active = true,
            source = EntitlementSource.PLAY_LOCAL,
            expiresAtMillis = null,
            productId = BillingProductIds.PRODUCT_ID_EXPERT,
            lastVerifiedAtMillis = now - (OFFLINE_CAP_MILLIS + 1L)
        )

        val capped = applyOfflineCap(stale, now)

        assertFalse("Entitlement past 14-day cap must be revoked", capped.active)
    }

    @Test
    fun noneEntitlement_withNoLastVerified_isReturnedUnchanged() {
        val capped = applyOfflineCap(Entitlement.NONE, now)
        assertEquals(Entitlement.NONE, capped)
    }

    @Test
    fun activeEntitlement_alreadyOfflineCached_isNotResourcedAgain() {
        val cached = Entitlement(
            active = true,
            source = EntitlementSource.OFFLINE_CACHED,
            expiresAtMillis = null,
            productId = BillingProductIds.PRODUCT_ID_EXPERT,
            lastVerifiedAtMillis = now - (60L * 1000L)
        )

        val capped = applyOfflineCap(cached, now)

        assertEquals(EntitlementSource.OFFLINE_CACHED, capped.source)
        assertTrue(capped.active)
    }
}
