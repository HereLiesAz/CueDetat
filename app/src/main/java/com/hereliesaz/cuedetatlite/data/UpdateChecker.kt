package com.hereliesaz.cuedetatlite.data

import com.hereliesaz.cuedetatlite.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _updateResult = MutableStateFlow<UpdateResult>(UpdateResult.UpToDate)
    val updateResult: StateFlow<UpdateResult> = _updateResult.asStateFlow()

    private var latestVersionUrl: String? = null

    /**
     * Checks for a new application version on GitHub and updates the public StateFlow.
     */
    suspend fun checkForUpdate() {
        val versionResult = githubRepository.getLatestVersion()
        val currentVersion = BuildConfig.VERSION_NAME

        _updateResult.value = when (versionResult) {
            is VersionResult.Success -> {
                val comparableLatestVersion = versionResult.tagName.removePrefix("v")
                if (comparableLatestVersion == currentVersion) {
                    UpdateResult.UpToDate
                } else {
                    latestVersionUrl = "https://github.com/hereliesaz/CueDetat/releases/latest"
                    UpdateResult.UpdateAvailable(versionResult.tagName)
                }
            }

            is VersionResult.Failure -> {
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

    /**
     * Returns the URL for the latest release if one is available.
     */
    fun getLatestReleaseUrl(): String? {
        return latestVersionUrl
    }
}