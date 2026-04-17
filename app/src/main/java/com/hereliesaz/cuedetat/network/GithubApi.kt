package com.hereliesaz.cuedetat.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * A data class representing the relevant fields from a GitHub Release API response.
 * We only care about the tag name, which contains the version string.
 */
data class GithubRelease(
    val tag_name: String
)

/**
 * Retrofit interface for the GitHub API.
 * Defines the endpoint for fetching the latest release of a repository.
 */
interface GithubApi {
    /**
     * Fetches the latest release info for a given public repository.
     *
     * @param owner The GitHub username/organization.
     * @param repo The repository name.
     * @return A Response object containing the release data.
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubRelease>
}
