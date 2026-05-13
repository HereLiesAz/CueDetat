package com.hereliesaz.cuedetat.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class EntitlementCacheSerializerTest {

    @Test
    fun serializeRoundTrip_active() {
        val original = Entitlement(
            active = true,
            source = EntitlementSource.PLAY_LOCAL,
            expiresAtMillis = 1_700_000_000_000L,
            productId = "expert_mode",
            lastVerifiedAtMillis = 1_650_000_000_000L
        )
        val map = EntitlementCacheSerializer.toMap(original)
        val restored = EntitlementCacheSerializer.fromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun serializeRoundTrip_none() {
        val original = Entitlement.NONE
        val map = EntitlementCacheSerializer.toMap(original)
        val restored = EntitlementCacheSerializer.fromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun fromMap_emptyMap_returnsNone() {
        val restored = EntitlementCacheSerializer.fromMap(emptyMap())
        assertEquals(Entitlement.NONE, restored)
    }
}
