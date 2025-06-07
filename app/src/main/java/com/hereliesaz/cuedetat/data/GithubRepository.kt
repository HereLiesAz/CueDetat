package com.hereliesaz.cuedetat.data

import com.hereliesaz.cuedetat.network.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubRepository @Inject constructor(private val githubApi: GithubApi) {

    companion object {
        private const val REPO_OWNER = "hereliesaz"
        private const val REPO_NAME = "CueDetat"
    }

    /**
     * Fetches the latest release version name from the project's GitHub repository.
     * @return The tag name of the latest release (e.g., "2025.06.08-release"), or null if an error occurs.
     */
    suspend fun getLatestVersion(): String? {
        return try {
            val response = githubApi.getLatestRelease(REPO_OWNER, REPO_NAME)
            if (response.isSuccessful) {
                response.body()?.tag_name
            } else {
                null
            }
        } catch (e: Exception) {
            // Exceptions are the universe's way of saying "not today." We listen.
            null
        }
    }
}
