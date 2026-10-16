package com.hereliesaz.cuedetat.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * A data class representing the relevant fields from a GitHub Release API response.
 * `tag_name` carries the version string; `assets` carry the downloadable APK(s)
 * used by the GitHub-channel in-app updater; `html_url` is the release page fallback.
 *
 * Every field is nullable: Gson instantiates through Unsafe and ignores Kotlin
 * nullability and defaults, so a field absent from the JSON arrives as null even when
 * declared non-null — the declared type has to admit that.
 */
data class GithubRelease(
    val tag_name: String? = null,
    val name: String? = null,
    val html_url: String? = null,
    val assets: List<GithubAsset>? = null
)

/** A single downloadable file attached to a GitHub release. */
data class GithubAsset(
    val name: String? = null,
    val browser_download_url: String? = null
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
