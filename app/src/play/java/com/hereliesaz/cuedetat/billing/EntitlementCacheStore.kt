// FILE: app/src/play/java/com/hereliesaz/cuedetat/billing/EntitlementCacheStore.kt

package com.hereliesaz.cuedetat.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.billingCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "billing_cache"
)

/**
 * Persisted last-known Entitlement state. Read at startup so the first emission
 * isn't NONE for a paying user with a slow Play connection.
 */
@Singleton
class EntitlementCacheStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val store = context.billingCacheDataStore

    suspend fun read(): Entitlement {
        val prefs = store.data.first()
        return EntitlementCacheSerializer.fromMap(prefs.toAttributeMap())
    }

    fun observe(): Flow<Entitlement> = store.data.map { prefs ->
        EntitlementCacheSerializer.fromMap(prefs.toAttributeMap())
    }

    suspend fun write(entitlement: Entitlement) {
        store.edit { prefs ->
            val map = EntitlementCacheSerializer.toMap(entitlement)
            (map[EntitlementCacheSerializer.KEY_ACTIVE] as? Boolean)?.let { prefs[KEY_ACTIVE] = it }
                ?: prefs.remove(KEY_ACTIVE)
            (map[EntitlementCacheSerializer.KEY_SOURCE] as? String)?.let { prefs[KEY_SOURCE] = it }
                ?: prefs.remove(KEY_SOURCE)
            (map[EntitlementCacheSerializer.KEY_EXPIRES_AT] as? Long)?.let { prefs[KEY_EXPIRES_AT] = it }
                ?: prefs.remove(KEY_EXPIRES_AT)
            (map[EntitlementCacheSerializer.KEY_PRODUCT_ID] as? String)?.let { prefs[KEY_PRODUCT_ID] = it }
                ?: prefs.remove(KEY_PRODUCT_ID)
            (map[EntitlementCacheSerializer.KEY_VERIFIED_AT] as? Long)?.let { prefs[KEY_VERIFIED_AT] = it }
                ?: prefs.remove(KEY_VERIFIED_AT)
            (map[EntitlementCacheSerializer.KEY_GENUINE] as? Boolean)?.let { prefs[KEY_GENUINE] = it }
                ?: prefs.remove(KEY_GENUINE)
        }
    }

    private fun Preferences.toAttributeMap(): Map<String, Any> = buildMap {
        this@toAttributeMap[KEY_ACTIVE]?.let { put(EntitlementCacheSerializer.KEY_ACTIVE, it) }
        this@toAttributeMap[KEY_SOURCE]?.let { put(EntitlementCacheSerializer.KEY_SOURCE, it) }
        this@toAttributeMap[KEY_EXPIRES_AT]?.let { put(EntitlementCacheSerializer.KEY_EXPIRES_AT, it) }
        this@toAttributeMap[KEY_PRODUCT_ID]?.let { put(EntitlementCacheSerializer.KEY_PRODUCT_ID, it) }
        this@toAttributeMap[KEY_VERIFIED_AT]?.let { put(EntitlementCacheSerializer.KEY_VERIFIED_AT, it) }
        this@toAttributeMap[KEY_GENUINE]?.let { put(EntitlementCacheSerializer.KEY_GENUINE, it) }
    }

    companion object {
        private val KEY_ACTIVE = booleanPreferencesKey(EntitlementCacheSerializer.KEY_ACTIVE)
        private val KEY_SOURCE = stringPreferencesKey(EntitlementCacheSerializer.KEY_SOURCE)
        private val KEY_EXPIRES_AT = longPreferencesKey(EntitlementCacheSerializer.KEY_EXPIRES_AT)
        private val KEY_PRODUCT_ID = stringPreferencesKey(EntitlementCacheSerializer.KEY_PRODUCT_ID)
        private val KEY_VERIFIED_AT = longPreferencesKey(EntitlementCacheSerializer.KEY_VERIFIED_AT)
        private val KEY_GENUINE = booleanPreferencesKey(EntitlementCacheSerializer.KEY_GENUINE)
    }
}

/**
 * Pure serialization helpers. Pulled out so they can be unit-tested without
 * needing an Android Context.
 */
object EntitlementCacheSerializer {

    const val KEY_ACTIVE = "entitlement_active"
    const val KEY_SOURCE = "entitlement_source"
    const val KEY_EXPIRES_AT = "entitlement_expires_at"
    const val KEY_PRODUCT_ID = "entitlement_product_id"
    const val KEY_VERIFIED_AT = "entitlement_verified_at"
    const val KEY_GENUINE = "entitlement_genuine"

    fun toMap(entitlement: Entitlement): Map<String, Any> = buildMap {
        put(KEY_ACTIVE, entitlement.active)
        put(KEY_SOURCE, entitlement.source.name)
        entitlement.expiresAtMillis?.let { put(KEY_EXPIRES_AT, it) }
        entitlement.productId?.let { put(KEY_PRODUCT_ID, it) }
        entitlement.lastVerifiedAtMillis?.let { put(KEY_VERIFIED_AT, it) }
        put(KEY_GENUINE, entitlement.isDeviceGenuine)
    }

    fun fromMap(map: Map<String, Any?>): Entitlement {
        if (map.isEmpty()) return Entitlement.NONE
        val active = map[KEY_ACTIVE] as? Boolean ?: return Entitlement.NONE
        val sourceName = map[KEY_SOURCE] as? String ?: return Entitlement.NONE
        val source = runCatching { EntitlementSource.valueOf(sourceName) }
            .getOrDefault(EntitlementSource.NONE)
        return Entitlement(
            active = active,
            source = source,
            expiresAtMillis = map[KEY_EXPIRES_AT] as? Long,
            productId = map[KEY_PRODUCT_ID] as? String,
            lastVerifiedAtMillis = map[KEY_VERIFIED_AT] as? Long,
            isDeviceGenuine = map[KEY_GENUINE] as? Boolean ?: true
        )
    }
}
