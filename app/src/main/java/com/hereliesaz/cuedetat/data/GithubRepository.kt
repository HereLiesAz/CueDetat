package com.hereliesaz.cuedetat.data

import android.util.Log
import com.hereliesaz.cuedetat.network.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository to fetch data from the project's GitHub page.
 * It is simplified to only fetch the latest release tag name.
 */
@Singleton
class GithubRepository @Inject constructor(private val githubApi: GithubApi) {

    companion object {
        private const val TAG = "GithubRepository"
        private const val REPO_OWNER = "hereliesaz"
        private const val REPO_NAME = "CueDetat"
    }

    /**
     * Fetches the latest release version name from the project's GitHub repository.
     * @return The tag name as a String, or null if the request fails or an error occurs.
     *   Failures are logged (not just swallowed) so a network/API problem is
     *   distinguishable from "genuinely no newer release" in logcat, even
     *   though both currently surface to callers as null.
     */
    suspend fun getLatestVersionName(): String? {
        return try {
            val response = githubApi.getLatestRelease(REPO_OWNER, REPO_NAME)
            if (response.isSuccessful) {
                response.body()?.tag_name
            } else {
                Log.w(TAG, "getLatestVersionName: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getLatestVersionName: request failed", e)
            null
        }
    }

    /**
     * Fetches the full latest release (tag, name, html_url, and downloadable
     * assets), or null on any failure. Used by the FOSS in-app updater to
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