// FILE: app/src/foss/java/com/hereliesaz/cuedetat/update/FossAppUpdater.kt

package com.hereliesaz.cuedetat.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.hereliesaz.cuedetat.BuildConfig
import com.hereliesaz.cuedetat.data.GithubRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS-flavor self-updater. Checks the project's GitHub releases and, when a
 * newer build is available, downloads the release APK and hands it to the
 * system package installer. This is the closest to "auto-update" Android
 * permits for a normally-installed (non device-owner) app — the OS always
 * shows its own install confirmation, which matches the agreed one-tap popup.
 */
@Singleton
class FossAppUpdater @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val githubRepository: GithubRepository,
) : AppUpdater {

    private val client by lazy { OkHttpClient() }

    override val isSupported: Boolean = true

    override suspend fun checkForUpdate(): UpdateInfo? {
        val release = githubRepository.getLatestRelease() ?: return null
        val tag = release.tag_name
        if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) return null

        // Prefer a FOSS-named APK asset, then any APK.
        val apk = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) && it.name.contains("foss", ignoreCase = true)
        } ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

        return UpdateInfo(
            versionName = tag.removePrefix("v").removePrefix("V"),
            apkUrl = apk?.browser_download_url,
            releaseUrl = release.html_url,
        )
    }

    override suspend fun downloadAndInstall(activity: Activity, info: UpdateInfo) {
        val url = info.apkUrl
        if (url.isNullOrBlank()) {
            // No APK asset — fall back to opening the release page.
            info.releaseUrl?.let { openUrl(activity, it) }
            return
        }

        // Installing from an APK requires the user to have granted this app the
        // "install unknown apps" permission. Route them to the system screen if
        // they haven't, then let them retry.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            runCatching {
                activity.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }

        val apkFile = withContext(Dispatchers.IO) { downloadApk(url) } ?: run {
            Log.w(TAG, "downloadAndInstall: download failed; opening release page")
            info.releaseUrl?.let { openUrl(activity, it) }
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { activity.startActivity(installIntent) }
            .onFailure { Log.e(TAG, "Failed to launch installer", it) }
    }

    private fun downloadApk(url: String): File? {
        return runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val out = File(dir, "cuedetat-update.apk")
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                out.outputStream().use { sink -> body.byteStream().copyTo(sink) }
            }
            out
        }.getOrNull()
    }

    private fun openUrl(activity: Activity, url: String) {
        runCatching {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    companion object {
        private const val TAG = "FossAppUpdater"
    }
}
