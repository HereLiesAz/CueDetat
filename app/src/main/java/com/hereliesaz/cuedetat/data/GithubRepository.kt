package com.hereliesaz.cuedetat.data

import com.hereliesaz.cuedetat.network.GithubApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the result of a version fetch, providing more detail than a nullable string.
 */
sealed class VersionResult {
    /**
     * Indicates a successful fetch.
     * @param tagName The version tag from the release.
     */
    data class Success(val tagName: String) : VersionResult()

    /**
     * Indicates a failed API request.
     * @param code The HTTP status code of the failure.
     */
    data class Failure(val code: Int) : VersionResult()

    /**
     * Indicates a network or other unknown error occurred.
     */
    object Error : VersionResult()
}

@Singleton
class GithubRepository @Inject constructor(private val githubApi: GithubApi) {

    companion object {
        private const val REPO_OWNER = "hereliesaz"
        private const val REPO_NAME = "CueDetat"
    }

    /**
     * Fetches the latest release version name from the project's GitHub repository.
     * @return A [VersionResult] indicating success, failure with an HTTP code, or a generic error.
     */
    suspend fun getLatestVersion(): VersionResult {
        return try {
            val response = githubApi.getLatestRelease(REPO_OWNER, REPO_NAME)
            if (response.isSuccessful) {
                // If successful, ensure the body and tag name are not null.
                response.body()?.tag_name?.let {
                    VersionResult.Success(it)
                } ?: VersionResult.Failure(response.code()) // Success but empty body is a failure.
            } else {
                // If the request was not successful, return the HTTP error code.
                VersionResult.Failure(response.code())
            }
        } catch (e: Exception) {
            // Exceptions are the universe's way of saying "not today."
            VersionResult.Error
        }
    }
}