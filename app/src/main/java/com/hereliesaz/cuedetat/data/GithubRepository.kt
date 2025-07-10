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
}