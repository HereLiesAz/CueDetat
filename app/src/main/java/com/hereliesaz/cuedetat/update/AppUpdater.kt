// FILE: app/src/main/java/com/hereliesaz/cuedetat/update/AppUpdater.kt

package com.hereliesaz.cuedetat.update

import android.app.Activity

/**
 * Details of an available update, surfaced to the UI so it can offer a
 * one-tap download + install.
 */
/** The install channel that owns this application's updates. */
enum class UpdateSource { GOOGLE_PLAY, GITHUB }

/** A Play-installed copy checks Play; package-installer/sideloaded copies use GitHub. */
fun updateSourceForInstaller(installerPackageName: String?): UpdateSource =
    if (installerPackageName == "com.android.vending") UpdateSource.GOOGLE_PLAY
    else UpdateSource.GITHUB

data class UpdateInfo(
    /** The release version on GitHub, or Play's available version code. */
    val versionName: String,
    /** Direct APK download URL, or null for Google Play. */
    val apkUrl: String?,
    /** GitHub release page fallback; null for Google Play. */
    val releaseUrl: String?,
    /** Always matches the source where the running app was installed. */
    val source: UpdateSource = UpdateSource.GITHUB,
)

/**
 * In-app update notice. The single build selects Google Play for Play-installed
 * copies and GitHub Releases for other installs, based on the recorded installer.
 */
interface AppUpdater {

    /** Both distribution channels can show update notices. */
    val isSupported: Boolean get() = true

    /** Returns source-specific update information when available, otherwise null. */
    suspend fun checkForUpdate(): UpdateInfo?

    /** Opens the original distribution channel to let the user update. */
    suspend fun downloadAndInstall(activity: Activity, info: UpdateInfo)
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
