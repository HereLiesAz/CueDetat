package com.hereliesaz.cuedetat.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArCoreStatusTest {
    @Test fun `only a missing or outdated ARCore can be fixed by asking Google Play`() {
        assertTrue(ArCoreStatus.NOT_INSTALLED.needsInstall)
        assertTrue(ArCoreStatus.TOO_OLD.needsInstall)
    }

    @Test fun `an installed, unsupported or undetermined ARCore is not offered an install`() {
        assertFalse(ArCoreStatus.READY.needsInstall)
        assertFalse(ArCoreStatus.UNSUPPORTED.needsInstall)
        assertFalse(ArCoreStatus.UNKNOWN.needsInstall)
    }
}
