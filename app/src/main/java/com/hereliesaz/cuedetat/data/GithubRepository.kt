package com.hereliesaz.cuedetat.data

import android.util.Log
import com.hereliesaz.cuedetat.network.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to fetch data from the project's GitHub page: the latest release, for the
 * GitHub-channel in-app updater.
 */
@Singleton
class GithubRepository @Inject constructor(private val githubApi: GithubApi) {

    companion object {
        private const val TAG = "GithubRepository"
        private const val REPO_OWNER = "hereliesaz"
        private const val REPO_NAME = "CueDetat"
    }

    /**
     * Fetches the full latest release (tag, name, html_url, and downloadable
     * assets), or null on any failure. Used by the GitHub-channel in-app updater ([com.hereliesaz.cuedetat.update.GithubAppUpdater]) to
     * locate the APK to download.
     */
    suspend fun getLatestRelease(): com.hereliesaz.cuedetat.network.GithubRelease? {
        return try {
            val response = githubApi.getLatestRelease(REPO_OWNER, REPO_NAME)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "getLatestRelease: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getLatestRelease: request failed", e)
            null
        }
    }
}