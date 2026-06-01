package com.hereliesaz.cuedetat.data

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
        private const val REPO_OWNER = "hereliesaz"
        private const val REPO_NAME = "CueDetat"
    }

    /**
     * Fetches the latest release version name from the project's GitHub repository.
     * @return The tag name as a String, or null if the request fails or an error occurs.
     */
    suspend fun getLatestVersionName(): String? {
        return try {
            val response = githubApi.getLatestRelease(REPO_OWNER, REPO_NAME)
            if (response.isSuccessful) {
                response.body()?.tag_name
            } else {
                null
            }
        } catch (e: Exception) {
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
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}