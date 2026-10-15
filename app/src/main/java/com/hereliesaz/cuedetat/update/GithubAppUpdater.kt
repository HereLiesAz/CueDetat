package com.hereliesaz.cuedetat.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.data.GithubRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes update notices to the installation source without mixing distribution channels.
 *
 * One signed package ships to both Google Play and GitHub. The package manager identifies
 * Google Play by its installer package. Sideloads, including GitHub APKs, normally report
 * the system package installer or no installer, so they use GitHub Releases.
 *
 * Play installs use the Play In-App Updates availability API to check the release visible
 * to that account/track, but the user chooses to install through the Play Store app.
 * GitHub installs compare GitHub release tags and open the APK/release in the browser.
 *
 * This app never installs packages directly and needs no REQUEST_INSTALL_PACKAGES permission.
 */
@Singleton
class GithubAppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubRepository: GithubRepository,
) : AppUpdater {

    override val isSupported: Boolean = true

    override suspend fun checkForUpdate(): UpdateInfo? = when (installationSource()) {
        UpdateSource.GOOGLE_PLAY -> checkGooglePlay()
        UpdateSource.GITHUB -> checkGithub()
    }

    /** Play's update status is account/track-specific; GitHub tags must not be used here. */
    private suspend fun checkGooglePlay(): UpdateInfo? {
        val update = try {
            AppUpdateManagerFactory.create(context).appUpdateInfo.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Google Play update check failed", e)
            return null
        }
        if (update.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ||
            update.availableVersionCode() <= BuildConfig.VERSION_CODE
        ) return null

        return UpdateInfo(
            versionName = update.availableVersionCode().toString(),
            apkUrl = null,
            releaseUrl = null,
            source = UpdateSource.GOOGLE_PLAY,
        )
    }

    /** Non-Play installs check only the public GitHub Releases endpoint. */
    private suspend fun checkGithub(): UpdateInfo? {
        val release = githubRepository.getLatestRelease() ?: return null
        val tag = release.tag_name
        if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) return null
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return UpdateInfo(
            versionName = tag.removePrefix("v").removePrefix("V"),
            apkUrl = apk?.browser_download_url,
            releaseUrl = release.html_url,
            source = UpdateSource.GITHUB,
        )
    }

    /** Source-checked route: never offer a GitHub APK to a Play-installed copy. */
    override suspend fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        if (info.source != installationSource()) {
            Log.w(TAG, "Refusing update from a different install source: ${info.source}")
            return
        }
        when (info.source) {
            UpdateSource.GOOGLE_PLAY -> openGooglePlay(activity)
            UpdateSource.GITHUB -> {
                val url = info.apkUrl?.takeIf { it.isNotBlank() }
                    ?: info.releaseUrl?.takeIf { it.isNotBlank() }
                    ?: return
                runCatching {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure { Log.e(TAG, "No app to open $url", it) }
            }
        }
    }

    private fun openGooglePlay(activity: Activity) {
        val marketUri = Uri.parse("market://details?id=${context.packageName}")
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, marketUri).setPackage(PLAY_STORE)
            )
        } catch (e: ActivityNotFoundException) {
            openPlayWebFallback(activity)
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to open Play Store app", e)
            openPlayWebFallback(activity)
        }
    }

    private fun openPlayWebFallback(activity: Activity) {
        val url = "https://play.google.com/store/apps/details?id=${context.packageName}"
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { Log.e(TAG, "No app to open Google Play listing", it) }
    }

    private fun installationSource(): UpdateSource = updateSourceForInstaller(
        runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        }.onFailure { Log.w(TAG, "Installer lookup failed; treating as a sideload", it) }
            .getOrNull()
    )

    private companion object {
        const val TAG = "GithubAppUpdater"
        const val PLAY_STORE = "com.android.vending"
    }
}
