package com.hereliesaz.cuedetat.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the GitHub API.
 */
interface GithubApi {
    /**
     * Fetches the latest release for a given repository.
     *
     * @param owner The owner of the repository.
     * @param repo The name of the repository.
     * @return A [Response] containing the [GithubRelease] data.
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubRelease>
}

/**
 * Data class representing a GitHub release.
 *
 * @property tag_name The name of the tag for this release (e.g., "v1.0.0").
 */
data class GithubRelease(
    val tag_name: String
)
