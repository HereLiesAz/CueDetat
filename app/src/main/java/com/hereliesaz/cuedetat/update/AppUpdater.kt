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
 * Flavor-specific self-update.
 *
 *  - **foss**: checks GitHub releases and can sideload the new APK via the
 *    system installer (the only kind of "auto-update" Android allows a
 *    normally-installed app).
 *  - **play**: no-op. Google Play owns updates for store installs, and
 *    self-updating APKs violate Play policy.
 */
interface AppUpdater {

    /** True only in flavors that can self-update (FOSS). */
    val isSupported: Boolean get() = false

    /**
     * Returns [UpdateInfo] when a newer release than the running build is
     * available, else null. No-op (null) in the Play flavor.
     */
    suspend fun checkForUpdate(): UpdateInfo? = null

    /**
     * Download the release APK and launch the system installer. No-op in Play.
     * Must be called with an [Activity] so the install prompt and any
     * "allow from this source" settings redirect have a UI host.
     */
    suspend fun downloadAndInstall(activity: Activity, info: UpdateInfo) {}
}

/**
 * Compares dotted version strings, ignoring a leading "v" and any non-numeric
 * suffix (e.g. the FOSS build's "-foss"). Returns true when [latestTag] is
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
