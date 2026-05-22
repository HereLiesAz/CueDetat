package com.hereliesaz.cuedetat.billing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hashed allowlist of tester emails baked into the play-release APK.
 *
 * The list is sourced from a Google Group (link in `GG_LINK` repo secret) and
 * scraped by a gradle task during the `assemblePlayRelease` pipeline. Only the
 * SHA-256 hex digest of each lowercased email ships in the APK; the plain
 * emails never leave CI.
 *
 * Local builds and play-debug builds get an empty list — the allowlist file
 * isn't generated unless `assemblePlayRelease` runs with the secrets present.
 */
@Singleton
class TestLicenseAllowlist @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val hashes: Set<String> by lazy { loadHashes() }

    /** Returns true when `email`'s normalized SHA-256 hex matches the list. */
    fun isAllowed(email: String): Boolean {
        if (hashes.isEmpty()) return false
        val normalized = email.trim().lowercase()
        if (normalized.isEmpty()) return false
        return containsHash(sha256Hex(normalized))
    }

    /** Returns true when the given SHA-256 hex is in the allowlist. Useful for
     *  re-validating a previously-accepted hash from persistent storage when
     *  the allowlist may have changed between builds. */
    fun containsHash(sha256Hex: String): Boolean {
        if (hashes.isEmpty()) return false
        return sha256Hex.lowercase() in hashes
    }

    /** True when this build was assembled with at least one tester email. */
    val isConfigured: Boolean
        get() = hashes.isNotEmpty()

    private fun loadHashes(): Set<String> {
        return runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
                reader.readText()
                    .lineSequence()
                    .map { it.trim().lowercase() }
                    .filter { it.length == 64 && it.all { ch -> ch in HEX_ALPHABET } }
                    .toHashSet()
            }
        }.getOrDefault(emptySet())
    }

    companion object {
        internal const val ASSET_NAME = "tester_emails.txt"
        private const val HEX_ALPHABET = "0123456789abcdef"

        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return buildString(digest.size * 2) {
                for (byte in digest) {
                    val v = byte.toInt() and 0xFF
                    append(HEX_ALPHABET[v ushr 4])
                    append(HEX_ALPHABET[v and 0x0F])
                }
            }
        }
    }
}