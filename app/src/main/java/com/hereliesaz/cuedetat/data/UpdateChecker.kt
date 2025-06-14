package com.hereliesaz.cuedetat.data

import com.hereliesaz.cuedetat.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A sealed class representing the possible outcomes of an update check.
 */
sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class UpdateAvailable(val latestVersion: String) : UpdateResult()

    /**
     * Indicates that the update check failed.
     * @param reason A user-facing string explaining why.
     */
    data class CheckFailed(val reason: String) : UpdateResult()
}

/**
 * A dedicated class to handle the application update check logic.
 * It encapsulates the process of fetching the latest version and comparing it
 * with the current installed version.
 */
@Singleton
class UpdateChecker @Inject constructor(private val githubRepository: GithubRepository) {

    /**
     * Checks for a new application version on GitHub.
     * @return An [UpdateResult] indicating the outcome of the check.
     */
    suspend fun checkForUpdate(): UpdateResult {
        val versionResult = githubRepository.getLatestVersion()
        val currentVersion = BuildConfig.VERSION_NAME

        return when (versionResult) {
            is VersionResult.Success -> {
                val comparableLatestVersion = versionResult.tagName.removePrefix("v")
                if (comparableLatestVersion == currentVersion) {
                    UpdateResult.UpToDate
                } else {
                    UpdateResult.UpdateAvailable(versionResult.tagName)
                }
            }

            is VersionResult.Failure -> {
                // Provide a more descriptive reason based on the HTTP code.
                val reason = if (versionResult.code == 404) {
                    "No public release found."
                } else {
                    "API request failed (Code: ${versionResult.code})"
                }
                UpdateResult.CheckFailed(reason)
            }

            is VersionResult.Error -> {
                UpdateResult.CheckFailed("A network error occurred.")
            }
        }
    }
}