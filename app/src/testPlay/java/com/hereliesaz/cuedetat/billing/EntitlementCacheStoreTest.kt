package com.hereliesaz.cuedetat.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [EntitlementCacheStore] itself wraps a Jetpack DataStore<Preferences>,
 * which requires an Android [android.content.Context] and isn't exercisable
 * from a plain JVM unit test (see [TestLicenseAllowlistTest] for the same
 * constraint). All of its actual persistence logic lives in the pure,
 * context-free [EntitlementCacheSerializer] object it delegates to
 * (`toMap`/`fromMap`), so that's what these tests round-trip against —
 * covering the cases [EntitlementCacheSerializerTest] doesn't: every
 * [EntitlementSource], the offline-cap downgrade shape, and malformed input.
 */
class EntitlementCacheStoreTest {

    @Test
    fun roundTrip_testerLicense() {
        val original = Entitlement(
            active = true,
            source = EntitlementSource.TESTER_LICENSE,
            expiresAtMillis = null,
            productId = "expert_mode",
            lastVerifiedAtMillis = 1_650_000_000_000L,
            isDeviceGenuine = true
        )
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun roundTrip_trial_withExpiry() {
        val original = Entitlement(
            active = true,
            source = EntitlementSource.TRIAL,
            expiresAtMillis = 1_700_003_600_000L,
            productId = null,
            lastVerifiedAtMillis = 1_700_000_000_000L,
            isDeviceGenuine = true
        )
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun roundTrip_offlineCached_inactiveAfterCapDowngrade() {
        // Shape written by PlayBillingEntitlementRepository.applyOfflineCapIfStale
        // once a cached entitlement has gone unverified for >14 days.
        val original = Entitlement(
            active = false,
            source = EntitlementSource.OFFLINE_CACHED,
            expiresAtMillis = null,
            productId = "expert_mode",
            lastVerifiedAtMillis = 1_600_000_000_000L,
            isDeviceGenuine = true
        )
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun roundTrip_deviceNotGenuine_persistsFalse() {
        val original = Entitlement(
            active = true,
            source = EntitlementSource.PLAY_LOCAL,
            expiresAtMillis = null,
            productId = "expert_mode",
            lastVerifiedAtMillis = 1_650_000_000_000L,
            isDeviceGenuine = false
        )
        val restored = roundTrip(original)
        assertEquals(original, restored)
        assertFalse(restored.isDeviceGenuine)
    }

    @Test
    fun roundTrip_noLastVerifiedOrProductId_omitsOptionalFields() {
        val original = Entitlement(
            active = false,
            source = EntitlementSource.NONE,
            expiresAtMillis = null,
            productId = null,
            lastVerifiedAtMillis = null,
            isDeviceGenuine = true
        )
        val map = EntitlementCacheSerializer.toMap(original)
        assertFalse(map.containsKey(EntitlementCacheSerializer.KEY_PRODUCT_ID))
        assertFalse(map.containsKey(EntitlementCacheSerializer.KEY_VERIFIED_AT))
        assertFalse(map.containsKey(EntitlementCacheSerializer.KEY_EXPIRES_AT))
        val restored = EntitlementCacheSerializer.fromMap(map)
        assertEquals(original, restored)
        assertNull(restored.lastVerifiedAtMillis)
        assertNull(restored.productId)
    }

    @Test
    fun fromMap_unknownSourceName_fallsBackToNone() {
        // A future app version could add/rename an EntitlementSource; an
        // older cache entry (or corrupted DataStore) with an unrecognized
        // name must not crash — it degrades to NONE rather than propagating
        // a bogus "active" state.
        val map = mapOf(
            EntitlementCacheSerializer.KEY_ACTIVE to true,
            EntitlementCacheSerializer.KEY_SOURCE to "SOME_FUTURE_SOURCE_NOT_YET_KNOWN",
        )
        val restored = EntitlementCacheSerializer.fromMap(map)
        assertEquals(EntitlementSource.NONE, restored.source)
    }

    @Test
    fun fromMap_missingActiveKey_returnsNone() {
        val map = mapOf(EntitlementCacheSerializer.KEY_SOURCE to EntitlementSource.PLAY_LOCAL.name)
        assertEquals(Entitlement.NONE, EntitlementCacheSerializer.fromMap(map))
    }

    @Test
    fun fromMap_missingSourceKey_returnsNone() {
        val map = mapOf(EntitlementCacheSerializer.KEY_ACTIVE to true)
        assertEquals(Entitlement.NONE, EntitlementCacheSerializer.fromMap(map))
    }

    @Test
    fun fromMap_missingGenuineKey_defaultsTrue() {
        val map = mapOf(
            EntitlementCacheSerializer.KEY_ACTIVE to true,
            EntitlementCacheSerializer.KEY_SOURCE to EntitlementSource.PLAY_LOCAL.name,
        )
        val restored = EntitlementCacheSerializer.fromMap(map)
        assertEquals(true, restored.isDeviceGenuine)
    }

    private fun roundTrip(entitlement: Entitlement): Entitlement =
        EntitlementCacheSerializer.fromMap(EntitlementCacheSerializer.toMap(entitlement))
}
