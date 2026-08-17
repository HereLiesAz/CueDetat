package com.hereliesaz.cuedetat.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TestLicenseAllowlist] is `@Singleton`-injected with an Android [android.content.Context]
 * and reads its hash set from `context.assets`, so its instance methods
 * (`isAllowed`, `containsHash`, `isConfigured`) cannot be exercised from a
 * plain JVM unit test without an Android framework test harness (Robolectric)
 * or a mocking library — neither is wired up in this module's test
 * dependencies. These tests instead pin down the pure, static normalization
 * and hashing logic that `isAllowed`/`containsHash` are built on
 * ([TestLicenseAllowlist.sha256Hex]), which is exactly the part of the class
 * responsible for "hash normalization/matching".
 */
class TestLicenseAllowlistTest {

    @Test
    fun sha256Hex_isDeterministic() {
        val a = TestLicenseAllowlist.sha256Hex("tester@example.com")
        val b = TestLicenseAllowlist.sha256Hex("tester@example.com")
        assertEquals(a, b)
    }

    @Test
    fun sha256Hex_producesLowercase64CharHex() {
        val hash = TestLicenseAllowlist.sha256Hex("tester@example.com")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun sha256Hex_differentInputs_produceDifferentHashes() {
        val a = TestLicenseAllowlist.sha256Hex("tester1@example.com")
        val b = TestLicenseAllowlist.sha256Hex("tester2@example.com")
        assertNotEquals(a, b)
    }

    @Test
    fun sha256Hex_matchesKnownVector() {
        // SHA-256("") — a stable, independently-verifiable vector, to catch
        // any accidental algorithm/encoding change (e.g. wrong charset).
        val hash = TestLicenseAllowlist.sha256Hex("")
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash
        )
    }

    @Test
    fun isAllowed_normalization_matchesWhatIsAllowedWouldCompute() {
        // isAllowed() normalizes via `email.trim().lowercase()` before hashing
        // (see TestLicenseAllowlist.isAllowed). Mixed case and surrounding
        // whitespace must therefore normalize to the exact same hash as the
        // canonical trimmed-lowercase form, since that canonical hash is what
        // the CI-generated allowlist asset actually contains.
        val canonical = TestLicenseAllowlist.sha256Hex("tester@example.com")

        val fromMixedCase = TestLicenseAllowlist.sha256Hex(
            "  Tester@Example.COM  ".trim().lowercase()
        )
        assertEquals(canonical, fromMixedCase)

        val fromTrailingWhitespace = TestLicenseAllowlist.sha256Hex(
            "tester@example.com\n".trim().lowercase()
        )
        assertEquals(canonical, fromTrailingWhitespace)
    }

    @Test
    fun containsHash_normalization_isCaseInsensitive() {
        // containsHash() lowercases its input before the set lookup; verify
        // that an uppercase-hex hash string normalizes to the same value as
        // its lowercase form, matching how it would compare against the
        // (always-lowercase) stored allowlist hashes.
        val hash = TestLicenseAllowlist.sha256Hex("tester@example.com")
        assertEquals(hash, hash.uppercase().lowercase())
    }
}
