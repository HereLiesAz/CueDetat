// FILE: app/src/main/java/com/hereliesaz/cuedetat/update/AppUpdater.kt

package com.hereliesaz.cuedetat.update

import android.app.Activity

/**
 * Details of an available update, surfaced to the UI so it can offer a
 * one-tap download + install.
 */
data class UpdateInfo(
    /** The release version, e.g. "1.9.0". */
    val versionName: String,
    /** Direct APK download URL, or null if the release has no APK asset. */
    val apkUrl: String?,
    /** The GitHub release page, used as a fallback when [apkUrl] is null. */
    val releaseUrl: String?,
)

/**
 * In-app update offer. The single build decides at runtime ([GithubAppUpdater]): copies
 * installed by Google Play leave updates to Play; any other install checks GitHub Releases.
 */
interface AppUpdater {

    /** True when this install updates from GitHub (not installed by Play). */
    val isSupported: Boolean get() = false

    /**
     * Returns [UpdateInfo] when a newer release than the running build is
     * available, else null. Always null for Play installs.
     */
    suspend fun checkForUpdate(): UpdateInfo? = null

    /**
     * Hands the update to the browser, which downloads the APK and opens the system installer.
     * Needs an [Activity] to start from. No-op for Play installs.
     */
    suspend fun downloadAndInstall(activity: Activity, info: UpdateInfo) {}
}

/**
 * Compares dotted version strings, ignoring a leading "v" and any non-numeric
 * suffix (e.g. the old FOSS build's "-foss"). Returns true when [latestTag] is
 * strictly newer than [current].
 */
fun isNewerVersion(latestTag: String, current: String): Boolean {
    fun parse(v: String): List<Int> =
        v.trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile { it.isDigit() || it == '.' }
            .split('.')
            .mapNotNull { it.toIntOrNull() }

    val a = parse(latestTag)
    val b = parse(current)
    if (a.isEmpty()) return false
    val n = maxOf(a.size, b.size)
    for (i in 0 until n) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
