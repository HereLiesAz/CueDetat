package com.hereliesaz.cuedetat.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the distribution boundary: Play installs never follow GitHub update links.
 *
 * Android's installer of record for a downloaded APK can be null or the system
 * package installer, not GitHub itself. Those sideloads intentionally use GitHub.
 */
class UpdateSourceTest {

    @Test
    fun playInstalledCopiesUseGooglePlay() {
        assertEquals(
            UpdateSource.GOOGLE_PLAY,
            updateSourceForInstaller("com.android.vending")
        )
    }

    @Test
    fun githubDownloadsAndOtherSideloadsUseGithub() {
        assertEquals(UpdateSource.GITHUB, updateSourceForInstaller(null))
        assertEquals(
            UpdateSource.GITHUB,
            updateSourceForInstaller("com.google.android.packageinstaller")
        )
        assertEquals(
            UpdateSource.GITHUB,
            updateSourceForInstaller("com.android.packageinstaller")
        )
    }

    @Test
    fun githubReleaseComparisonRequiresNewerVersion() {
        assertTrue(isNewerVersion("v1.10.29.601", "1.10.28.566"))
        assertFalse(isNewerVersion("v1.10.28.566", "1.10.28.566"))
        assertFalse(isNewerVersion("v1.10.27.565", "1.10.28.566"))
    }
}
