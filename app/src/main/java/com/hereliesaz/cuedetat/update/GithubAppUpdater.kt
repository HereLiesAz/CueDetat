package com.hereliesaz.cuedetat.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.data.GithubRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Updates from GitHub Releases, for copies not installed by Google Play.
 *
 * One build ships to both channels, so the channel is decided at runtime from the installer
 * of record. Play installs get nothing from here ([isSupported] false): Play owns their updates,
 * and Play policy forbids an app it distributes from updating itself any other way.
 *
 * Everything else checks the latest GitHub release and, when it is newer, offers it. The update
 * opens in the browser, which downloads the APK and hands it to the system installer. The app
 * itself never installs packages, so it needs no `REQUEST_INSTALL_PACKAGES` permission (which
 * Play restricts to apps whose core purpose is installing packages).
 */
@Singleton
class GithubAppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubRepository: GithubRepository,
) : AppUpdater {

    override val isSupported: Boolean by lazy { !installedFromPlay() }

    override suspend fun checkForUpdate(): UpdateInfo? {
        if (!isSupported) return null
        val release = githubRepository.getLatestRelease() ?: return null
        val tag = release.tag_name
        if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) return null
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return UpdateInfo(
            versionName = tag.removePrefix("v").removePrefix("V"),
            apkUrl = apk?.browser_download_url,
            releaseUrl = release.html_url,
        )
    }

    /** Opens the APK download (or, failing that, the release page) in the browser. */
    override suspend fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        val url = info.apkUrl?.takeIf { it.isNotBlank() } ?: info.releaseUrl ?: return
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { Log.e(TAG, "No app to open $url", it) }
    }

    private fun installedFromPlay(): Boolean = runCatching {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        installer == PLAY_STORE
    }.getOrDefault(false)

    private companion object {
        const val TAG = "GithubAppUpdater"
        const val PLAY_STORE = "com.android.vending"
    }
}
