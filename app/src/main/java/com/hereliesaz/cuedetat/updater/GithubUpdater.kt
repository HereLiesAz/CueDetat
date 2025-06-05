// FILE: app\src\main\java\com\hereliesaz\cuedetat\updater\GithubUpdater.kt
package com.hereliesaz.cuedetat.updater

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hereliesaz.cuedetat.config.AppConfig
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

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
        fun onUpdateCheckComplete(latestRelease: ReleaseInfo?, isNewer: Boolean)
        fun onUpdateDownloadComplete(downloadId: Long)
        fun onUpdateDownloadFailed(reason: String)
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
    private var downloadedApkFileName: String? = null // Store the expected file name

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
                        val extractedVersionCode = extractVersionCodeFromTag(tagName)

                        if (extractedVersionCode == 0) {
                            Log.w(TAG, "Could not extract version code from tag: $tagName. Skipping update check.")
                            callback.onError("Could not parse version from latest release tag: $tagName")
                            callback.onUpdateCheckComplete(null, false) // Indicate no valid update found
                            return
                        }

                        Log.d(TAG, "Latest release tag: $tagName (Version Code: $extractedVersionCode), Current: $currentVersionCode")

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
                            val latestReleaseInfo = ReleaseInfo(tagName, extractedVersionCode, apkDownloadUrl, releaseName)
                            val isNewer = extractedVersionCode > currentVersionCode
                            callback.onUpdateCheckComplete(latestReleaseInfo, isNewer)
                        } else {
                            callback.onError("No APK asset found in latest release for $tagName.")
                            callback.onUpdateCheckComplete(null, false) // Indicate no valid update found
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing GitHub API response: ${e.message}", e)
                        callback.onError("Error parsing GitHub API response: ${e.message}")
                        callback.onUpdateCheckComplete(null, false) // Indicate no valid update found
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
    @SuppressLint("Range")
    fun downloadUpdate(apkDownloadUrl: String, releaseName: String) {
        val fileName = "${repoName}_${releaseName}.apk"
        downloadedApkFileName = fileName // Store the expected filename for later use

        // Use context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) for app-specific external storage
        // This is the correct way to get a path that FileProvider's <external-files-path> will recognize
        val destinationDir = File(context.getExternalFilesDir(null), "updates")
        if (!destinationDir.exists()) {
            destinationDir.mkdirs() // Create the directory if it doesn't exist
        }
        val destinationFile = File(destinationDir, fileName)

        // Clear previous download if it exists and wasn't completed
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING
        )
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            do {
                val existingDownloadId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                val existingUriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                if (existingUriString != null && existingUriString.contains(destinationFile.name)) {
                    downloadManager.remove(existingDownloadId)
                    Log.d(TAG, "Removed incomplete previous download for ${destinationFile.name}")
                    break
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        if (destinationFile.exists()) {
            destinationFile.delete() // Delete existing completed file if it exists
            Log.d(TAG, "Deleted existing old APK: ${destinationFile.absolutePath}")
        }


        val request = DownloadManager.Request(apkDownloadUrl.toUri())
            .setTitle("Downloading $repoName Update")
            .setDescription("Version: $releaseName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Use setDestinationUri with a Uri from a File object that matches FileProvider config
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Download started: $downloadId for $apkDownloadUrl to ${destinationFile.absolutePath}")

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
                            Log.d(TAG, "Download successful for ID: $downloadId")
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
                    unregisterDownloadReceiver() // Unregister immediately after handling
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Initiates the installation of the downloaded APK.
     */
    @SuppressLint("Range")
    fun installUpdate(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val downloadedUriString = cursor.getString(uriIndex)
            cursor.close()

            if (downloadedUriString == null) {
                callback.onError("Downloaded file URI is null. Download might not be complete or failed.")
                return
            }

            val downloadedUri = downloadedUriString.toUri()
            // Reconstruct the File object using the original destination path.
            // This is safer than relying on `downloadedUri.path` which might be a content URI.
            val destinationDir = File(context.getExternalFilesDir(null), "updates")
            val apkFile = File(destinationDir, downloadedApkFileName) // Use the stored file name

            if (!apkFile.exists()) {
                Log.e(TAG, "Downloaded APK file not found at expected path: ${apkFile.absolutePath}. URI from DM: $downloadedUri")
                callback.onError("Downloaded APK file not found. Please try updating again.")
                return
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.fileprovider",
                apkFile // Pass the File object that is within the FileProvider's configured paths
            )

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setData(contentUri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                context.startActivity(installIntent)
                Log.d(TAG, "Installation intent sent for: ${apkFile.absolutePath}.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start installation: ${e.message}", e)
                callback.onError("Failed to start installation: ${e.message}")
            }
        } else {
            cursor.close()
            callback.onError("Download information not found for ID: $downloadId")
        }
    }

    /**
     * Unregisters the BroadcastReceiver if it's currently registered.
     */
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