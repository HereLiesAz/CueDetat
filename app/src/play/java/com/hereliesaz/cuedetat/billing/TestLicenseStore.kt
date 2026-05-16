package com.hereliesaz.cuedetat.billing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists which tester email the user has verified (only the SHA-256 hex of
 * the normalized email; plain emails never touch disk). Scoped to play flavor;
 * the foss flavor is permanently entitled and has no use for this.
 *
 * Cleared on app uninstall or "Clear data". Surviving "Clear cache" is
 * intentional — testers shouldn't have to re-enter their email after a routine
 * cache clear.
 */
@Singleton
class TestLicenseStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val verifiedHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_VERIFIED_HASH]?.takeIf { it.isNotBlank() }
    }

    suspend fun setVerifiedHash(hashHex: String?) {
        context.dataStore.edit { prefs ->
            if (hashHex.isNullOrBlank()) {
                prefs.remove(KEY_VERIFIED_HASH)
            } else {
                prefs[KEY_VERIFIED_HASH] = hashHex
            }
        }
    }

    suspend fun clear() = setVerifiedHash(null)

    companion object {
        private val Context.dataStore by preferencesDataStore("tester_license")
        private val KEY_VERIFIED_HASH = stringPreferencesKey("verified_hash")
    }
}
