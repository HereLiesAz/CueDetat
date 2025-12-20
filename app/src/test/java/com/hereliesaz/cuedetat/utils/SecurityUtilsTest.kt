package com.hereliesaz.cuedetat.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `isSafeUrl returns true for https urls`() {
        assertTrue(SecurityUtils.isSafeUrl("https://github.com/hereliesaz/cuedetat"))
    }

    @Test
    fun `isSafeUrl returns true for http urls`() {
        assertTrue(SecurityUtils.isSafeUrl("http://example.com"))
    }

    @Test
    fun `isSafeUrl returns false for file urls`() {
        assertFalse(SecurityUtils.isSafeUrl("file:///etc/hosts"))
    }

    @Test
    fun `isSafeUrl returns false for javascript urls`() {
        assertFalse(SecurityUtils.isSafeUrl("javascript:alert(1)"))
    }

    @Test
    fun `isSafeUrl returns false for other schemes`() {
        assertFalse(SecurityUtils.isSafeUrl("content://com.android.providers.media.documents/document/image%3A123"))
    }
}
