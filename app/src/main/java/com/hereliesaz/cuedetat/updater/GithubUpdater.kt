package com.hereliesaz.cuedetat.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.hereliesaz.cuedetat.config.AppConfig
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubUpdater(
    private val context: Context,
    private val repoOwner: String,
    private val repoName: String,
    private val currentVersionCode: Int,
    private val callback: Callback
) {
    private val TAG = "${AppConfig.TAG}_GitHubUpdater"
    private val API_BASE_URL = "https://api.github.com/repos/$repoOwner/$repoName"
    private val RELEASE_URL = "$API_BASE_URL/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    interface Callback {
        fun onUpdateCheckComplete(latestRelease: ReleaseInfo?)
        fun onUpdateDownloadComplete(downloadId: Long)
        fun onUpdateDownloadFailed(reason: String)
        fun onNoUpdateAvailable()
        fun onError(message: String)
    }

    data class ReleaseInfo(
        val tagName: String,
        val versionCode: Int, // The version code extracted from the release, or 0 if not found
        val downloadUrl: String,
        val releaseName: String
    )

    private var downloadReceiver: BroadcastReceiver? = null
    private var downloadId: Long = -1L

    /**
     * Checks for a new release on GitHub.
     */
    fun checkForUpdate() {
        Log.d(TAG, "Checking for updates at $RELEASE_URL")
        val request = Request.Builder().url(RELEASE_URL).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to check for updates: ${e.message}")
                callback.onError("Failed to check for updates: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "GitHub API request failed: ${response.code} - ${response.message}")
                        callback.onError("Failed to check for updates: HTTP ${response.code}")
                        return
                    }

                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        callback.onError("Empty response from GitHub API.")
                        return
                    }

                    try {
                        val json = JSONObject(responseBody)
                        val tagName = json.getString("tag_name")
                        val releaseName = json.getString("name")

                        // Try to extract version code from tag name or release name
                        // This is a simple regex example; needs to be robust for your tagging scheme.
                        val extractedVersionCode = extractVersionCodeFromTag(tagName)

                        if (extractedVersionCode == 0) {
                            Log.w(TAG, "Could not extract version code from tag: $tagName. Skipping update check.")
                            callback.onError("Could not parse version from latest release tag: $tagName")
                            return
                        }

                        Log.d(TAG, "Latest release tag: $tagName (Version Code: $extractedVersionCode), Current: $currentVersionCode")

                        if (extractedVersionCode > currentVersionCode) {
                            val assets = json.getJSONArray("assets")
                            var apkDownloadUrl: String? = null
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val assetName = asset.getString("name")
                                if (assetName.matches(Regex(AppConfig.GITHUB_RELEASE_ASSET_NAME_REGEX))) {
                                    apkDownloadUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }

                            if (apkDownloadUrl != null) {
                                callback.onUpdateCheckComplete(ReleaseInfo(tagName, extractedVersionCode, apkDownloadUrl, releaseName))
                            } else {
                                callback.onError("No APK asset found in latest release for $tagName.")
                            }
                        } else {
                            callback.onNoUpdateAvailable()
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing GitHub API response: ${e.message}", e)
                        callback.onError("Error parsing GitHub API response: ${e.message}")
                    }
                }
            }
        })
    }

    /**
     * Attempts to extract a numeric version code from a tag name (e.g., "v1.2.3" -> 123).
     * This needs to be adapted to your actual release tagging scheme.
     */
    private fun extractVersionCodeFromTag(tagName: String): Int {
        // Example: If tag is "v1.0.0" and versionCode is 100, "v1.1.0" is 110 etc.
        // This regex takes numbers separated by dots and joins them.
        val numericParts = Regex("\\d+").findAll(tagName).map { it.value }.joinToString("")
        return numericParts.toIntOrNull() ?: 0 // Convert to Int, return 0 if conversion fails
    }


    /**
     * Downloads the APK file.
     */
    fun downloadUpdate(apkDownloadUrl: String, releaseName: String) {
        val fileName = "${repoName}_${releaseName}.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) {
            destination.delete() // Delete old file if it exists
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkDownloadUrl))
            .setTitle("Downloading $repoName Update")
            .setDescription("Version: $releaseName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Download started: $downloadId")

        // Register receiver for download completion
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val downloadedUriString = cursor.getString(uriIndex)
                            Log.d(TAG, "Download successful: $downloadedUriString")
                            callback.onUpdateDownloadComplete(downloadId)
                        } else {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(reasonIndex)
                            Log.e(TAG, "Download failed: $status, reason: $reason")
                            callback.onUpdateDownloadFailed("Download failed (status: $status, reason: $reason)")
                        }
                    } else {
                        callback.onUpdateDownloadFailed("Download failed: Cursor empty")
                    }
                    cursor.close()
                    unregisterDownloadReceiver()
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(downloadReceiver, filter)
    }

    /**
     * Initiates the installation of the downloaded APK.
     */
    fun installUpdate(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            callback.onError("Downloaded file URI is null. Download might not be complete or failed.")
            return
        }

        val apkFile = File(uri.path!!)
        if (!apkFile.exists()) {
            callback.onError("Downloaded APK file not found at path: ${apkFile.absolutePath}")
            return
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.applicationContext.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setData(contentUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(installIntent)
            Log.d(TAG, "Installation intent sent.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start installation: ${e.message}", e)
            callback.onError("Failed to start installation: ${e.message}")
        }
    }

    private fun unregisterDownloadReceiver() {
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver)
                downloadReceiver = null
                Log.d(TAG, "Download receiver unregistered.")
            } catch (e: IllegalArgumentException) {
                // Receiver was already unregistered or never registered
                Log.w(TAG, "Attempted to unregister receiver that was not registered: ${e.message}")
            }
        }
    }
}